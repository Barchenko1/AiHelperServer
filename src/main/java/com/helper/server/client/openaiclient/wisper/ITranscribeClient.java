package com.helper.server.client.openaiclient.wisper;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface ITranscribeClient {
    String transcribeWithOpenAI(MultipartFile audioFile);
    String transcribeWithOpenAI(File audioFile);
}
