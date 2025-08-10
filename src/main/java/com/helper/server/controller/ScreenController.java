package com.helper.server.controller;

import com.helper.server.process.screenshot.IScreenshotProcess;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ScreenController {

    private final IScreenshotProcess screenshotProcess;

    public ScreenController(IScreenshotProcess screenshotProcess) {
        this.screenshotProcess = screenshotProcess;
    }

    @PostMapping(value = "/screen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> handle(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "subPrompt", required = false) String subPrompt) {
        screenshotProcess.execute(file, subPrompt);
        return ResponseEntity.ok().build();
    }
}
