package com.dburnwal.inspectionassistant.inspection;

import com.dburnwal.inspectionassistant.adapter.InMemorySessionRepository;
import com.dburnwal.inspectionassistant.inspection.application.FindingTracker;
import com.dburnwal.inspectionassistant.inspection.application.InspectionService;
import com.dburnwal.inspectionassistant.inspection.application.InspectionSessionService;
import com.dburnwal.inspectionassistant.inspection.domain.Finding;
import com.dburnwal.inspectionassistant.inspection.domain.InspectionGuidance;
import com.dburnwal.inspectionassistant.inspection.domain.Severity;
import com.dburnwal.inspectionassistant.inspection.domain.TrackedFinding;
import com.dburnwal.inspectionassistant.inspection.ports.InspectionProfile;
import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisPort;
import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InspectionServiceTest {

    private InspectionService service;
    private VisionAnalysisPort visionPort;

    private final InspectionProfile testProfile = new InspectionProfile() {
        public String getId() { return "TEST"; }
        public String getName() { return "Test Profile"; }
        public String getDescription() { return "Test"; }
        public List<String> getSupportedFindingTypes() { return List.of("DENT", "SCRATCH"); }
        public String buildPrompt() { return "test prompt"; }
    };

    @BeforeEach
    void setUp() {
        visionPort = mock(VisionAnalysisPort.class);
        InspectionSessionService sessionService = new InspectionSessionService(new InMemorySessionRepository());
        FindingTracker tracker = new FindingTracker();
        service = new InspectionService(sessionService, visionPort, tracker, List.of(testProfile));
    }

    @Test
    void createSessionWithValidProfile() {
        var session = service.createSession("TEST");
        assertThat(session.getId()).isNotNull();
        assertThat(session.getProfileId()).isEqualTo("TEST");
    }

    @Test
    void createSessionWithUnknownProfileThrows() {
        assertThatThrownBy(() -> service.createSession("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processFrameReturnsFindingsFromVisionPort() {
        Finding finding = new Finding(UUID.randomUUID().toString(), "DENT", "REAR_DOOR",
                "Possible dent", 0.9, Severity.MEDIUM, false, "", Instant.now());
        when(visionPort.analyze(any(), any()))
                .thenReturn(new VisionAnalysisResult(List.of(finding), InspectionGuidance.of("")));

        var session = service.createSession("TEST");
        var result = service.processFrame(session.getId(), new byte[]{1, 2, 3});

        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).getLatest().type()).isEqualTo("DENT");
    }

    @Test
    void lowConfidenceFindingsAreFiltered() {
        Finding lowConf = new Finding(UUID.randomUUID().toString(), "SCRATCH", "HOOD",
                "Faint scratch", 0.50, Severity.LOW, false, "", Instant.now());
        when(visionPort.analyze(any(), any()))
                .thenReturn(new VisionAnalysisResult(List.of(lowConf), InspectionGuidance.of("")));

        var session = service.createSession("TEST");
        var result = service.processFrame(session.getId(), new byte[]{1});

        assertThat(result.findings()).isEmpty();
    }

    @Test
    void completeSessionReturnsOnlyConfirmedFindings() {
        Finding finding = new Finding(UUID.randomUUID().toString(), "DENT", "REAR_DOOR",
                "Possible dent", 0.9, Severity.MEDIUM, false, "", Instant.now());
        when(visionPort.analyze(any(), any()))
                .thenReturn(new VisionAnalysisResult(List.of(finding), InspectionGuidance.of("")));

        var session = service.createSession("TEST");
        service.processFrame(session.getId(), new byte[]{1});
        service.processFrame(session.getId(), new byte[]{1});

        List<TrackedFinding> confirmed = service.completeSession(session.getId());
        assertThat(confirmed).hasSize(1);
    }

    @Test
    void openClosedPrinciple_newProfileWorksWithoutModifyingEngine() {
        // Adding a new profile only requires a new InspectionProfile implementation
        InspectionProfile homeProfile = new InspectionProfile() {
            public String getId() { return "HOME"; }
            public String getName() { return "Home Inspection"; }
            public String getDescription() { return "Home"; }
            public List<String> getSupportedFindingTypes() { return List.of("CRACK", "MOISTURE"); }
            public String buildPrompt() { return "home prompt"; }
        };

        InspectionSessionService sessionService = new InspectionSessionService(new InMemorySessionRepository());
        InspectionService serviceWithHome = new InspectionService(
                sessionService, visionPort, new FindingTracker(), List.of(testProfile, homeProfile));

        var session = serviceWithHome.createSession("HOME");
        assertThat(session.getProfileId()).isEqualTo("HOME");
        // No modification to InspectionService was needed
    }
}
