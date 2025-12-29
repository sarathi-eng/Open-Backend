package com.opencore.auth.core;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore {
    private final Map<String, Session> sessionsByRefreshJti = new ConcurrentHashMap<>();

    public void put(String refreshJti, Session session) {
        sessionsByRefreshJti.put(refreshJti, session);
    }

    public Session get(String refreshJti) {
        return sessionsByRefreshJti.get(refreshJti);
    }

    public void revoke(String refreshJti) {
        Session s = sessionsByRefreshJti.get(refreshJti);
        if (s != null) {
            sessionsByRefreshJti.put(refreshJti, s.withRevoked(true));
        }
    }

    public record Session(
            String userId,
            String deviceId,
            long expiresAtEpochSeconds,
            boolean revoked
    ) {
        public boolean isExpired() {
            return Instant.now().getEpochSecond() >= expiresAtEpochSeconds;
        }

        public Session withRevoked(boolean value) {
            return new Session(userId, deviceId, expiresAtEpochSeconds, value);
        }
    }
}
