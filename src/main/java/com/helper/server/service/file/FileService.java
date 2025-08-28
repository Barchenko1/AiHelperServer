package com.helper.server.service.file;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

import static com.helper.server.unil.Constant.PICTURE_REQ;

@Service
public class FileService implements IFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    private final IOpenAIClient openAIClient;
    private final IJsonTemplateService jsonTemplateService;
    private final WSHandler wsHandler;

    @Autowired
    public FileService(@Qualifier("openAIScreenClient") IOpenAIClient openAIClient,
                             IJsonTemplateService jsonTemplateService, WSHandler wsHandler) {
        this.openAIClient = openAIClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }

    @Override
    public void sendFile(MultipartFile file, String subPrompt) {
        try {
            long handle1 = System.currentTimeMillis();
            byte[] fileBytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            String jsonPayload = jsonTemplateService.buildJsonPayload(PICTURE_REQ, base64, subPrompt);
            long timeDiff2 = System.currentTimeMillis() - handle1;
            LOGGER.info("time gets {}", timeDiff2);

            long handle3 = System.currentTimeMillis();
            String response = openAIClient.sendToOpenAI(jsonPayload);
            long timeDiff3 = System.currentTimeMillis() - handle3;
            LOGGER.info("time gets {}", timeDiff3);
            long handle4 = System.currentTimeMillis();
            wsHandler.broadcast(response);
            long timeDiff4 = System.currentTimeMillis() - handle4;
            LOGGER.info("time gets {}", timeDiff4);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
