package com.helper.server.process.screenshot;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.service.file.IFileService;
import com.helper.server.service.text.ITextService;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScreenshotProcess implements IScreenshotProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotProcess.class);

    private final ITextService textService;
    private final IFileService fileService;

    @Autowired
    public ScreenshotProcess(ITextService textService, IFileService fileService) {
        this.textService = textService;
        this.fileService = fileService;
    }

    @Override
    public void execute(MultipartFile file, String subPrompt) {
        fileService.sendFile(file, subPrompt);
    }

}
