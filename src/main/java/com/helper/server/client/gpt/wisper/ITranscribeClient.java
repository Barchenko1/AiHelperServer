package com.helper.server.client.gpt.wisper;

import org.springframework.web.multipart.MultipartFile;

public interface ITranscribeClient {
    String transcribeWithGPT(MultipartFile audioFile);
}
