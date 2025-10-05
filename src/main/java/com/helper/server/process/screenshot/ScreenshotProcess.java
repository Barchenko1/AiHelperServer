package com.helper.server.process.screenshot;

import com.helper.server.process.AbstractProcess;
import com.helper.server.service.file.IFileService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Service
public class ScreenshotProcess extends AbstractProcess implements IScreenshotProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotProcess.class);

    private final IFileService fileService;

    @Autowired
    public ScreenshotProcess(WSHandler wsHandler,
                             IFileService fileService) {
        super(wsHandler);
        this.fileService = fileService;
    }

    @Override
    public void execute(MultipartFile file, String prompt, Principal principal) {
        if (file == null) {
            wsHandler.broadcastToUser(principal.getName(), "Commit folder is empty");
        } else {
            wsHandler.broadcastToUser(principal.getName(), "Processing...");
            fileService.sendFile(principal, file, prompt);
        }
    }

    @Override
    public void execute(List<MultipartFile> files, String prompt, Principal principal) {
        if (files == null || files.isEmpty()) {
            wsHandler.broadcastToUser(principal.getName(), "Commit folder is empty");
        } else {
            wsHandler.broadcastToUser(principal.getName(), "Processing...");
            fileService.sendFiles(principal, files, prompt);
        }
    }

}
