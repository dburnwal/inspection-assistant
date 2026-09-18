package com.dburnwal.inspectionassistant.inspection;

import java.time.Instant;
import java.util.*;

public class InspectionSession {

    private final String sessionId;
    private final Instant createdAt;
    private Instant lastUpdatedAt;
    private byte[] previousFrame;
    private final List<Finding> detectedFindings = new ArrayList<>();
    private final List<Finding> confirmedFindings = new ArrayList<>();

    public InspectionSession(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = Instant.now();
        this.lastUpdatedAt = this.createdAt;
    }

    public String getSessionId() { return sessionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public byte[] getPreviousFrame() { return previousFrame; }
    public List<Finding> getDetectedFindings() { return detectedFindings; }
    public List<Finding> getConfirmedFindings() { return confirmedFindings; }

    public void setPreviousFrame(byte[] frame) { this.previousFrame = frame; }

    public void touch() { this.lastUpdatedAt = Instant.now(); }
}
