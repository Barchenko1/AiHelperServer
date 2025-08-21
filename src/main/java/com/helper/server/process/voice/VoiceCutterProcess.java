package com.helper.server.process.voice;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.openaiclient.wisper.IOpenAITranscribeClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Service
public class VoiceCutterProcess implements IVoiceCutterProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceCutterProcess.class);

    private static final String TEXT_REQ = "/templates/textRequest.json";

    @Value(value = "${voices.dir}")
    private String voicesDir;
    private final IOpenAITranscribeClient transcribeClient;
    private final IJsonTemplateService jsonTemplateService;
    private final IOpenAIClient openAIClient;
    private final WSHandler wsHandler;


    @Autowired
    public VoiceCutterProcess(IOpenAITranscribeClient transcribeClient,
                             @Qualifier("openAITextClient") IOpenAIClient openAIClient,
                             IJsonTemplateService jsonTemplateService,
                             WSHandler wsHandler) {
        this.transcribeClient = transcribeClient;
        this.openAIClient = openAIClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }

    private void sendToOpenAITranscript(MultipartFile multipartFile, String subPrompt) {
        String transcript = transcribeClient.transcribeWithOpenAI(multipartFile);
        sendToOpenAI(transcript, subPrompt);

    }

    private void sendToOpenAI(String transcript, String subPrompt) {
        if (transcript != null && !transcript.isEmpty()) {
            LOGGER.info("✅ Final Transcript: {}", transcript);
            String jsonPayload = jsonTemplateService.buildJsonPayload(TEXT_REQ, transcript, subPrompt);
            LOGGER.info(jsonPayload);
            String response = openAIClient.sendToOpenAI(jsonPayload);
            wsHandler.broadcast(response);
        } else {
            LOGGER.info("⚠️ No speech detected.");
        }
    }

    @Override
    public void execute(MultipartFile file, String subPrompt) {
        sendToOpenAITranscript(file, subPrompt);
    }

    private File convertToFile(MultipartFile multipartFile) {
        String originalFilename = Optional.ofNullable(multipartFile.getOriginalFilename())
                .orElse("upload");
        // create a temp file, preserving extension if present
        String prefix = originalFilename.contains(".")
                ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                : originalFilename;
        String suffix = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : null;

        File convFile = null;
        try {
            convFile = File.createTempFile(prefix + "-", suffix);
            multipartFile.transferTo(convFile); // writes contents
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return convFile;
    }

    private File saveToVoicesDir(MultipartFile multipartFile) {
        try {
            String workDir = voicesDir.isEmpty() ? "" : voicesDir;
            Path voicesDir = Paths.get(workDir);

            Files.createDirectories(voicesDir);

            // 2) Build a safe, unique filename (preserve extension if present)
            String original = Optional.ofNullable(multipartFile.getOriginalFilename()).orElse("audio");
            String cleanBase = original.replaceAll("[^a-zA-Z0-9._-]", "_");

            String ext;
            int dot = cleanBase.lastIndexOf('.');
            if (dot > 0 && dot < cleanBase.length() - 1) {
                ext = cleanBase.substring(dot);                 // includes the dot
                cleanBase = cleanBase.substring(0, dot);
            } else {
                // try from content type; fallback to .wav
                String ct = Optional.ofNullable(multipartFile.getContentType()).orElse("");
                ext = ct.equalsIgnoreCase("audio/wav") || ct.equalsIgnoreCase("audio/x-wav") ? ".wav" : ".bin";
            }

            String unique = cleanBase + "-" + System.currentTimeMillis() + ext;
            Path target = voicesDir.resolve(unique).normalize();

            // 3) Write file (replace if somehow exists)
            try (InputStream in = multipartFile.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return target.toFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save voice file", e);
        }
    }

}
