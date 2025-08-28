package com.helper.server.process.extension;

import org.springframework.web.multipart.MultipartFile;

public interface IExtensionProcess {
    void executeText(String payload, String subPrompt);
    void executeCanvasTag(MultipartFile file, String subPrompt);
}
