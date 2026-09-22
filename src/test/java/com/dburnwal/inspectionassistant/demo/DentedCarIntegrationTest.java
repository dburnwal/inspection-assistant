package com.dburnwal.inspectionassistant.demo;

import com.dburnwal.inspectionassistant.adapter.InMemorySessionRepository;
import com.dburnwal.inspectionassistant.adapter.ai.MockVisionAnalysisAdapter;
import com.dburnwal.inspectionassistant.car.cost.CarRepairCostEstimator;
import com.dburnwal.inspectionassistant.car.profile.CarDamageInspectionProfile;
import com.dburnwal.inspectionassistant.inspection.application.FindingTracker;
import com.dburnwal.inspectionassistant.inspection.application.InspectionService;
import com.dburnwal.inspectionassistant.inspection.application.InspectionSessionService;
import com.dburnwal.inspectionassistant.inspection.domain.FindingStatus;
import com.dburnwal.inspectionassistant.inspection.domain.TrackedFinding;
import com.dburnwal.inspectionassistant.inspection.ports.CostEstimationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: DENTED_CAR scenario + MockVisionAnalysisAdapter.
 * No Ollama required.
 */
class DentedCarIntegrationTest {

    private InspectionService service;
    private CostEstimationPort costEstimator;

    @BeforeEach
    void setUp() {
        MockVisionAnalysisAdapter mockAi = new MockVisionAnalysisAdapter();
        InspectionSessionService sessionService = new InspectionSessionService(new InMemorySessionRepository());
        FindingTracker tracker = new FindingTracker();
        CarDamageInspectionProfile profile = new CarDamageInspectionProfile();
        service = new InspectionService(sessionService, mockAi, tracker, List.of(profile));
        costEstimator = new CarRepairCostEstimator();
    }

    @Test
    void dentedCarScenario_rearDoorDentIsConfirmedAfterMultipleFrames() {
        var session = service.createSession("CAR_DAMAGE");
        String sid = session.getId();

        // Frame 1: front — no damage
        submitFrame(sid, "dent-front");

        // Frame 2: rear door dent — first observation
        var result2 = submitFrame(sid, "dent-rear-door");
        assertThat(result2.findings()).anyMatch(tf -> tf.getLatest().type().equals("DENT"));

        // Frame 3: rear door dent again — should confirm
        submitFrame(sid, "dent-rear-door");

        // Frame 4: bumper scratch
        submitFrame(sid, "dent-bumper-scratch");

        // Complete and get report
        List<TrackedFinding> confirmed = service.completeSession(sid);

        // Rear door dent must be confirmed (seen twice)
        assertThat(confirmed).anyMatch(tf ->
                tf.getLatest().type().equals("DENT") &&
                tf.getLatest().part().equals("REAR_LEFT_DOOR") &&
                tf.getStatus() == FindingStatus.CONFIRMED
        );
    }

    @Test
    void dentedCarScenario_noDuplicateFindingsForSameDent() {
        var session = service.createSession("CAR_DAMAGE");
        String sid = session.getId();

        submitFrame(sid, "dent-rear-door");
        submitFrame(sid, "dent-rear-door");
        submitFrame(sid, "dent-rear-door");

        List<TrackedFinding> all = service.getFindings(sid);
        long dentCount = all.stream()
                .filter(tf -> tf.getLatest().type().equals("DENT") && tf.getLatest().part().equals("REAR_LEFT_DOOR"))
                .count();

        assertThat(dentCount).isEqualTo(1);
        assertThat(all.stream().filter(tf -> tf.getLatest().type().equals("DENT")).findFirst())
                .get().extracting(TrackedFinding::getObservationCount).isEqualTo(3);
    }

    @Test
    void dentedCarScenario_repairEstimateExistsForConfirmedDent() {
        var session = service.createSession("CAR_DAMAGE");
        String sid = session.getId();
        submitFrame(sid, "dent-rear-door");
        submitFrame(sid, "dent-rear-door");

        List<TrackedFinding> confirmed = service.completeSession(sid);
        TrackedFinding dent = confirmed.stream()
                .filter(tf -> tf.getLatest().type().equals("DENT"))
                .findFirst().orElseThrow();

        CostEstimationPort.CostEstimate estimate = costEstimator.estimate(
                dent.getLatest().type(), dent.getLatest().part(), dent.getLatest().severity().name());

        assertThat(estimate.currency()).isEqualTo("INR");
        assertThat(estimate.minimum()).isGreaterThan(0);
        assertThat(estimate.maximum()).isGreaterThan(estimate.minimum());
        assertThat(estimate.disclaimer()).isNotBlank();
    }

    @Test
    void cleanCarScenario_noConfirmedFindings() {
        var session = service.createSession("CAR_DAMAGE");
        String sid = session.getId();

        for (String frameId : List.of("clean-front", "clean-left-side", "clean-rear", "clean-right-side")) {
            submitFrame(sid, frameId);
        }

        List<TrackedFinding> confirmed = service.completeSession(sid);
        // NO_OBVIOUS_DAMAGE findings have confidence 0.92 but are below confirmation threshold (seen once each)
        assertThat(confirmed.stream().filter(tf -> tf.getLatest().type().equals("DENT"))).isEmpty();
    }

    private InspectionService.FrameAnalysisResult submitFrame(String sessionId, String frameId) {
        byte[] bytes = MockVisionAnalysisAdapter.encodeFrameId(frameId, new byte[]{1, 2, 3});
        return service.processFrame(sessionId, bytes);
    }
}
