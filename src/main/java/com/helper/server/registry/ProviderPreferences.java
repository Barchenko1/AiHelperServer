package com.helper.server.registry;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory per-user LLM provider choice (set from the UI); lost on server restart. */
@Component
public class ProviderPreferences {
    public static final String OPENAI = "openai";
    public static final String CLAUDE = "claude";

    private final Map<String, String> byUser = new ConcurrentHashMap<>();

    public Optional<String> get(String username) {
        return Optional.ofNullable(byUser.get(username));
    }

    public void set(String username, String provider) {
        byUser.put(username, provider);
    }

    public static boolean isValid(String provider) {
        return OPENAI.equals(provider) || CLAUDE.equals(provider);
    }
}
