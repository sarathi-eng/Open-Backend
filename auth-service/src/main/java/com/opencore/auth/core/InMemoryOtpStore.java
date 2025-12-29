package com.opencore.auth.core;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOtpStore {
    private static final Duration TTL = Duration.ofMinutes(5);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> byEmail = new ConcurrentHashMap<>();

    public OtpIssue issue(String email) {
        String requestId = UUID.randomUUID().toString();
        String otp = String.format("%06d", random.nextInt(1_000_000));
        byEmail.put(normalize(email), new Entry(otp, Instant.now().plus(TTL), requestId));
        return new OtpIssue(requestId, otp);
    }

    public boolean verify(String email, String otp, String requestId) {
        Entry entry = byEmail.get(normalize(email));
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            byEmail.remove(normalize(email));
            return false;
        }
        if (!entry.requestId().equals(requestId)) return false;
        if (!entry.otp().equals(otp)) return false;
        byEmail.remove(normalize(email));
        return true;
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record Entry(String otp, Instant expiresAt, String requestId) {}

    public record OtpIssue(String requestId, String otp) {}
}
