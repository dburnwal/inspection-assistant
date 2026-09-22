package com.dburnwal.inspectionassistant.inspection.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InspectionSession {

    private final String id;
    private final String profileId;
    private final Instant startedAt;
    private Instant completedAt;
    private SessionStatus status;
    private final List<TrackedFinding> trackedFindings = new ArrayList<>();

    public InspectionSession(String id, String profileId) {
        this.id = id;
        this.profileId = profileId;
        this.startedAt = Instant.now();
        this.status = SessionStatus.ACTIVE;
    }

    public String getId() { return id; }
    public String getProfileId() { return profileId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public SessionStatus getStatus() { return status; }
    public List<TrackedFinding> getTrackedFindings() { return trackedFindings; }

    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail() {
        this.status = SessionStatus.FAILED;
        this.completedAt = Instant.now();
    }
}
