package com.helper.server.process.extension;

import com.helper.server.entity.ExtensionPayload;
import com.helper.server.process.AbstractProcess;
import com.helper.server.service.text.ITextService;
import com.helper.server.service.transform.IFileTransformService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Service
public class ExtensionProcess extends AbstractProcess implements IExtensionProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionProcess.class);

    private final ITextService textService;
    private final IFileTransformService fileTransform;

    @Autowired
    public ExtensionProcess(WSHandler wsHandler,
                            ITextService textService,
                            IFileTransformService fileTransform) {
        super(wsHandler);
        this.textService = textService;
        this.fileTransform = fileTransform;
    }

    @Override
    public void executeText(Principal principal, ExtensionPayload payload) {
        wsHandler.broadcastToUser(principal.getName(), "Processing...");
        textService.sendText(principal, payload);
    }

    @Override
    public void executeCanvasTag(Principal principal,ExtensionPayload payload, MultipartFile file) {
        wsHandler.broadcastToUser(principal.getName(), "Processing...");
        String filePayload = fileTransform.getFilePayload(file);
        payload.setText(filePayload);
        textService.sendText(principal, payload);
    }
}
