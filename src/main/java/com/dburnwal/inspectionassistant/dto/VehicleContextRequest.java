package com.dburnwal.inspectionassistant.dto;

public record VehicleContextRequest(
        String make,
        String model,
        Integer year,
        String variant,
        String city
) {}
