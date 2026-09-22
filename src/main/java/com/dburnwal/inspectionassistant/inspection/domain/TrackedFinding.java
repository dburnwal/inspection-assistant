package com.dburnwal.inspectionassistant.inspection.domain;

import java.time.Instant;

/**
 * A deduplicated, tracked finding across multiple frames.
 */
public class TrackedFinding {

    private Finding latest;
    private final Instant firstSeen;
    private Instant lastSeen;
    private int observationCount;
    private FindingStatus status;

    public TrackedFinding(Finding finding) {
        this.latest = finding;
        this.firstSeen = finding.observedAt();
        this.lastSeen = finding.observedAt();
        this.observationCount = 1;
        this.status = FindingStatus.OBSERVED;
    }

    public void merge(Finding finding, int confirmationThreshold) {
        this.latest = finding;
        this.lastSeen = finding.observedAt();
        this.observationCount++;
        if (this.observationCount >= confirmationThreshold) {
            this.status = FindingStatus.CONFIRMED;
        }
    }

    public Finding getLatest() { return latest; }
    public Instant getFirstSeen() { return firstSeen; }
    public Instant getLastSeen() { return lastSeen; }
    public int getObservationCount() { return observationCount; }
    public FindingStatus getStatus() { return status; }
    public boolean isConfirmed() { return status == FindingStatus.CONFIRMED; }
}
