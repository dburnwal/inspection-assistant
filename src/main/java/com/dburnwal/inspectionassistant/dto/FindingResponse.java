package com.dburnwal.inspectionassistant.dto;

import com.dburnwal.inspectionassistant.inspection.FindingType;
import com.dburnwal.inspectionassistant.inspection.Severity;

public record FindingResponse(
        String id,
        FindingType type,
        String description,
        double confidence,
        Severity severity,
        String action
) {}
