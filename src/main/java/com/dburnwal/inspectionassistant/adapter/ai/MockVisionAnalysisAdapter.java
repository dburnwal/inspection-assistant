package com.dburnwal.inspectionassistant.adapter.ai;

import com.dburnwal.inspectionassistant.inspection.domain.Finding;
import com.dburnwal.inspectionassistant.inspection.domain.InspectionGuidance;
import com.dburnwal.inspectionassistant.inspection.domain.Severity;
import com.dburnwal.inspectionassistant.inspection.ports.InspectionProfile;
import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisPort;
import com.dburnwal.inspectionassistant.inspection.ports.VisionAnalysisResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic mock AI adapter. Does not require Ollama.
 * Responses are keyed by frameId header (X-Frame-Id) embedded in imageBytes metadata,
 * or fall back to a default response set.
 *
 * Activated via: inspection.ai.provider=mock
 */
public class MockVisionAnalysisAdapter implements VisionAnalysisPort {

    // Deterministic responses keyed by frameId
    private static final Map<String, VisionAnalysisResult> RESPONSES = Map.ofEntries(
            // DENTED_CAR scenario
            entry("dent-front",
                    findings(finding("NO_OBVIOUS_DAMAGE", "FRONT_BUMPER", "No visible damage on the front.", 0.92, Severity.LOW, false, "")),
                    guidance("MOVE_CAMERA", "RIGHT", "Move to the left side of the car.")),
            entry("dent-rear-door",
                    findings(finding("DENT", "REAR_LEFT_DOOR", "Possible moderate dent visible on the rear left door panel.", 0.91, Severity.MEDIUM, true, "Move the camera closer to the rear door.")),
                    guidance("MOVE_CAMERA", "CLOSER", "Move closer to inspect the dent.")),
            entry("dent-bumper-scratch",
                    findings(
                            finding("SCRATCH", "REAR_BUMPER", "Possible scratch visible on the rear bumper.", 0.85, Severity.LOW, false, ""),
                            finding("PAINT_DAMAGE", "REAR_BUMPER", "Possible paint damage near the scratch.", 0.78, Severity.LOW, false, "")
                    ),
                    guidance("NONE", "NONE", "Good angle. Continue scanning.")),
            entry("dent-rear",
                    findings(finding("DENT", "REAR_LEFT_DOOR", "Possible moderate dent still visible from rear angle.", 0.94, Severity.MEDIUM, false, "")),
                    guidance("MOVE_CAMERA", "LEFT", "Move to the right side of the car.")),

            // SCRATCHED_CAR scenario
            entry("scratch-front",
                    findings(finding("NO_OBVIOUS_DAMAGE", "FRONT_BUMPER", "No visible damage on the front.", 0.90, Severity.LOW, false, "")),
                    guidance("MOVE_CAMERA", "RIGHT", "Move to the left side.")),
            entry("scratch-left-door",
                    findings(finding("SCRATCH", "FRONT_LEFT_DOOR", "Possible deep scratch visible on the front left door.", 0.88, Severity.MEDIUM, true, "Move closer to the door.")),
                    guidance("MOVE_CAMERA", "CLOSER", "Move closer to the scratch.")),
            entry("scratch-rear",
                    findings(finding("SCRATCH", "REAR_BUMPER", "Possible light scratch on the rear bumper.", 0.76, Severity.LOW, false, "")),
                    guidance("NONE", "NONE", "Continue scanning.")),
            entry("scratch-right-side",
                    findings(finding("NO_OBVIOUS_DAMAGE", "FRONT_RIGHT_DOOR", "No visible damage on the right side.", 0.91, Severity.LOW, false, "")),
                    guidance("NONE", "NONE", "Inspection complete.")),

            // CLEAN_CAR scenario — all frames return no damage
            entry("clean-front",       noFindings("FRONT_BUMPER",  "MOVE_CAMERA", "RIGHT",  "Move to the left side.")),
            entry("clean-front-left",  noFindings("FRONT_LEFT_FENDER", "MOVE_CAMERA", "RIGHT", "Continue to the left side.")),
            entry("clean-left-side",   noFindings("FRONT_LEFT_DOOR",   "MOVE_CAMERA", "RIGHT", "Move to the rear.")),
            entry("clean-rear-left",   noFindings("REAR_LEFT_DOOR",    "MOVE_CAMERA", "RIGHT", "Move to the rear.")),
            entry("clean-rear",        noFindings("REAR_BUMPER",       "MOVE_CAMERA", "RIGHT", "Move to the right side.")),
            entry("clean-rear-right",  noFindings("REAR_RIGHT_DOOR",   "MOVE_CAMERA", "RIGHT", "Continue to the right side.")),
            entry("clean-right-side",  noFindings("FRONT_RIGHT_DOOR",  "MOVE_CAMERA", "RIGHT", "Move to the front.")),
            entry("clean-front-right", noFindings("FRONT_RIGHT_FENDER","NONE",        "NONE",  "Inspection complete.")),

            // MULTIPLE_DAMAGE scenario
            entry("multi-front",        noFindings("FRONT_BUMPER", "MOVE_CAMERA", "RIGHT", "Move to the rear.")),
            entry("multi-rear-door",
                    findings(finding("DENT", "REAR_LEFT_DOOR", "Possible dent on rear left door.", 0.89, Severity.MEDIUM, true, "Move closer.")),
                    guidance("MOVE_CAMERA", "CLOSER", "Move closer to the dent.")),
            entry("multi-bumper",
                    findings(finding("BUMPER_DAMAGE", "REAR_BUMPER", "Possible bumper damage visible.", 0.82, Severity.MEDIUM, false, "")),
                    guidance("NONE", "NONE", "Continue scanning.")),
            entry("multi-left-scratch",
                    findings(finding("SCRATCH", "FRONT_LEFT_DOOR", "Possible scratch on front left door.", 0.87, Severity.LOW, false, "")),
                    guidance("NONE", "NONE", "Continue scanning.")),
            entry("multi-rear",
                    findings(finding("DENT", "REAR_LEFT_DOOR", "Dent still visible from rear angle.", 0.93, Severity.MEDIUM, false, "")),
                    guidance("NONE", "NONE", "Inspection complete.")),

            // DIFFICULT_VISIBILITY scenario
            entry("diff-glare",
                    new VisionAnalysisResult(List.of(),
                            new InspectionGuidance("ADJUST_ANGLE", "NONE", "Reflection is obscuring the surface. Try another angle."))),
            entry("diff-blurry",
                    new VisionAnalysisResult(List.of(),
                            new InspectionGuidance("NONE", "NONE", "Unable to inspect reliably. Please hold the camera steady."))),
            entry("diff-partial",
                    new VisionAnalysisResult(List.of(),
                            new InspectionGuidance("MOVE_CAMERA", "FARTHER", "Move back slightly to show more of the panel."))),
            entry("diff-damage",
                    findings(finding("SCRATCH", "FRONT_LEFT_DOOR", "Possible scratch visible despite difficult lighting.", 0.74, Severity.LOW, true, "Move closer for a clearer view.")),
                    guidance("MOVE_CAMERA", "CLOSER", "Move closer for a clearer view."))
    );

