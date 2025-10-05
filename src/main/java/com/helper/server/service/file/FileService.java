package com.helper.server.service.file;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.service.AbstractService;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class FileService extends AbstractService implements IFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    @Autowired
    public FileService(@Qualifier("openAIScreenClient") IOpenAIClient openAIClient,
                       IJsonTemplateService jsonTemplateService,
                       WSHandler wsHandler) {
        super(openAIClient, jsonTemplateService, wsHandler);
    }

    @Override
    public void sendFile(Principal principal, MultipartFile file, String prompt) {
        try {
            byte[] fileBytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            executeAiCall(principal, List.of(base64), prompt);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void sendFiles(Principal principal, List<MultipartFile> files, String prompt) {
        try {
            List<String> imagesB64 = files.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(this::toBase64DataUrl)
                    .toList();

            if (imagesB64.isEmpty()) {
                LOGGER.warn("sendFiles: no non-empty files received");
                return;
            }
            executeAiCall(principal, imagesB64, prompt);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String toBase64DataUrl(MultipartFile f) {
        try {
            String ct = Optional.ofNullable(f.getContentType()).orElse("application/octet-stream");
            String base64 = Base64.getEncoder().encodeToString(f.getBytes());
            return ct.startsWith("image/") ? "data:" + ct + ";base64," + base64 : base64;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void executeAiCall(Principal principal, List<String> imagesB64, String prompt) {
        long handle1 = System.currentTimeMillis();
        String jsonPayload = jsonTemplateService.buildJsonFilePayload(imagesB64, prompt);
        long timeDiff2 = System.currentTimeMillis() - handle1;
        LOGGER.info("time gets {}", timeDiff2);

        long handle3 = System.currentTimeMillis();
        String response = openAIClient.sendToOpenAI(jsonPayload);
        long timeDiff3 = System.currentTimeMillis() - handle3;
        LOGGER.info("time gets {}", timeDiff3);
        long handle4 = System.currentTimeMillis();
        wsHandler.broadcastToUser(principal.getName(), response);
        long timeDiff4 = System.currentTimeMillis() - handle4;
        LOGGER.info("time gets {}", timeDiff4);
    }

}
