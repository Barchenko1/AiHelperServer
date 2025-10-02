package com.helper.server.process.voice;

import org.springframework.web.multipart.MultipartFile;

public interface IVoiceCutterProcess {
    void execute(MultipartFile file, String prompt);
}
