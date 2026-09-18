package com.dburnwal.inspectionassistant.inspection;

import com.dburnwal.inspectionassistant.vision.VisionAnalyzer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InspectionOrchestrator {

    private final InspectionSessionService sessionService;
    private final VisionAnalyzer visionAnalyzer;
    private final FindingTracker findingTracker;

    public InspectionOrchestrator(InspectionSessionService sessionService,
                                   VisionAnalyzer visionAnalyzer,
                                   FindingTracker findingTracker) {
        this.sessionService = sessionService;
        this.visionAnalyzer = visionAnalyzer;
        this.findingTracker = findingTracker;
    }

    public InspectionSession createSession() {
        return sessionService.create();
    }

    public List<Finding> processFrame(String sessionId, byte[] imageBytes) {
        InspectionSession session = sessionService.get(sessionId);
        List<Finding> raw = visionAnalyzer.analyze(imageBytes);
        List<Finding> toReport = findingTracker.track(sessionId, raw);
        session.getDetectedFindings().addAll(toReport);
        session.getConfirmedFindings().clear();
        session.getConfirmedFindings().addAll(findingTracker.confirmedFindings(sessionId));
        session.setPreviousFrame(imageBytes);
        session.touch();
        return toReport;
    }

    public InspectionSession getSession(String sessionId) {
        return sessionService.get(sessionId);
    }

    public List<Finding> generateReport(String sessionId) {
        return findingTracker.confirmedFindings(sessionId);
    }
}
