package com.helper.server.client.claudeclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.helper.server.client.claudeclient.ClaudeClient;
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

class ClaudeClientTest {

    private HttpServer server;
    private final AtomicReference<com.sun.net.httpserver.Headers> seenHeaders = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody = "{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}";

    private ClaudeClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            seenHeaders.set(exchange.getRequestHeaders());
            seenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();

        client = new ClaudeClient();
        ReflectionTestUtils.setField(client, "apiKey", "sk-ant-test");
        ReflectionTestUtils.setField(client, "messagesApiUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/messages");
        ReflectionTestUtils.setField(client, "apiVersion", "2023-06-01");
        ReflectionTestUtils.setField(client, "model", "claude-test-model");
        ReflectionTestUtils.setField(client, "maxTokens", 777);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static String textPayload(String text, String prompt) {
        return """
                {"model":"claude-sonnet-5-5","max_tokens":1000,"messages":[{"role":"user","content":[
                  {"type":"text","text":"%s"},{"type":"text","text":"%s"}]}]}
                """.formatted(text, prompt);
    }

    private static String imagePayload(String url) {
        return """
                {"model":"claude-sonnet-5-5","messages":[{"role":"user","content":[
                  {"type":"text","text":"describe"},
                  {"type":"image_url","image_url":{"url":"%s"}}]}]}
                """.formatted(url);
    }

    private JsonObject sentRequest() {
        return JsonParser.parseString(seenBody.get()).getAsJsonObject();
    }

    @Test
    void sendsAuthHeadersAndUsesConfiguredModelAndTokens() {
        client.sendToAI(textPayload("code", "solve"));

        assertThat(seenHeaders.get().getFirst("x-api-key")).isEqualTo("sk-ant-test");
        assertThat(seenHeaders.get().getFirst("anthropic-version")).isEqualTo("2023-06-01");
        assertThat(seenHeaders.get().getFirst("Content-Type")).isEqualTo("application/json");
        JsonObject req = sentRequest();
        assertThat(req.get("model").getAsString()).isEqualTo("claude-test-model");
        assertThat(req.get("max_tokens").getAsInt()).isEqualTo(777);
    }

    @Test
    void forwardsTextPartsAsSingleUserMessage() {
        client.sendToAI(textPayload("some page text", "solve it"));

        JsonArray messages = sentRequest().getAsJsonArray("messages");
        assertThat(messages).hasSize(1);
        JsonObject user = messages.get(0).getAsJsonObject();
        assertThat(user.get("role").getAsString()).isEqualTo("user");
        JsonArray content = user.getAsJsonArray("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).getAsJsonObject().get("text").getAsString()).isEqualTo("some page text");
        assertThat(content.get(1).getAsJsonObject().get("text").getAsString()).isEqualTo("solve it");
    }

    @Test
    void convertsDataUrlImageToClaudeImageBlock() {
        client.sendToAI(imagePayload("data:image/jpeg;base64,QUJD"));

        JsonObject image = sentRequest().getAsJsonArray("messages").get(0).getAsJsonObject()
                .getAsJsonArray("content").get(1).getAsJsonObject();
        assertThat(image.get("type").getAsString()).isEqualTo("image");
        JsonObject source = image.getAsJsonObject("source");
        assertThat(source.get("type").getAsString()).isEqualTo("base64");
        assertThat(source.get("media_type").getAsString()).isEqualTo("image/jpeg");
        assertThat(source.get("data").getAsString()).isEqualTo("QUJD");
        assertThat(image.has("image_url")).isFalse();
    }

    @Test
    void rawBase64DefaultsToPng() {
        client.sendToAI(imagePayload("iVBORw0KGgo="));

        JsonObject source = firstImageSource();
        assertThat(source.get("media_type").getAsString()).isEqualTo("image/png");
        assertThat(source.get("data").getAsString()).isEqualTo("iVBORw0KGgo=");
    }

    @Test
    void rawBase64JpegIsDetectedBySignature() {
        client.sendToAI(imagePayload("/9j/4AAQSkZJRg=="));

        assertThat(firstImageSource().get("media_type").getAsString()).isEqualTo("image/jpeg");
    }

    private JsonObject firstImageSource() {
        return sentRequest().getAsJsonArray("messages").get(0).getAsJsonObject()
                .getAsJsonArray("content").get(1).getAsJsonObject().getAsJsonObject("source");
    }

    @Test
    void returnsAnswerAsJsonEncodedString() {
        responseBody = "{\"content\":[{\"type\":\"text\",\"text\":\"line1\\n\\\"quoted\\\"\"}]}";

        String result = client.sendToAI(textPayload("a", "b"));

        assertThat(JsonParser.parseString(result).getAsString()).isEqualTo("line1\n\"quoted\"");
    }

    @Test
    void joinsMultipleTextBlocksAndIgnoresOtherBlocks() {
        responseBody = "{\"content\":[{\"type\":\"text\",\"text\":\"Hello \"},"
                + "{\"type\":\"thinking\",\"thinking\":\"hidden\"},{\"type\":\"text\",\"text\":\"world\"}]}";

        String result = client.sendToAI(textPayload("a", "b"));

        assertThat(JsonParser.parseString(result).getAsString()).isEqualTo("Hello world");
    }

    @Test
    void throwsWithApiMessageOnErrorStatus() {
        status = 401;
        responseBody = "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}";

        assertThatThrownBy(() -> client.sendToAI(textPayload("a", "b")))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Claude API error 401: " + JsonParser.parseString(responseBody));
    }

    @Test
    void throwsWhenServerIsUnreachable() {
        server.stop(0);

        assertThatThrownBy(() -> client.sendToAI(textPayload("a", "b")))
                .isInstanceOf(RuntimeException.class);
    }
}
