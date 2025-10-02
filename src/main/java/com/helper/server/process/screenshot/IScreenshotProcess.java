package com.helper.server.process.screenshot;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IScreenshotProcess {
    void execute(MultipartFile file, String prompt);
    void execute(List<MultipartFile> files, String prompt);
}