    private static final VisionAnalysisResult DEFAULT_RESPONSE = new VisionAnalysisResult(
            List.of(),
            new InspectionGuidance("NONE", "NONE", "Scanning…")
    );

    @Override
    public VisionAnalysisResult analyze(byte[] imageBytes, InspectionProfile profile) {
        String frameId = extractFrameId(imageBytes);
        return RESPONSES.getOrDefault(frameId, DEFAULT_RESPONSE);
    }

    /**
     * Frame ID is encoded as ASCII text at the start of the byte array when sent from demo mode.
     * Format: "FRAME_ID:<id>\n" followed by image bytes (or just the marker for tests).
     * Falls back to DEFAULT_RESPONSE for real camera frames.
     */
    public static byte[] encodeFrameId(String frameId, byte[] imageBytes) {
        byte[] prefix = ("FRAME_ID:" + frameId + "\n").getBytes();
        byte[] result = new byte[prefix.length + imageBytes.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(imageBytes, 0, result, prefix.length, imageBytes.length);
        return result;
    }

    private String extractFrameId(byte[] imageBytes) {
        try {
            String header = new String(imageBytes, 0, Math.min(64, imageBytes.length));
            if (header.startsWith("FRAME_ID:")) {
                int end = header.indexOf('\n');
                if (end > 0) return header.substring(9, end);
            }
        } catch (Exception ignored) {}
        return "";
    }

    // --- builder helpers ---

    private static Map.Entry<String, VisionAnalysisResult> entry(String frameId, List<Finding> findings, InspectionGuidance guidance) {
        return Map.entry(frameId, new VisionAnalysisResult(findings, guidance));
    }

    private static Map.Entry<String, VisionAnalysisResult> entry(String frameId, VisionAnalysisResult result) {
        return Map.entry(frameId, result);
    }

    private static List<Finding> findings(Finding... fs) { return List.of(fs); }

    private static InspectionGuidance guidance(String action, String direction, String message) {
        return new InspectionGuidance(action, direction, message);
    }

    private static Finding finding(String type, String part, String desc, double conf, Severity sev, boolean closer, String guidanceMsg) {
        return new Finding(UUID.randomUUID().toString(), type, part, desc, conf, sev, closer, guidanceMsg, Instant.now());
    }

    private static VisionAnalysisResult noFindings(String part, String action, String direction, String message) {
        return new VisionAnalysisResult(
                List.of(finding("NO_OBVIOUS_DAMAGE", part, "No visible damage detected.", 0.92, Severity.LOW, false, "")),
                new InspectionGuidance(action, direction, message)
        );
    }
}
