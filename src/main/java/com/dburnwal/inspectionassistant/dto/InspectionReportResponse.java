package com.dburnwal.inspectionassistant.dto;

import java.util.List;

public record InspectionReportResponse(
        String sessionId,
        String profileId,
        List<TrackedFindingResponse> findings,
        long totalMinCost,
        long totalMaxCost,
        String currency,
        String disclaimer
) {}
