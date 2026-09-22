package com.dburnwal.inspectionassistant.inspection.application;

import com.dburnwal.inspectionassistant.inspection.domain.InspectionSession;
import com.dburnwal.inspectionassistant.inspection.ports.InspectionSessionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InspectionSessionService {

    private final InspectionSessionRepository repository;

    public InspectionSessionService(InspectionSessionRepository repository) {
        this.repository = repository;
    }

    public InspectionSession create(String profileId) {
        InspectionSession session = new InspectionSession(UUID.randomUUID().toString(), profileId);
        return repository.save(session);
    }

    public InspectionSession get(String sessionId) {
        return repository.findById(sessionId);
    }

    public InspectionSession complete(String sessionId) {
        InspectionSession session = repository.findById(sessionId);
        session.complete();
        return repository.save(session);
    }
}
