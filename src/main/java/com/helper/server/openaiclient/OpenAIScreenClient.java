package com.helper.server.openaiclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

@Service("openAIScreenClient")
public class OpenAIScreenClient extends AbstractOpenAiClient {
    private static final Gson GSON = new Gson();

    public OpenAIScreenClient() {}

    @Override
    public String sendToOpenAI(String json) {
        try {
            HttpURLConnection connection = getURLConnection();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes());
            }

            int code = connection.getResponseCode();
            InputStream responseStream = (code >= 200 && code < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            JsonObject jsonResponse = JsonParser.parseReader(new InputStreamReader(responseStream)).getAsJsonObject();

            String content = jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            jsonResponse.addProperty("timestamp", System.currentTimeMillis());

            return GSON.toJson(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
