package com.helper.server.config;

import com.helper.server.client.IAIClient;
import com.helper.server.util.UiErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class AiClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiClientConfig.class);

    public static final String PROVIDER_HEADER = "X-AI-Provider";

    @Value("${ai.provider:claude}")
    private String defaultProvider;

    @Bean("aiTextClient")
    public IAIClient aiTextClient(@Qualifier("gptClient") IAIClient gpt,
                                  @Qualifier("claudeClient") IAIClient claude) {
        return json -> call(useClaude() ? claude : gpt, json);
    }

    @Bean("aiScreenClient")
    public IAIClient aiScreenClient(@Qualifier("gptClient") IAIClient gpt,
                                    @Qualifier("claudeClient") IAIClient claude) {
        return json -> call(useClaude() ? claude : gpt, json);
    }

    private String call(IAIClient client, String json) {
        try {
            return client.sendToAI(json);
        } catch (RuntimeException e) {
            LOGGER.error("AI call failed", e);
            return UiErrors.toUiMessage(e);
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
