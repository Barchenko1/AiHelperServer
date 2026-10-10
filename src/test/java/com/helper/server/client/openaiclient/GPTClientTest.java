package com.helper.server.client.openaiclient;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GPTClientTest {

    private HttpServer server;
    private final AtomicReference<Headers> seenHeaders = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody = "{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}";

    private GPTClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            seenHeaders.set(exchange.getRequestHeaders());
            seenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();

        client = new GPTClient();
        ReflectionTestUtils.setField(client, "apiKey", "sk-test");
        ReflectionTestUtils.setField(client, "completionsApiUrl",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsPayloadUnchangedWithBearerAuth() {
        String payload = "{\"model\":\"gpt-4o\",\"messages\":[]}";

        client.sendToOpenAI(payload);

        assertThat(seenBody.get()).isEqualTo(payload);
        assertThat(seenHeaders.get().getFirst("Authorization")).isEqualTo("Bearer sk-test");
        assertThat(seenHeaders.get().getFirst("Content-Type")).isEqualTo("application/json");
    }

    @Test
    void returnsAnswerAsJsonEncodedString() {
        responseBody = "{\"choices\":[{\"message\":{\"content\":\"a \\\"b\\\"\\nc\"}}]}";

        String result = client.sendToOpenAI("{}");

        assertThat(JsonParser.parseString(result).getAsString()).isEqualTo("a \"b\"\nc");
    }

    @Test
    void errorStatusThrowsWithOpenAiMessage() {
        status = 429;
        responseBody = "{\"error\":{\"message\":\"You have no credits remaining.\",\"code\":\"credit_balance_exhausted\"}}";

        assertThatThrownBy(() -> client.sendToOpenAI("{}"))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("OpenAI API error 429: " + responseBody);
    }

    @Test
    void successBodyWithoutChoicesThrowsInsteadOfNpe() {
        responseBody = "{\"unexpected\":true}";

        assertThatThrownBy(() -> client.sendToOpenAI("{}"))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(java.io.IOException.class);
    }
}
