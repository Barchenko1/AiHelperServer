package com.helper.server.process.extension;

import com.helper.server.entity.ExtensionPayload;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

public interface IExtensionProcess {
    void executeText(Principal principal, ExtensionPayload payload);
    void executeCanvasTag(Principal principal, ExtensionPayload payload, MultipartFile file);
}
