package com.helper.server.rest;

import com.helper.server.entity.ExtensionPayload;
import com.helper.server.process.extension.IExtensionProcess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ExtensionController {

    private final IExtensionProcess extensionExecutor;

    public ExtensionController(IExtensionProcess extensionExecutor) {
        this.extensionExecutor = extensionExecutor;
    }

    @PostMapping(value = "/text", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleText(@RequestBody final ExtensionPayload payload) {
        extensionExecutor.executeText(payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/canvas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> handleCanvasTag(
            @RequestPart("file") final MultipartFile file,
            @RequestPart("payload") final ExtensionPayload payload) {
        extensionExecutor.executeCanvasTag(payload, file);
        return ResponseEntity.ok().build();
    }
}
