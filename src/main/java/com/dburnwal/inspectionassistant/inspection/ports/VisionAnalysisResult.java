package com.dburnwal.inspectionassistant.inspection.ports;

import com.dburnwal.inspectionassistant.inspection.domain.Finding;
import com.dburnwal.inspectionassistant.inspection.domain.InspectionGuidance;

import java.util.List;

/**
 * Result returned by the vision analysis adapter.
 */
public record VisionAnalysisResult(
        List<Finding> findings,
        InspectionGuidance guidance
) {}
