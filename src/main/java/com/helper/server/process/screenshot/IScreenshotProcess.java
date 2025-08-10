package com.helper.server.process.screenshot;

import org.springframework.web.multipart.MultipartFile;

public interface IScreenshotProcess {
    void execute(MultipartFile file, String subPrompt);
}
