package com.dburnwal.inspectionassistant.inspection.domain;

import java.time.Instant;

/**
 * A single observation from one AI analysis pass.
 * Generic — does not contain car-specific fields.
 */
public record Finding(
        String id,
        String type,          // domain-specific string, e.g. "DENT", "SCRATCH"
        String part,          // domain-specific part label, e.g. "REAR_DOOR"
        String description,
        double confidence,
        Severity severity,
        boolean requiresCloserInspection,
        String guidanceMessage,
        Instant observedAt
) {}
