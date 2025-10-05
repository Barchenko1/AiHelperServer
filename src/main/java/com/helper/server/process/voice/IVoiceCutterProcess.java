package com.helper.server.process.voice;

import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

public interface IVoiceCutterProcess {
    void execute(Principal principal, MultipartFile file, String prompt);
}
