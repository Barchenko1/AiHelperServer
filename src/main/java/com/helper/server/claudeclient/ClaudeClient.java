package com.helper.server.claudeclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.helper.server.openaiclient.IOpenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sends the same OpenAI-style chat payload built by JsonTemplateService to the Claude Messages API,
 * so text, screenshot and voice flows work unchanged. Returns the answer the same way the OpenAI clients do.
 */
@Service("claudeClient")
public class ClaudeClient implements IOpenAIClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeClient.class);
    private static final Gson GSON = new Gson();
    private static final String DATA_PREFIX = "data:";

    @Value("${ANTHROPIC_API_KEY:}")
    private String apiKey;

    @Value("${claude.api.url.messages}")
    private String messagesApiUrl;

    @Value("${claude.api.version}")
    private String apiVersion;

    @Value("${claudeModelVersion}")
    private String model;

    @Value("${openAiTokensSize}")
    private Integer maxTokens;

    @Override
    public String sendToOpenAI(String json) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(messagesApiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", apiVersion);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(toClaudeRequest(json).getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
            JsonObject response = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (code < 200 || code >= 300) {
                throw new IOException("Claude API error " + code + ": " + response);
            }

            StringBuilder text = new StringBuilder();
            for (JsonElement block : response.getAsJsonArray("content")) {
                JsonObject b = block.getAsJsonObject();
                if ("text".equals(b.get("type").getAsString())) {
                    text.append(b.get("text").getAsString());
                }
            }
            return GSON.toJson(text.toString());
        } catch (IOException e) {
            LOGGER.error("❌ Error sending to Claude: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String toClaudeRequest(String openAiJson) {
        JsonObject in = JsonParser.parseString(openAiJson).getAsJsonObject();

        JsonArray content = new JsonArray();
        for (JsonElement msg : in.getAsJsonArray("messages")) {
            for (JsonElement part : msg.getAsJsonObject().getAsJsonArray("content")) {
                JsonObject p = part.getAsJsonObject();
                if ("image_url".equals(p.get("type").getAsString())) {
                    content.add(toImageBlock(p.getAsJsonObject("image_url").get("url").getAsString()));
                } else {
                    content.add(p);
                }
            }
        }

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.add("content", content);
        JsonArray messages = new JsonArray();
        messages.add(user);

        JsonObject out = new JsonObject();
        out.addProperty("model", model);
        out.addProperty("max_tokens", maxTokens);
        out.add("messages", messages);
        return out.toString();
    }

    /** Accepts "data:<mime>;base64,<data>" or raw base64 (FileService single-file path sends raw base64). */
    private JsonObject toImageBlock(String url) {
        String mediaType = "image/png";
        String data = url;
        if (url.startsWith(DATA_PREFIX)) {
            int semi = url.indexOf(';');
            int comma = url.indexOf(',');
            mediaType = url.substring(DATA_PREFIX.length(), semi);
            data = url.substring(comma + 1);
        } else if (url.startsWith("/9j/")) {
            mediaType = "image/jpeg";
        }

        JsonObject source = new JsonObject();
        source.addProperty("type", "base64");
        source.addProperty("media_type", mediaType);
        source.addProperty("data", data);
        JsonObject block = new JsonObject();
        block.addProperty("type", "image");
        block.add("source", source);
        return block;
    }
}
