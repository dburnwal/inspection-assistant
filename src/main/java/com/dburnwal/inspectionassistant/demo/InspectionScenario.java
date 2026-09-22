package com.dburnwal.inspectionassistant.demo;

import java.util.List;

/**
 * A named sequence of demo frames representing a realistic inspection scenario.
 */
public record InspectionScenario(
        String id,
        String name,
        String description,
        List<DemoFrame> frames
) {
    public record DemoFrame(
            int sequenceNumber,
            String frameId,       // used by MockVisionAdapter for deterministic responses
            String label,         // human-readable e.g. "Front bumper"
            String imagePath      // classpath-relative path to demo image
    ) {}
}
