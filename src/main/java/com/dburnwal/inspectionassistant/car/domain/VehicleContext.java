package com.dburnwal.inspectionassistant.car.domain;

/**
 * Optional vehicle context collected before inspection.
 * Car-specific — not part of the generic InspectionSession.
 */
public record VehicleContext(
        String make,
        String model,
        Integer year,
        String variant,
        String city
) {
    public static VehicleContext unknown() {
        return new VehicleContext("Unknown", "Unknown", null, null, null);
    }
}
