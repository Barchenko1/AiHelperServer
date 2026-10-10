package com.helper.server.config;

import com.google.gson.Gson;
import com.helper.server.client.openaiclient.IOpenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Chooses the LLM backend per request: the client (extension / CLI) sends X-AI-Provider (openai | claude);
 * without the header the server falls back to ai.provider (claude by default).
 * Controllers call the clients synchronously on the request thread.
 */
@Configuration
public class AiClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiClientConfig.class);

    public static final String PROVIDER_HEADER = "X-AI-Provider";

    @Value("${ai.provider:claude}")
    private String defaultProvider;

    @Bean("aiTextClient")
    public IOpenAIClient aiTextClient(@Qualifier("openAITextClient") IOpenAIClient openAi,
                                      @Qualifier("claudeClient") IOpenAIClient claude) {
        return json -> call(useClaude() ? claude : openAi, json);
    }

    @Bean("aiScreenClient")
    public IOpenAIClient aiScreenClient(@Qualifier("openAIScreenClient") IOpenAIClient openAi,
                                        @Qualifier("claudeClient") IOpenAIClient claude) {
        return json -> call(useClaude() ? claude : openAi, json);
    }

    /** Turns provider failures (no credits, bad key, ...) into a message the UI can show instead of a silent 5xx/401. */
    private String call(IOpenAIClient client, String json) {
        try {
            return client.sendToOpenAI(json);
        } catch (RuntimeException e) {
            LOGGER.error("AI call failed", e);
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            return new Gson().toJson("⚠️ AI error: " + root.getMessage());
        }
    }

    private boolean useClaude() {
        String provider = defaultProvider;
        String source = "default";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String header = attrs.getRequest().getHeader(PROVIDER_HEADER);
            if (header != null && !header.isBlank()) {
                provider = header;
                source = "request header";
            }
        }
        LOGGER.info("AI provider for this request: {} (source: {})", provider.trim(), source);
        return "claude".equalsIgnoreCase(provider.trim());
    }
}
