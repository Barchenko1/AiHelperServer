package com.helper.server.client.gpt.wisper;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GPTTranscribeClientTest {

    private HttpServer server;
    private volatile int status = 200;
    private volatile String responseBody = "{\"text\":\"hello world\"}";
    private GPTTranscribeClient client;
    private final MockMultipartFile audio = new MockMultipartFile("file", "a.wav", "audio/wav", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/audio/transcriptions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        client = new GPTTranscribeClient();
        ReflectionTestUtils.setField(client, "apiKey", "sk-test");
        ReflectionTestUtils.setField(client, "transcriptionApiUrl",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/audio/transcriptions");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void returnsTranscript() {
        assertThat(client.transcribeWithGPT(audio)).isEqualTo("hello world");
    }

    @Test
    void errorStatusThrowsSoTheUiCanShowIt() {
        status = 429;
        responseBody = "{\"error\":{\"message\":\"You have no credits remaining.\"}}";

        assertThatThrownBy(() -> client.transcribeWithGPT(audio))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("GPT transcribe error 429: " + responseBody);
    }
}
