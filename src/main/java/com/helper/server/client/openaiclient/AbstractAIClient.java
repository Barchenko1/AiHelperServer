package com.helper.server.client.openaiclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class AbstractAIClient implements IOpenAIClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAIClient.class);
    protected static final Gson GSON = new Gson();

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    /** Name used in error messages, e.g. "OpenAI" or "Claude". */
    protected abstract String providerName();

    protected abstract String apiUrl();

    /** Auth and version headers; Content-Type is added by the base class. */
    protected abstract Map<String, String> requestHeaders();

    /** Converts the OpenAI-style payload into this provider's request body. */
    protected abstract String buildRequestBody(String openAiJson);

    /** Pulls the answer text out of a successful response. */
    protected abstract String extractText(JsonObject response) throws IOException;

    @Override
    public String sendToOpenAI(String json) {
        try {
            HttpURLConnection connection = openConnection();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(buildRequestBody(json).getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            boolean ok = code >= 200 && code < 300;
            String body = readBody(ok ? connection.getInputStream() : connection.getErrorStream());
            if (!ok) {
                throw apiError(code, body);
            }
            return GSON.toJson(extractText(JsonParser.parseString(body).getAsJsonObject()));
        } catch (IOException e) {
            LOGGER.error("❌ Error sending to {}: {}", providerName(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected IOException apiError(int code, String body) {
        return new IOException(providerName() + " API error " + code + ": " + body);
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl()).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Content-Type", "application/json");
        requestHeaders().forEach(connection::setRequestProperty);
        return connection;
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
