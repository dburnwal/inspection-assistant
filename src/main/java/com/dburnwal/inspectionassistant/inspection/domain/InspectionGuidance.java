package com.dburnwal.inspectionassistant.inspection.domain;

/**
 * Structured guidance returned alongside findings.
 */
public record InspectionGuidance(
        String action,      // e.g. MOVE_CAMERA, INSPECT_AREA, ADJUST_ANGLE
        String direction,   // e.g. LEFT, RIGHT, UP, DOWN, CLOSER, FARTHER
        String message      // human-readable instruction
) {
    public static InspectionGuidance of(String message) {
        return new InspectionGuidance("NONE", "NONE", message);
    }
}
