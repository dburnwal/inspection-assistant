package com.dburnwal.inspectionassistant.controller;

import com.dburnwal.inspectionassistant.adapter.ImageResizer;
import com.dburnwal.inspectionassistant.adapter.InMemorySessionRepository.SessionNotFoundException;
import com.dburnwal.inspectionassistant.adapter.ai.MockVisionAnalysisAdapter;
import com.dburnwal.inspectionassistant.car.domain.VehicleContext;
import com.dburnwal.inspectionassistant.car.profile.CarDamageInspectionProfile;
import com.dburnwal.inspectionassistant.dto.*;
import com.dburnwal.inspectionassistant.inspection.application.InspectionService;
import com.dburnwal.inspectionassistant.inspection.application.InspectionService.FrameAnalysisResult;
import com.dburnwal.inspectionassistant.inspection.domain.InspectionSession;
import com.dburnwal.inspectionassistant.inspection.domain.TrackedFinding;
import com.dburnwal.inspectionassistant.inspection.ports.CostEstimationPort;
import com.dburnwal.inspectionassistant.inspection.ports.CostEstimationPort.CostEstimate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/inspection/sessions")
@CrossOrigin(origins = "*")
public class InspectionController {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final String REPORT_DISCLAIMER =
            "AI-assisted estimate based on visible damage only. Actual workshop quotation may differ.";

    private final InspectionService inspectionService;
    private final CostEstimationPort costEstimationPort;
    private final ImageResizer imageResizer;

    // sessionId -> VehicleContext (car-specific, not in generic session)
    private final Map<String, VehicleContext> vehicleContexts = new ConcurrentHashMap<>();

    public InspectionController(InspectionService inspectionService,
                                CostEstimationPort costEstimationPort,
                                ImageResizer imageResizer) {
        this.inspectionService = inspectionService;
        this.costEstimationPort = costEstimationPort;
        this.imageResizer = imageResizer;
    }

    @PostMapping
    public SessionResponse createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String profileId = (request != null && request.profileId() != null)
                ? request.profileId()
                : CarDamageInspectionProfile.ID;
        try {
            InspectionSession session = inspectionService.createSession(profileId);
            if (request != null && request.vehicle() != null) {
                VehicleContextRequest v = request.vehicle();
                vehicleContexts.put(session.getId(),
                        new VehicleContext(v.make(), v.model(), v.year(), v.variant(), v.city()));
            }
            return new SessionResponse(session.getId(), session.getProfileId(), session.getStatus());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{sessionId}")
    public SessionResponse getSession(@PathVariable String sessionId) {
        try {
            InspectionSession session = inspectionService.getSession(sessionId);
            return new SessionResponse(session.getId(), session.getProfileId(), session.getStatus());
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping(value = "/{sessionId}/frames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FrameAnalysisResponse analyzeFrame(
            @PathVariable String sessionId,
            @RequestParam("image") MultipartFile image,
            @RequestHeader(value = "X-Frame-Id", required = false) String frameId) throws IOException {
        validateImage(image);
        try {
            byte[] imageBytes = imageResizer.resize(image.getBytes());
            // Encode frameId for MockVisionAnalysisAdapter when provided
            if (frameId != null && !frameId.isBlank()) {
                imageBytes = MockVisionAnalysisAdapter.encodeFrameId(frameId, imageBytes);
            }
            FrameAnalysisResult result = inspectionService.processFrame(sessionId, imageBytes);
            return new FrameAnalysisResponse(sessionId, toResponses(result.findings()), result.guidance());
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{sessionId}/findings")
    public List<TrackedFindingResponse> getFindings(@PathVariable String sessionId) {
        try {
            return toResponses(inspectionService.getFindings(sessionId));
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/complete")
    public InspectionReportResponse completeSession(@PathVariable String sessionId) {
        try {
            List<TrackedFinding> confirmed = inspectionService.completeSession(sessionId);
            List<TrackedFindingResponse> responses = toResponses(confirmed);
            long totalMin = responses.stream().mapToLong(r -> r.costEstimate() != null ? r.costEstimate().minimum() : 0).sum();
            long totalMax = responses.stream().mapToLong(r -> r.costEstimate() != null ? r.costEstimate().maximum() : 0).sum();
            InspectionSession session = inspectionService.getSession(sessionId);
            return new InspectionReportResponse(sessionId, session.getProfileId(), responses, totalMin, totalMax, "INR", REPORT_DISCLAIMER);
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private void validateImage(MultipartFile image) {
        if (image.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is empty");
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image content type");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image exceeds 5MB limit");
        }
    }

    private List<TrackedFindingResponse> toResponses(List<TrackedFinding> findings) {
        return findings.stream().map(tf -> {
            CostEstimate cost = null;
            try {
                cost = costEstimationPort.estimate(tf.getLatest().type(), tf.getLatest().part(), tf.getLatest().severity().name());
            } catch (Exception ignored) {}
            return new TrackedFindingResponse(
                    tf.getLatest().id(),
                    tf.getLatest().type(),
                    tf.getLatest().part(),
                    tf.getLatest().description(),
                    tf.getLatest().confidence(),
                    tf.getLatest().severity(),
                    tf.getLatest().requiresCloserInspection(),
                    tf.getLatest().guidanceMessage(),
                    tf.getStatus(),
                    tf.getObservationCount(),
                    tf.getFirstSeen(),
                    tf.getLastSeen(),
                    cost
            );
        }).toList();
    }
}
