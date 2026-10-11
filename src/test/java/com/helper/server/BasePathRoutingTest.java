package com.helper.server;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** With APP_BASE_PATH set, every route (REST and websocket) lives under that prefix - what a reverse proxy forwards to. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "APP_BASE_PATH=/aihelper",
        "APP_JWT_SECRET=integration-test-secret-long-enough-for-hs256",
        "APP_NOOP_SECRET=1",
        "GPT_API_KEY=test",
        "ANTHROPIC_API_KEY=test"
})
class BasePathRoutingTest {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String loginToken() throws Exception {
        HttpRequest login = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/aihelper/api/v1/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=pa&password=pa1"))
                .build();
        HttpResponse<String> res = http.send(login, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        return JsonParser.parseString(res.body()).getAsJsonObject().get("token").getAsString();
    }

    @Test
    void restRoutesAreServedUnderTheBasePath() throws Exception {
        HttpResponse<String> status = get("/aihelper/api/v1/status");

        assertThat(status.statusCode()).isEqualTo(200);
        assertThat(status.body()).contains("\"session\":false");
    }

    @Test
    void loginUnderTheBasePathGivesAWorkingToken() throws Exception {
        String token = loginToken();

        HttpResponse<String> status = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/aihelper/api/v1/status"))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(status.body()).contains("\"session\":true");
    }

    @Test
    void rootPathNoLongerServesTheApi() throws Exception {
        assertThat(get("/api/v1/status").statusCode()).isEqualTo(404);
    }

    @Test
    void websocketIsServedUnderTheBasePath() throws Exception {
        String token = loginToken();

        WebSocketSession ws = new StandardWebSocketClient()
                .execute(new TextWebSocketHandler(), "ws://localhost:" + port + "/aihelper/ws?token=" + token)
                .get(5, TimeUnit.SECONDS);

        assertThat(ws.isOpen()).isTrue();
        ws.sendMessage(new TextMessage("hi"));
        ws.close();
    }
}
