package com.helper.server.template;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class JsonTemplateService implements IJsonTemplateService {

    @Override
    public String buildJsonPayload(String templateFile, String prompt, String subPrompt) {
        try (InputStream inputStream = this.getClass().getResourceAsStream(templateFile)) {
            if (inputStream == null) {
                throw new RuntimeException("JSON template not found: " + templateFile);
            }

            String template = new String(inputStream.readAllBytes());
            String subTemplate = subPrompt != null ? subPrompt : "";
            return template.formatted(prompt, subTemplate);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON template", e);
        }
    }
}
