package com.dburnwal.inspectionassistant.inspection.ports;

/**
 * Port for AI vision analysis. Implement to swap AI providers.
 */
public interface VisionAnalysisPort {
    VisionAnalysisResult analyze(byte[] imageBytes, InspectionProfile profile);
}
