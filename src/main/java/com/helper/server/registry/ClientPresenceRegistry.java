package com.helper.server.registry;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers when each client type (cli, extension, autoscreen) last checked in with a valid code, per user. */
@Component
public class ClientPresenceRegistry {

    public static final String CLI = "cli";
    public static final String EXTENSION = "extension";
    /** The CLI's F6 auto-screen loop: checks in after every screenshot it delivered. */
    public static final String AUTO_SCREEN = "autoscreen";

    private final Map<String, Map<String, Instant>> lastSeen = new ConcurrentHashMap<>();
    private final Clock clock;

    public ClientPresenceRegistry() {
        this(Clock.systemUTC());
    }

    ClientPresenceRegistry(Clock clock) {
        this.clock = clock;
    }

    public static boolean isKnownClient(String client) {
        return CLI.equals(client) || EXTENSION.equals(client) || AUTO_SCREEN.equals(client);
    }

    public void record(String username, String client) {
        lastSeen.computeIfAbsent(username, u -> new ConcurrentHashMap<>()).put(client, clock.instant());
    }

    /** A client announced it is going away (stopped, or switched to another code): red right away. */
    public void forget(String username, String client) {
        Map<String, Instant> seen = lastSeen.get(username);
        if (seen != null) seen.remove(client);
    }

    /** Seconds since each known client last checked in; a client that never did is absent from the map. */
    public Map<String, Long> secondsSinceSeen(String username) {
        Map<String, Long> result = new HashMap<>();
        Map<String, Instant> seen = lastSeen.getOrDefault(username, Map.of());
        seen.forEach((client, at) -> result.put(client, Math.max(0, Duration.between(at, clock.instant()).getSeconds())));
        return result;
    }
}
