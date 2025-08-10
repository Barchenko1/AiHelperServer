package com.helper.server.openaiclient.wisper;

import org.springframework.web.multipart.MultipartFile;

public interface IOpenAITranscribeClient {
    String transcribeWithOpenAI(MultipartFile audioFile);
}
