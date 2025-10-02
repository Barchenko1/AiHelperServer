package com.helper.server.process.extension;

import com.helper.server.entity.ExtensionPayload;
import org.springframework.web.multipart.MultipartFile;

public interface IExtensionProcess {
    void executeText(ExtensionPayload payload);
    void executeCanvasTag(ExtensionPayload payload, MultipartFile file);
}
