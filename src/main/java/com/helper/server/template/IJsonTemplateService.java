package com.helper.server.template;

import java.util.List;

public interface IJsonTemplateService {
    String buildJsonTextPayload(String transcript, String prompt);
    String buildJsonFilePayload(String base64File, String prompt);
    String buildJsonFilePayload(List<String> base64Files, String prompt);
}
