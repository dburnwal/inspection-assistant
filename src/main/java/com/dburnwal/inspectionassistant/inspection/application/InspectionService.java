package com.dburnwal.inspectionassistant.inspection.application;

import com.dburnwal.inspectionassistant.inspection.domain.*;
import com.dburnwal.inspectionassistant.inspection.ports.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core inspection engine. Profile-agnostic.
 * Delegates AI analysis to VisionAnalysisPort and finding deduplication to FindingTracker.
 */
@Service
public class InspectionService {

    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);

    @Value("${inspection.ai.confidence-threshold:0.70}")
    private double confidenceThreshold = 0.70;

    private final InspectionSessionService sessionService;
    private final VisionAnalysisPort visionAnalysisPort;
    private final FindingTracker findingTracker;
    private final Map<String, InspectionProfile> profilesById;

    public InspectionService(InspectionSessionService sessionService,
                             VisionAnalysisPort visionAnalysisPort,
                             FindingTracker findingTracker,
                             List<InspectionProfile> profiles) {
        this.sessionService = sessionService;
        this.visionAnalysisPort = visionAnalysisPort;
        this.findingTracker = findingTracker;
        this.profilesById = profiles.stream()
                .collect(Collectors.toMap(InspectionProfile::getId, Function.identity()));
    }

    public InspectionSession createSession(String profileId) {
        if (!profilesById.containsKey(profileId)) {
            throw new IllegalArgumentException("Unknown inspection profile: " + profileId);
        }
        InspectionSession session = sessionService.create(profileId);
        log.info("inspection.started sessionId={} profileId={}", session.getId(), profileId);
        return session;
    }

    public FrameAnalysisResult processFrame(String sessionId, byte[] imageBytes) {
        InspectionSession session = sessionService.get(sessionId);
        InspectionProfile profile = profilesById.get(session.getProfileId());

        log.info("inspection.ai.analysis.started sessionId={}", sessionId);
        VisionAnalysisResult result = visionAnalysisPort.analyze(imageBytes, profile);
        log.info("inspection.ai.analysis.completed sessionId={} findings={}", sessionId, result.findings().size());

        List<Finding> aboveThreshold = result.findings().stream()
                .filter(f -> f.confidence() >= confidenceThreshold)
                .toList();

        List<TrackedFinding> reported = findingTracker.track(sessionId, aboveThreshold);

        reported.forEach(tf -> {
            if (tf.getObservationCount() == 1) {
                log.info("inspection.finding.created sessionId={} type={} part={}", sessionId, tf.getLatest().type(), tf.getLatest().part());
            } else {
                log.info("inspection.finding.updated sessionId={} type={} part={} observations={}", sessionId, tf.getLatest().type(), tf.getLatest().part(), tf.getObservationCount());
            }
        });

        return new FrameAnalysisResult(reported, result.guidance());
    }

    public List<TrackedFinding> getFindings(String sessionId) {
        return findingTracker.allFindings(sessionId);
    }

    public List<TrackedFinding> completeSession(String sessionId) {
        sessionService.complete(sessionId);
        List<TrackedFinding> confirmed = findingTracker.confirmedFindings(sessionId);
        log.info("inspection.completed sessionId={} confirmedFindings={}", sessionId, confirmed.size());
        return confirmed;
    }

    public InspectionSession getSession(String sessionId) {
        return sessionService.get(sessionId);
    }

    public record FrameAnalysisResult(List<TrackedFinding> findings, InspectionGuidance guidance) {}
}
