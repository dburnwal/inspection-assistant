package com.dburnwal.inspectionassistant.dto;

import com.dburnwal.inspectionassistant.inspection.domain.FindingStatus;
import com.dburnwal.inspectionassistant.inspection.domain.Severity;
import com.dburnwal.inspectionassistant.inspection.ports.CostEstimationPort.CostEstimate;

import java.time.Instant;

public record TrackedFindingResponse(
        String id,
        String type,
        String part,
        String description,
        double confidence,
        Severity severity,
        boolean requiresCloserInspection,
        String guidanceMessage,
        FindingStatus status,
        int observationCount,
        Instant firstSeen,
        Instant lastSeen,
        CostEstimate costEstimate
) {}
