package com.dburnwal.inspectionassistant.inspection.application;

import com.dburnwal.inspectionassistant.inspection.domain.Finding;
import com.dburnwal.inspectionassistant.inspection.domain.TrackedFinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Deduplicates findings across frames within a session.
 * Matches by type + part; confirms after reaching the threshold.
 */
@Component
public class FindingTracker {

    @Value("${inspection.finding.confirmation-threshold:2}")
    private int confirmationThreshold = 2;

    // sessionId -> tracked findings
    private final Map<String, List<TrackedFinding>> sessionTracks = new HashMap<>();

    /**
     * Merges incoming findings into tracked state.
     * Returns findings that are new or just became confirmed.
     */
    public List<TrackedFinding> track(String sessionId, List<Finding> incoming) {
        sessionTracks.putIfAbsent(sessionId, new ArrayList<>());
        List<TrackedFinding> tracks = sessionTracks.get(sessionId);
        List<TrackedFinding> toReport = new ArrayList<>();

        for (Finding f : incoming) {
            Optional<TrackedFinding> match = tracks.stream()
                    .filter(t -> matches(t.getLatest(), f))
                    .findFirst();

            if (match.isPresent()) {
                TrackedFinding existing = match.get();
                boolean wasConfirmed = existing.isConfirmed();
                existing.merge(f, confirmationThreshold);
                if (!wasConfirmed && existing.isConfirmed()) {
                    toReport.add(existing);
                }
            } else {
                TrackedFinding tracked = new TrackedFinding(f);
                tracks.add(tracked);
                toReport.add(tracked);
            }
        }
        return toReport;
    }

    public List<TrackedFinding> confirmedFindings(String sessionId) {
        return sessionTracks.getOrDefault(sessionId, List.of()).stream()
                .filter(TrackedFinding::isConfirmed)
                .toList();
    }

    public List<TrackedFinding> allFindings(String sessionId) {
        return List.copyOf(sessionTracks.getOrDefault(sessionId, List.of()));
    }

    public void clear(String sessionId) {
        sessionTracks.remove(sessionId);
    }

    private boolean matches(Finding a, Finding b) {
        return Objects.equals(a.type(), b.type()) && Objects.equals(a.part(), b.part());
    }
}
