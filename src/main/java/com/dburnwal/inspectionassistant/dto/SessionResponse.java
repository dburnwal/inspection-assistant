package com.dburnwal.inspectionassistant.dto;

import com.dburnwal.inspectionassistant.inspection.domain.SessionStatus;

public record SessionResponse(
        String sessionId,
        String profileId,
        SessionStatus status
) {}
