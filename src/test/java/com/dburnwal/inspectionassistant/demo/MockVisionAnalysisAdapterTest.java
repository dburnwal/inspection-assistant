package com.dburnwal.inspectionassistant.demo;

import com.dburnwal.inspectionassistant.adapter.ai.MockVisionAnalysisAdapter;
import com.dburnwal.inspectionassistant.car.profile.CarDamageInspectionProfile;
import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockVisionAnalysisAdapterTest {

    private final MockVisionAnalysisAdapter adapter = new MockVisionAnalysisAdapter();
    private final CarDamageInspectionProfile profile = new CarDamageInspectionProfile();

    @Test
    void dentedCarRearDoorFrameReturnsDentFinding() {
        byte[] bytes = MockVisionAnalysisAdapter.encodeFrameId("dent-rear-door", new byte[]{1, 2, 3});
        VisionAnalysisResult result = adapter.analyze(bytes, profile);
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).type()).isEqualTo("DENT");
        assertThat(result.findings().get(0).part()).isEqualTo("REAR_LEFT_DOOR");
    }

    @Test
    void responseIsDeterministic() {
        byte[] bytes = MockVisionAnalysisAdapter.encodeFrameId("dent-rear-door", new byte[]{1});
        VisionAnalysisResult r1 = adapter.analyze(bytes, profile);
        VisionAnalysisResult r2 = adapter.analyze(bytes, profile);
        assertThat(r1.findings().get(0).type()).isEqualTo(r2.findings().get(0).type());
        assertThat(r1.findings().get(0).part()).isEqualTo(r2.findings().get(0).part());
        assertThat(r1.findings().get(0).severity()).isEqualTo(r2.findings().get(0).severity());
    }

    @Test
    void unknownFrameIdReturnsDefaultEmptyResponse() {
        byte[] bytes = MockVisionAnalysisAdapter.encodeFrameId("unknown-frame-xyz", new byte[]{1});
        VisionAnalysisResult result = adapter.analyze(bytes, profile);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void realCameraFrameWithoutFrameIdReturnsDefault() {
        byte[] realJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00, 0x01};
        VisionAnalysisResult result = adapter.analyze(realJpegBytes, profile);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void cleanCarFrameReturnsNoObviousDamage() {
        byte[] bytes = MockVisionAnalysisAdapter.encodeFrameId("clean-front", new byte[]{1});
        VisionAnalysisResult result = adapter.analyze(bytes, profile);
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).type()).isEqualTo("NO_OBVIOUS_DAMAGE");
    }

    @Test
    void difficultGlareFrameReturnsEmptyFindingsWithGuidance() {
        byte[] bytes = MockVisionAnalysisAdapter.encodeFrameId("diff-glare", new byte[]{1});
        VisionAnalysisResult result = adapter.analyze(bytes, profile);
        assertThat(result.findings()).isEmpty();
        assertThat(result.guidance().message()).contains("angle");
    }
}
