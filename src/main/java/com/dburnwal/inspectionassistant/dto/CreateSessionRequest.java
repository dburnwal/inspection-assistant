package com.dburnwal.inspectionassistant.dto;

public record CreateSessionRequest(
        String profileId,
        VehicleContextRequest vehicle
) {}
