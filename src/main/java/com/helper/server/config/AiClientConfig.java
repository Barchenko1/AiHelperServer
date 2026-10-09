package com.helper.server.config;

import com.helper.server.openaiclient.IOpenAIClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.helper.server.registry.ProviderPreferences;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Chooses the LLM backend per request via the X-AI-Provider header (openai | claude),
 * then the user's choice from the UI (ProviderPreferences), then ai.provider. Controllers call the clients synchronously on the request thread.
 */
@Configuration
public class AiClientConfig {

    public static final String PROVIDER_HEADER = "X-AI-Provider";

    @Value("${ai.provider:openai}")
    private String defaultProvider;

    private final ProviderPreferences preferences;

    public AiClientConfig(ProviderPreferences preferences) {
        this.preferences = preferences;
    }

    @Bean("aiTextClient")
    public IOpenAIClient aiTextClient(@Qualifier("openAITextClient") IOpenAIClient openAi,
                                      @Qualifier("claudeClient") IOpenAIClient claude) {
        return json -> useClaude() ? claude.sendToOpenAI(json) : openAi.sendToOpenAI(json);
    }

    @Bean("aiScreenClient")
    public IOpenAIClient aiScreenClient(@Qualifier("openAIScreenClient") IOpenAIClient openAi,
                                        @Qualifier("claudeClient") IOpenAIClient claude) {
        return json -> useClaude() ? claude.sendToOpenAI(json) : openAi.sendToOpenAI(json);
    }

    private boolean useClaude() {
        String provider = defaultProvider;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            provider = preferences.get(auth.getName()).orElse(provider);
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String header = attrs.getRequest().getHeader(PROVIDER_HEADER);
            if (header != null && !header.isBlank()) provider = header;
        }
        return "claude".equalsIgnoreCase(provider.trim());
    }
}
