package com.dburnwal.inspectionassistant.car;

import com.dburnwal.inspectionassistant.car.cost.CarRepairCostEstimator;
import com.dburnwal.inspectionassistant.inspection.ports.CostEstimationPort.CostEstimate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarRepairCostEstimatorTest {

    private final CarRepairCostEstimator estimator = new CarRepairCostEstimator();

    @Test
    void dentMediumReturnsExpectedRange() {
        CostEstimate estimate = estimator.estimate("DENT", "REAR_DOOR", "MEDIUM");
        assertThat(estimate.currency()).isEqualTo("INR");
        assertThat(estimate.minimum()).isEqualTo(4500);
        assertThat(estimate.maximum()).isEqualTo(8000);
        assertThat(estimate.disclaimer()).isNotBlank();
    }

    @Test
    void scratchLowReturnsExpectedRange() {
        CostEstimate estimate = estimator.estimate("SCRATCH", "HOOD", "LOW");
        assertThat(estimate.minimum()).isEqualTo(500);
        assertThat(estimate.maximum()).isEqualTo(1500);
    }

    @Test
    void paintDamageMediumReturnsExpectedRange() {
        CostEstimate estimate = estimator.estimate("PAINT_DAMAGE", "FRONT_LEFT_DOOR", "MEDIUM");
        assertThat(estimate.minimum()).isGreaterThan(0);
        assertThat(estimate.maximum()).isGreaterThan(estimate.minimum());
    }

    @Test
    void unknownDamageTypeReturnsFallback() {
        CostEstimate estimate = estimator.estimate("UNKNOWN_DAMAGE", "UNKNOWN", "LOW");
        assertThat(estimate.minimum()).isGreaterThan(0);
        assertThat(estimate.maximum()).isGreaterThan(estimate.minimum());
    }
}
