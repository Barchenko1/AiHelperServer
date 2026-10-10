package com.helper.server.client.openaiclient;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/** Sends text and screenshot (image_url) chat payloads to the OpenAI completions API. */
@Service("gptClient")
public class GPTClient extends AbstractAIClient {

    @Value("${GPT_API_KEY}")
    private String apiKey;

    @Value("${openai.api.url.completions}")
    private String completionsApiUrl;

    @Override
    protected String providerName() {
        return "OpenAI";
    }

    @Override
    protected String apiUrl() {
        return completionsApiUrl;
    }

    @Override
    protected Map<String, String> requestHeaders() {
        return Map.of("Authorization", "Bearer " + apiKey);
    }

    @Override
    protected String buildRequestBody(String openAiJson) {
        return openAiJson;
    }

    @Override
    protected String extractText(JsonObject response) throws IOException {
        if (!response.has("choices")) {
            throw apiError(200, response.toString());
        }
        return response.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
