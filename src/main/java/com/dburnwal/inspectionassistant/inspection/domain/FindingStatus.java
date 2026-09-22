package com.dburnwal.inspectionassistant.inspection.domain;

public enum FindingStatus {
    OBSERVED,    // seen once
    CONFIRMED,   // seen multiple times
    DISMISSED    // below confidence threshold
}
