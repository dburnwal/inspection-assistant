package com.dburnwal.inspectionassistant.car.cost;

import com.dburnwal.inspectionassistant.inspection.ports.CostEstimationPort;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * India-specific car repair cost estimator using configurable mock pricing data.
 * Replace the pricing map with a real data source (DB, API) without changing the port.
 */
@Component
public class CarRepairCostEstimator implements CostEstimationPort {

    private static final String DISCLAIMER =
            "AI-assisted estimate based on visible damage only. Actual workshop quotation may differ. Hidden damage cannot be assessed from images.";

    // key: "TYPE_SEVERITY", value: {min, max} in INR
    private static final Map<String, long[]> PRICING = Map.ofEntries(
            Map.entry("DENT_LOW",          new long[]{2000,  4000}),
            Map.entry("DENT_MEDIUM",       new long[]{4500,  8000}),
            Map.entry("DENT_HIGH",         new long[]{8000, 18000}),
            Map.entry("SCRATCH_LOW",       new long[]{500,   1500}),
            Map.entry("SCRATCH_MEDIUM",    new long[]{1500,  3000}),
            Map.entry("SCRATCH_HIGH",      new long[]{3000,  6000}),
            Map.entry("PAINT_DAMAGE_LOW",  new long[]{1500,  3500}),
            Map.entry("PAINT_DAMAGE_MEDIUM", new long[]{3500, 7000}),
            Map.entry("PAINT_DAMAGE_HIGH", new long[]{7000, 15000}),
            Map.entry("CRACK_LOW",         new long[]{1000,  3000}),
            Map.entry("CRACK_MEDIUM",      new long[]{3000,  8000}),
            Map.entry("CRACK_HIGH",        new long[]{8000, 20000}),
            Map.entry("BUMPER_DAMAGE_LOW",  new long[]{2000,  5000}),
            Map.entry("BUMPER_DAMAGE_MEDIUM", new long[]{5000, 12000}),
            Map.entry("BUMPER_DAMAGE_HIGH", new long[]{12000, 25000}),
            Map.entry("LIGHT_DAMAGE_LOW",  new long[]{1500,  4000}),
            Map.entry("LIGHT_DAMAGE_MEDIUM", new long[]{4000, 9000}),
            Map.entry("LIGHT_DAMAGE_HIGH", new long[]{9000, 20000}),
            Map.entry("BROKEN_PART_LOW",   new long[]{2000,  6000}),
            Map.entry("BROKEN_PART_MEDIUM", new long[]{6000, 15000}),
            Map.entry("BROKEN_PART_HIGH",  new long[]{15000, 35000}),
            Map.entry("MISSING_PART_LOW",  new long[]{1000,  4000}),
            Map.entry("MISSING_PART_MEDIUM", new long[]{4000, 12000}),
            Map.entry("MISSING_PART_HIGH", new long[]{12000, 30000})
    );

    @Override
    public CostEstimate estimate(String findingType, String part, String severity) {
        String key = findingType + "_" + severity;
        long[] range = PRICING.getOrDefault(key, new long[]{1000, 5000});
        return new CostEstimate("INR", range[0], range[1], 0.70, DISCLAIMER);
    }
}
