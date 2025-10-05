package com.helper.server.util;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CodeToTokenService {
    private static final SecureRandom RNG = new SecureRandom();

    private static final class Entry {
        final String token;
        final Instant exp;
        Entry(String token, Instant exp) { this.token = token; this.exp = exp; }
    }

    private final Map<String, Entry> map = new ConcurrentHashMap<>();

    /** Issue a 6-digit code that maps to this JWT and is valid for the given TTL. */
    public String issueForToken(String jwt, Duration ttl) {
        String code = String.format("%06d", RNG.nextInt(1_000_000));
        map.put(code, new Entry(jwt, Instant.now().plus(ttl)));
        return code;
    }

    /** Reusable lookup (does NOT consume). Returns the mapped JWT or null if invalid/expired. */
    public String peekToken(String code) {
        if (code == null) return null;
        Entry e = map.get(code);
        if (e == null) return null;
        if (Instant.now().isAfter(e.exp)) { map.remove(code); return null; }
        return e.token;
    }

    /** Optional: revoke a code early. */
    public void revoke(String code) { if (code != null) map.remove(code); }
}
