package com.dburnwal.inspectionassistant.inspection;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InspectionSessionService {

    private final Map<String, InspectionSession> sessions = new ConcurrentHashMap<>();

    public InspectionSession create() {
        String id = UUID.randomUUID().toString();
        InspectionSession session = new InspectionSession(id);
        sessions.put(id, session);
        return session;
    }

    public InspectionSession get(String sessionId) {
        InspectionSession session = sessions.get(sessionId);
        if (session == null) throw new SessionNotFoundException(sessionId);
        return session;
    }

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String id) { super("Session not found: " + id); }
    }
}
