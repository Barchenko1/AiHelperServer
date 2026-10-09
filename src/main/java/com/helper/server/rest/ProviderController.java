package com.helper.server.rest;

import com.helper.server.registry.ProviderPreferences;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/provider")
public class ProviderController {

    private final ProviderPreferences preferences;
    private final String defaultProvider;

    public ProviderController(ProviderPreferences preferences,
                              @Value("${ai.provider:openai}") String defaultProvider) {
        this.preferences = preferences;
        this.defaultProvider = defaultProvider.trim().toLowerCase();
    }

    @GetMapping
    public Map<String, String> get(Principal principal) {
        return Map.of("provider", preferences.get(principal.getName()).orElse(defaultProvider));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> set(@RequestBody Map<String, String> body, Principal principal) {
        String provider = body.getOrDefault("provider", "").trim().toLowerCase();
        if (!ProviderPreferences.isValid(provider)) {
            return ResponseEntity.badRequest().build();
        }
        preferences.set(principal.getName(), provider);
        return ResponseEntity.ok(Map.of("provider", provider));
    }
}
