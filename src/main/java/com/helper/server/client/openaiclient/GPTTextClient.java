package com.helper.server.client.openaiclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

@Service("gptTextClient")
public class GPTTextClient extends AbstractAIClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GPTTextClient.class);

    @Override
    public String sendToOpenAI(String json) {
        try {
            HttpURLConnection connection = getURLConnection();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                jsonResponse.addProperty("timestamp", System.currentTimeMillis());

                String content = jsonResponse
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();

                return GSON.toJson(content);
            }
        } catch (Exception e) {
            LOGGER.error("❌ Error sending to AI: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
