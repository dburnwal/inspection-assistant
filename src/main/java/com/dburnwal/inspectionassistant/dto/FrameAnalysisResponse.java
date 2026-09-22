package com.dburnwal.inspectionassistant.dto;

import com.dburnwal.inspectionassistant.inspection.domain.InspectionGuidance;

import java.util.List;

public record FrameAnalysisResponse(
        String sessionId,
        List<TrackedFindingResponse> findings,
        InspectionGuidance guidance
) {}
