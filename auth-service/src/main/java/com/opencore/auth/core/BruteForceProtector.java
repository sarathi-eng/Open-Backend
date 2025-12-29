package com.opencore.auth.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BruteForceProtector {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILURES = 5;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void onFailure(String key) {
        buckets.compute(key, (k, existing) -> {
            Bucket b = existing == null ? new Bucket(0, Instant.now().plus(WINDOW)) : existing;
            if (Instant.now().isAfter(b.resetAt())) {
                return new Bucket(1, Instant.now().plus(WINDOW));
            }
            return new Bucket(b.failures() + 1, b.resetAt());
        });
    }

    public void onSuccess(String key) {
        buckets.remove(key);
    }

    public boolean isBlocked(String key) {
        Bucket b = buckets.get(key);
        if (b == null) return false;
        if (Instant.now().isAfter(b.resetAt())) {
            buckets.remove(key);
            return false;
        }
        return b.failures() >= MAX_FAILURES;
    }

    public record Bucket(int failures, Instant resetAt) {}
}
