package com.dburnwal.inspectionassistant.car.profile;

import com.dburnwal.inspectionassistant.car.domain.CarFindingType;
import com.dburnwal.inspectionassistant.car.domain.CarPart;
import com.dburnwal.inspectionassistant.inspection.ports.InspectionProfile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CarDamageInspectionProfile implements InspectionProfile {

    public static final String ID = "CAR_DAMAGE";

    @Override
    public String getId() { return ID; }

    @Override
    public String getName() { return "Car Damage Inspection"; }

    @Override
    public String getDescription() {
        return "Identifies visible exterior damage on a car including dents, scratches, paint damage, cracks, and broken parts.";
    }

    @Override
    public List<String> getSupportedFindingTypes() {
        return Arrays.stream(CarFindingType.values()).map(Enum::name).collect(Collectors.toList());
    }

    @Override
    public String buildPrompt() {
        String types = getSupportedFindingTypes().stream().collect(Collectors.joining(", "));
        String parts = Arrays.stream(CarPart.values()).map(Enum::name).collect(Collectors.joining(", "));

        return """
                You are an AI car damage inspection assistant. Analyze the provided image for visible exterior car damage.

                Supported finding types (use exactly these values):
                %s

                Supported car parts (use exactly these values):
                %s

                Rules:
                - Report only damage that is clearly visible in the image.
                - Do NOT invent damage. If nothing is wrong, return an empty findings array.
                - Use cautious language: "possible", "appears to be", "visible mark that may indicate".
                - Do NOT claim structural, mechanical, or roadworthiness assessments.
                - Confidence must be between 0.0 and 1.0.
                - Severity must be LOW, MEDIUM, or HIGH.
                - If the image does not show a car or is unclear, return an empty findings array.
                - guidanceMessage should help the user reposition the camera if needed.
                - guidance.action: MOVE_CAMERA, INSPECT_AREA, ADJUST_ANGLE, or NONE
                - guidance.direction: LEFT, RIGHT, UP, DOWN, CLOSER, FARTHER, or NONE

                Return ONLY valid JSON, no markdown, no explanation:
                {
                  "findings": [
                    {
                      "type": "DENT",
                      "part": "REAR_DOOR",
                      "description": "Possible moderate dent visible on the rear door panel.",
                      "confidence": 0.85,
                      "severity": "MEDIUM",
                      "requiresCloserInspection": true,
                      "guidanceMessage": "Move the camera closer to the rear door."
                    }
                  ],
                  "guidance": {
                    "action": "MOVE_CAMERA",
                    "direction": "CLOSER",
                    "message": "Move the camera closer to inspect the rear door."
                  }
                }
                """.formatted(types, parts);
    }
}
