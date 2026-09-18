package com.dburnwal.inspectionassistant.inspection;

public record Finding(
        String id,
        FindingType type,
        String description,
        double confidence,
        Severity severity,
        String action
) {}
