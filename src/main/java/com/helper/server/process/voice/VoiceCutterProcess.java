package com.helper.server.process.voice;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.openaiclient.wisper.IOpenAITranscribeClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@Service
public class VoiceCutterProcess implements IVoiceCutterProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceCutterProcess.class);

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

    @Override
    public void execute(Principal principal, MultipartFile file, String prompt) {
        if (file == null) {
            wsHandler.broadcastToUser(principal.getName(), "No voice file provided");
        } else {
            wsHandler.broadcastToUser(principal.getName(),"Processing...");
            sendToOpenAITranscript(principal, file, prompt);
        }
    }

    private void sendToOpenAITranscript(Principal principal, MultipartFile multipartFile, String prompt) {
        String transcript = transcribeClient.transcribeWithOpenAI(multipartFile);
        sendToOpenAI(principal, transcript, prompt);

    }

    private void sendToOpenAI(Principal principal, String transcript, String prompt) {
        if (transcript != null && !transcript.isEmpty()) {
            LOGGER.info("✅ Final Transcript: {}", transcript);
            String jsonPayload = jsonTemplateService.buildJsonTextPayload(transcript, prompt);
            LOGGER.info(jsonPayload);
            String response = openAIClient.sendToOpenAI(jsonPayload);
            wsHandler.broadcastToUser(principal.getName(), response);
        } else {
            LOGGER.info("⚠️ No speech detected.");
        }
    }

}
