package config;

import com.google.gson.JsonParser;
import com.helper.server.client.IAIClient;
import com.helper.server.config.AiClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientConfigTest {

    private static final IAIClient GPT = json -> "\"gpt-answer\"";
    private static final IAIClient CLAUDE = json -> "\"claude-answer\"";

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static AiClientConfig config(String defaultProvider) {
        AiClientConfig config = new AiClientConfig();
        ReflectionTestUtils.setField(config, "defaultProvider", defaultProvider);
        return config;
    }

    private static void requestWithHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) request.addHeader(AiClientConfig.PROVIDER_HEADER, value);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void headerClaudeRoutesToClaude() {
        requestWithHeader("claude");
        assertThat(config("openai").aiTextClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"claude-answer\"");
    }

    @Test
    void headerOpenAiRoutesToGPTEvenWhenDefaultIsClaude() {
        requestWithHeader("openai");
        assertThat(config("claude").aiTextClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"gpt-answer\"");
    }

    @Test
    void headerIsCaseAndWhitespaceInsensitive() {
        requestWithHeader("  CLAUDE ");
        assertThat(config("openai").aiScreenClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"claude-answer\"");
    }

    @Test
    void missingHeaderFallsBackToClaudeDefault() {
        requestWithHeader(null);
        assertThat(config("claude").aiScreenClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"claude-answer\"");
    }

    @Test
    void blankHeaderFallsBackToDefault() {
        requestWithHeader("   ");
        assertThat(config("claude").aiTextClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"claude-answer\"");
    }

    @Test
    void missingHeaderUsesOpenAiWhenThatIsTheConfiguredDefault() {
        requestWithHeader(null);
        assertThat(config("openai").aiTextClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"gpt-answer\"");
    }

    @Test
    void unknownProviderValueIsNotClaude() {
        requestWithHeader("gemini");
        assertThat(config("claude").aiTextClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"gpt-answer\"");
    }

    @Test
    void usesDefaultWhenCalledOutsideARequest() {
        assertThat(config("claude").aiTextClient(GPT, CLAUDE).sendToAI("{}")).isEqualTo("\"claude-answer\"");
    }

    @Test
    void providerFailureBecomesReadableMessageForTheUi() {
        requestWithHeader("claude");
        IAIClient failing = json -> {
            throw new RuntimeException(new java.io.IOException("Claude API error 429: no credits"));
        };

        String result = config("claude").aiScreenClient(GPT, failing).sendToAI("{}");

        assertThat(JsonParser.parseString(result).getAsString()).isEqualTo("⚠️ Claude API error 429: no credits");
    }
}
