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

@Service
public class VoiceHotkeyDaemon implements IVoiceCutterProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceHotkeyDaemon.class);

    private static final String TEXT_REQ = "/templates/textRequest.json";

    private final IOpenAITranscribeClient transcribeClient;
    private final IJsonTemplateService jsonTemplateService;
    private final IOpenAIClient openAIClient;
    private final WSHandler wsHandler;


    @Autowired
    public VoiceHotkeyDaemon(IOpenAITranscribeClient transcribeClient,
                             @Qualifier("openAITextClient") IOpenAIClient openAIClient,
                             IJsonTemplateService jsonTemplateService,
                             WSHandler wsHandler) {
        this.transcribeClient = transcribeClient;
        this.openAIClient = openAIClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }

    private void sendToOpenAITranscript(MultipartFile file, String subPrompt) {
        String transcript = transcribeClient.transcribeWithOpenAI(file);
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
    public void execute(String payload, String subPrompt) {
        sendToOpenAI(payload, subPrompt);
    }
}
