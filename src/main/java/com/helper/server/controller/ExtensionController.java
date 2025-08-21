package com.helper.server.controller;

import com.helper.server.process.extension.IExtensionProcess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ExtensionController {

    private final IExtensionProcess extensionExecutor;

    @Value(value = "${prompt.1}")
    private String prompt;

    public ExtensionController(IExtensionProcess extensionExecutor) {
        this.extensionExecutor = extensionExecutor;
    }

    @PostMapping(value = "/text")
    public ResponseEntity<Void> handle(
            @RequestBody String body,
            @RequestPart(value = "subPrompt", required = false) String subPrompt) {
        extensionExecutor.execute(body, prompt);
        return ResponseEntity.ok().build();
    }
}
