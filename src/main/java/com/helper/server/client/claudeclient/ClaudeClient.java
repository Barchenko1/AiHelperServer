package com.helper.server.client.claudeclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.helper.server.client.AbstractAIClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("claudeClient")
public class ClaudeClient extends AbstractAIClient {

    private static final String DATA_PREFIX = "data:";

    @Value("${ANTHROPIC_API_KEY:}")
    private String apiKey;

    @Value("${claude.api.url.messages}")
    private String messagesApiUrl;

    @Value("${claude.api.version}")
    private String apiVersion;

    @Value("${claudeModelVersion}")
    private String model;

    @Value("${tokensSize}")
    private Integer maxTokens;

    @Override
    protected String providerName() {
        return "Claude";
    }

    @Override
    protected String apiUrl() {
        return messagesApiUrl;
    }

    @Override
    protected Map<String, String> requestHeaders() {
        return Map.of("x-api-key", apiKey, "anthropic-version", apiVersion);
    }

    @Override
    protected String buildRequestBody(String json) {
        return toClaudeRequest(json);
    }

    @Override
    protected String extractText(JsonObject response) {
        StringBuilder text = new StringBuilder();
        for (JsonElement block : response.getAsJsonArray("content")) {
            JsonObject b = block.getAsJsonObject();
            if ("text".equals(b.get("type").getAsString())) {
                text.append(b.get("text").getAsString());
            }
        }
        return text.toString();
    }

    private String toClaudeRequest(String json) {
        JsonObject in = JsonParser.parseString(json).getAsJsonObject();

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
