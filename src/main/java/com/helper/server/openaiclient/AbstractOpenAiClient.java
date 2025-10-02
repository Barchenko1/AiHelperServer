package com.helper.server.openaiclient;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AbstractOpenAiClient implements IOpenAIClient {
    @Value(value = "${OPEN_AI_API_KEY}")
    protected String openAiApiKey;

    @Value(value = "${openai.api.url.completions}")
    protected String completionsApiUrl;

    protected static final Gson GSON = new Gson();

    protected HttpURLConnection getURLConnection() {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL(completionsApiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + openAiApiKey);
            connection.setRequestProperty("Content-Type", "application/json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }
}
