package com.helper.server.openaiclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

@Service("openAITextClient")
public class OpenAITextClient extends AbstractOpenAiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAITextClient.class);

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
                JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                return jsonObject.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Error sending to GPT: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
