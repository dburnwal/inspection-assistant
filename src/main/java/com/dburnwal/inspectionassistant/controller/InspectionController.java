package com.dburnwal.inspectionassistant.controller;

import com.dburnwal.inspectionassistant.dto.FindingResponse;
import com.dburnwal.inspectionassistant.dto.InspectionResponse;
import com.dburnwal.inspectionassistant.inspection.Finding;
import com.dburnwal.inspectionassistant.inspection.InspectionOrchestrator;
import com.dburnwal.inspectionassistant.inspection.InspectionSession;
import com.dburnwal.inspectionassistant.inspection.InspectionSessionService.SessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspection")
@CrossOrigin(origins = "*")
public class InspectionController {

    private final InspectionOrchestrator orchestrator;

    public InspectionController(InspectionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/session")
    public Map<String, String> createSession() {
        InspectionSession session = orchestrator.createSession();
        return Map.of("sessionId", session.getSessionId());
    }

    @PostMapping(value = "/{sessionId}/frame", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InspectionResponse analyzeFrame(@PathVariable String sessionId,
                                           @RequestParam("image") MultipartFile image) throws IOException {
        try {
            List<Finding> findings = orchestrator.processFrame(sessionId, image.getBytes());
            return new InspectionResponse(sessionId, toResponse(findings));
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{sessionId}")
    public InspectionResponse getSession(@PathVariable String sessionId) {
        try {
            InspectionSession session = orchestrator.getSession(sessionId);
            return new InspectionResponse(sessionId, toResponse(session.getDetectedFindings()));
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/report")
    public InspectionResponse generateReport(@PathVariable String sessionId) {
        try {
            List<Finding> confirmed = orchestrator.generateReport(sessionId);
            return new InspectionResponse(sessionId, toResponse(confirmed));
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private List<FindingResponse> toResponse(List<Finding> findings) {
        return findings.stream()
                .map(f -> new FindingResponse(f.id(), f.type(), f.description(), f.confidence(), f.severity(), f.action()))
                .toList();
    }
}
