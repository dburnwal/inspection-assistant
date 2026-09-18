package com.dburnwal.inspectionassistant.dto;

import java.util.List;

public record InspectionResponse(
        String sessionId,
        List<FindingResponse> findings
) {}
