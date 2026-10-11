package com.helper.server.template;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class JsonTemplateServiceTest {

    private final JsonTemplateService service = new JsonTemplateService();

    {
        ReflectionTestUtils.setField(service, "modalVersion", "gpt-test");
        ReflectionTestUtils.setField(service, "tokensSize", 123);
    }

    private static JsonArray content(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertThat(root.get("model").getAsString()).isEqualTo("gpt-test");
        assertThat(root.get("max_tokens").getAsInt()).isEqualTo(123);
        JsonObject msg = root.getAsJsonArray("messages").get(0).getAsJsonObject();
        assertThat(msg.get("role").getAsString()).isEqualTo("user");
        return msg.getAsJsonArray("content");
    }

    @Test
    void textPayloadHasTextThenPrompt() {
        JsonArray c = content(service.buildJsonTextPayload("body", "prompt"));

        assertThat(c).hasSize(2);
        assertThat(c.get(0).getAsJsonObject().get("text").getAsString()).isEqualTo("body");
        assertThat(c.get(1).getAsJsonObject().get("text").getAsString()).isEqualTo("prompt");
    }

    @Test
    void nullTextAndPromptBecomeEmpty() {
        JsonArray c = content(service.buildJsonTextPayload(null, null));

        assertThat(c.get(0).getAsJsonObject().get("text").getAsString()).isEmpty();
        assertThat(c.get(1).getAsJsonObject().get("text").getAsString()).isEmpty();
    }

    @Test
    void singleImagePayload() {
        JsonArray c = content(service.buildJsonFilePayload("AAAA", "what"));

        assertThat(c).hasSize(2);
        assertThat(c.get(0).getAsJsonObject().get("text").getAsString()).isEqualTo("what");
        JsonObject img = c.get(1).getAsJsonObject();
        assertThat(img.get("type").getAsString()).isEqualTo("image_url");
        assertThat(img.getAsJsonObject("image_url").get("url").getAsString()).isEqualTo("AAAA");
    }

    @Test
    void blankImagesAreSkippedAndNullPromptIsEmpty() {
        JsonArray c = content(service.buildJsonFilePayload(Arrays.asList("A", null, " ", "B"), null));

        assertThat(c).hasSize(3);
        assertThat(c.get(0).getAsJsonObject().get("text").getAsString()).isEmpty();
    }
}
