package com.helper.server.process.extension;

import com.helper.server.service.text.ITextService;
import com.helper.server.service.transform.IFileTransformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExtensionProcess implements IExtensionProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionProcess.class);

    private final ITextService textService;
    private final IFileTransformService fileTransform;

    @Autowired
    public ExtensionProcess(ITextService textService, IFileTransformService fileTransform) {
        this.textService = textService;
        this.fileTransform = fileTransform;
    }

    @Override
    public void executeText(String payload, String subPrompt) {
        textService.sendText(payload, subPrompt);
    }

    @Override
    public void executeCanvasTag(MultipartFile file, String subPrompt) {
        String payload = fileTransform.getFilePayload(file);
        textService.sendText(payload, subPrompt);
    }
}
