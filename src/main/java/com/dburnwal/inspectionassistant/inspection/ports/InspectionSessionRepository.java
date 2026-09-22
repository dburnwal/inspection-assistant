package com.dburnwal.inspectionassistant.inspection.ports;

import com.dburnwal.inspectionassistant.inspection.domain.InspectionSession;

/**
 * Port for persisting inspection sessions.
 */
public interface InspectionSessionRepository {
    InspectionSession save(InspectionSession session);
    InspectionSession findById(String id);
}
