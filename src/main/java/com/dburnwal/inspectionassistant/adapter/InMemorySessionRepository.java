package com.dburnwal.inspectionassistant.adapter;

import com.dburnwal.inspectionassistant.inspection.domain.InspectionSession;
import com.dburnwal.inspectionassistant.inspection.ports.InspectionSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySessionRepository implements InspectionSessionRepository {

    private final Map<String, InspectionSession> store = new ConcurrentHashMap<>();

    @Override
    public InspectionSession save(InspectionSession session) {
        store.put(session.getId(), session);
        return session;
    }

    @Override
    public InspectionSession findById(String id) {
        InspectionSession session = store.get(id);
        if (session == null) throw new SessionNotFoundException(id);
        return session;
    }

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String id) { super("Session not found: " + id); }
    }
}
