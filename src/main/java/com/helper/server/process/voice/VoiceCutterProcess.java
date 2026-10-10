package com.helper.server.process.voice;

import com.helper.server.client.IAIClient;
import com.helper.server.client.gpt.wisper.ITranscribeClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.util.UiErrors;
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

    private final ITranscribeClient transcribeClient;
    private final IJsonTemplateService jsonTemplateService;
    private final IAIClient gptClient;
    private final WSHandler wsHandler;

    @Autowired
    public VoiceCutterProcess(ITranscribeClient transcribeClient,
                              @Qualifier("aiTextClient") IAIClient gptClient,
                              IJsonTemplateService jsonTemplateService,
                              WSHandler wsHandler) {
        this.transcribeClient = transcribeClient;
        this.gptClient = gptClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }

    @Override
    public void execute(Principal principal, MultipartFile file, String prompt) {
        if (file == null) {
            wsHandler.broadcastToUser(principal.getName(), "No voice file provided");
        } else {
            wsHandler.broadcastToUser(principal.getName(),"Processing...");
            sendToGPTTranscript(principal, file, prompt);
        }
    }

    private void sendToGPTTranscript(Principal principal, MultipartFile multipartFile, String prompt) {
        String transcript = transcribeClient.transcribeWithGPT(multipartFile);
        sendToGPT(principal, transcript, prompt);

    }

    private void sendToGPT(Principal principal, String transcript, String prompt) {
        if (transcript != null && !transcript.isEmpty()) {
            LOGGER.info("✅ Final Transcript: {}", transcript);
            String jsonPayload = jsonTemplateService.buildJsonTextPayload(transcript, prompt);
            LOGGER.info(jsonPayload);
            String response = gptClient.sendToAI(jsonPayload);
            wsHandler.broadcastToUser(principal.getName(), response);
        } else {
            LOGGER.info("⚠️ No speech detected.");
            wsHandler.broadcastToUser(principal.getName(), UiErrors.toUiMessage("No speech detected in the recording."));
        }
    }

}
