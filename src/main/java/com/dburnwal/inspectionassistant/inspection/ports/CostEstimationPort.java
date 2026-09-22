package com.dburnwal.inspectionassistant.inspection.ports;

/**
 * Port for cost estimation. Implement per inspection domain.
 */
public interface CostEstimationPort {

    record CostEstimate(
            String currency,
            long minimum,
            long maximum,
            double confidence,
            String disclaimer
    ) {}

    CostEstimate estimate(String findingType, String part, String severity);
}
