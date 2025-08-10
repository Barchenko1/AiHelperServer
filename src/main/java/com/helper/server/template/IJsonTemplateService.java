package com.helper.server.template;

public interface IJsonTemplateService {
    String buildJsonPayload(String templateFile, String prompt, String subPrompt);
}
