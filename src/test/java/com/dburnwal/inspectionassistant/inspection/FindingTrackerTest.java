package com.dburnwal.inspectionassistant.inspection;

import com.dburnwal.inspectionassistant.inspection.application.FindingTracker;
import com.dburnwal.inspectionassistant.inspection.domain.Finding;
import com.dburnwal.inspectionassistant.inspection.domain.FindingStatus;
import com.dburnwal.inspectionassistant.inspection.domain.Severity;
import com.dburnwal.inspectionassistant.inspection.domain.TrackedFinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FindingTrackerTest {

    private FindingTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FindingTracker();
    }

    private Finding finding(String type, String part) {
        return new Finding(UUID.randomUUID().toString(), type, part,
                "Possible " + type + " on " + part, 0.85, Severity.MEDIUM, false, "", Instant.now());
    }

    @Test
    void newFindingIsReportedImmediately() {
        List<TrackedFinding> result = tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(FindingStatus.OBSERVED);
    }

    @Test
    void sameFindingTwiceBecomesConfirmed() {
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        List<TrackedFinding> result = tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(FindingStatus.CONFIRMED);
    }

    @Test
    void differentTypeOrPartAreTrackedSeparately() {
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        List<TrackedFinding> result = tracker.track("s1", List.of(finding("SCRATCH", "HOOD")));
        assertThat(tracker.allFindings("s1")).hasSize(2);
    }

    @Test
    void confirmedFindingsReturnsOnlyConfirmed() {
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        tracker.track("s1", List.of(finding("SCRATCH", "HOOD")));
        assertThat(tracker.confirmedFindings("s1")).hasSize(1);
        assertThat(tracker.confirmedFindings("s1").get(0).getLatest().type()).isEqualTo("DENT");
    }

    @Test
    void clearRemovesSessionData() {
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        tracker.clear("s1");
        assertThat(tracker.allFindings("s1")).isEmpty();
    }

    @Test
    void observationCountIncrementsCorrectly() {
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        tracker.track("s1", List.of(finding("DENT", "REAR_DOOR")));
        assertThat(tracker.allFindings("s1").get(0).getObservationCount()).isEqualTo(3);
    }
}
