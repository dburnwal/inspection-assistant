package com.dburnwal.inspectionassistant.inspection.ports;

import java.util.List;

/**
 * Defines what an inspection profile looks for.
 * Implement this interface to add a new inspection domain.
 */
public interface InspectionProfile {
    String getId();
    String getName();
    String getDescription();
    List<String> getSupportedFindingTypes();
    String buildPrompt();
}
