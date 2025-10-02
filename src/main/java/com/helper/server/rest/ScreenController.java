package com.helper.server.rest;

import com.helper.server.process.screenshot.IScreenshotProcess;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScreenController {

    private final IScreenshotProcess screenshotProcess;

    public ScreenController(IScreenshotProcess screenshotProcess) {
        this.screenshotProcess = screenshotProcess;
    }

    @PostMapping(value = "/screen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> handle(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "prompt", required = false) String prompt) {
        screenshotProcess.execute(file, prompt);
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/screens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> screen(
            @RequestPart(name = "files", required = false) List<MultipartFile> files,
            @RequestPart(name = "prompt", required = false) String prompt) {

        screenshotProcess.execute(files, prompt);
        return ResponseEntity.ok().build();
    }
}
