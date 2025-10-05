package com.helper.server.template;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JsonTemplateService implements IJsonTemplateService {

    @Value(value = "${openAiModalVersion}")
    private String openAiModalVersion;
    @Value(value = "${openAiTokensSize}")
    private Integer openAiTokensSize;

    @Override
    public String buildJsonTextPayload(String payload, String prompt) {
        return buildTextJsonObject(payload, prompt).toString();
    }

    @Override
    public String buildJsonFilePayload(String base64File, String prompt) {
        return buildImageJsonObject(List.of(base64File), prompt).toString();
    }

    @Override
    public String buildJsonFilePayload(List<String> base64Files, String prompt) {
        return buildImageJsonObject(base64Files, prompt).toString();
    }

    private JsonObject buildTextJsonObject(String payload, String prompt) {
        JsonArray content = new JsonArray();

        JsonObject transcriptObject = new JsonObject();
        transcriptObject.addProperty("type", "text");
        transcriptObject.addProperty("text", payload == null ? "" : payload);
        content.add(transcriptObject);

        JsonObject text = new JsonObject();
        text.addProperty("type", "text");
        text.addProperty("text", prompt == null ? "" : prompt);
        content.add(text);

        return createJsonObjectRoot(content);
    }

    private JsonObject buildImageJsonObject(List<String> base64Files, String prompt) {
        JsonArray content = new JsonArray();

        JsonObject text = new JsonObject();
        text.addProperty("type", "text");
        text.addProperty("text", prompt == null ? "" : prompt);
        content.add(text);

        for (String url : base64Files) {
            if (url == null || url.isBlank()) continue;
            JsonObject img = new JsonObject();
            img.addProperty("type", "image_url");
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", url);
            img.add("image_url", imageUrl);
            content.add(img);
        }

        return createJsonObjectRoot(content);
    }

    private JsonObject createJsonObjectRoot(JsonElement content){
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.add("content", content);

        JsonArray messages = new JsonArray();
        messages.add(user);

        JsonObject root = new JsonObject();
        root.addProperty("model", openAiModalVersion);
        root.add("messages", messages);
        root.addProperty("max_tokens", openAiTokensSize);

        return root;
    }

}
