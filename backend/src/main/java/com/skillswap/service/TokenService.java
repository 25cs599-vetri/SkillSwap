package com.skillswap.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    public String create(Long studentId) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenEntry(studentId, Instant.now().plusSeconds(72 * 3600)));
        return token;
    }

    public Long getStudentId(String token) {
        TokenEntry entry = tokens.get(token);
        if (entry == null) return null;
        if (Instant.now().isAfter(entry.expiresAt())) {
            tokens.remove(token);
            return null;
        }
        return entry.studentId();
    }

    public void remove(String token) { tokens.remove(token); }

    private record TokenEntry(Long studentId, Instant expiresAt) {}
}
