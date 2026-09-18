package com.dburnwal.inspectionassistant.inspection;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Tracks findings across frames to avoid duplicates and confirm persistent anomalies.
 */
@Component
public class FindingTracker {

    private static final int CONFIRMATION_THRESHOLD = 2;

    record TrackedFinding(Finding finding, int count, Instant firstSeen, Instant lastSeen, boolean confirmed) {}

    // sessionId -> list of tracked findings
    private final Map<String, List<TrackedFinding>> sessionTracks = new HashMap<>();

    /**
     * Merges new raw findings into the session's tracked state.
     * Returns only findings that are new or just confirmed (to avoid flooding the UI).
     */
    public List<Finding> track(String sessionId, List<Finding> incoming) {
        sessionTracks.putIfAbsent(sessionId, new ArrayList<>());
        List<TrackedFinding> tracks = sessionTracks.get(sessionId);

        List<Finding> toReport = new ArrayList<>();

        for (Finding f : incoming) {
            Optional<TrackedFinding> match = tracks.stream()
                    .filter(t -> t.finding().type() == f.type() && similar(t.finding().description(), f.description()))
                    .findFirst();

            if (match.isPresent()) {
                TrackedFinding existing = match.get();
                int newCount = existing.count() + 1;
                boolean wasConfirmed = existing.confirmed();
                boolean nowConfirmed = newCount >= CONFIRMATION_THRESHOLD;
                TrackedFinding updated = new TrackedFinding(f, newCount, existing.firstSeen(), Instant.now(), nowConfirmed);
                tracks.set(tracks.indexOf(existing), updated);
                // Report only when it just became confirmed
                if (!wasConfirmed && nowConfirmed) {
                    toReport.add(f);
                }
            } else {
                tracks.add(new TrackedFinding(f, 1, Instant.now(), Instant.now(), false));
                // Report new findings immediately so the UI shows something
                toReport.add(f);
            }
        }

        return toReport;
    }

    public List<Finding> confirmedFindings(String sessionId) {
        return sessionTracks.getOrDefault(sessionId, List.of()).stream()
                .filter(TrackedFinding::confirmed)
                .map(TrackedFinding::finding)
                .toList();
    }

    public void clear(String sessionId) {
        sessionTracks.remove(sessionId);
    }

    private boolean similar(String a, String b) {
        if (a == null || b == null) return false;
        String la = a.toLowerCase(), lb = b.toLowerCase();
        // Simple word-overlap heuristic
        Set<String> wordsA = new HashSet<>(Arrays.asList(la.split("\\W+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(lb.split("\\W+")));
        wordsA.retainAll(wordsB);
        return wordsA.size() >= 3;
    }
}
