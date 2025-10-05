package com.helper.server.process.screenshot;

import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

public interface IScreenshotProcess {
    void execute(MultipartFile file, String prompt, Principal principal);
    void execute(List<MultipartFile> files, String prompt, Principal principal);
}
