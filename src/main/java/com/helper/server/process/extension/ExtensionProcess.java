package com.helper.server.process.extension;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ExtensionProcess implements IExtensionProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionProcess.class);

    private static final String TEXT_REQ = "/templates/textRequest.json";

    private final IOpenAIClient openAIClient;
    private final IJsonTemplateService jsonTemplateService;
    private final WSHandler wsHandler;

    @Autowired
    public ExtensionProcess(@Qualifier("openAITextClient") IOpenAIClient openAIClient,
                            IJsonTemplateService jsonTemplateService,
                            WSHandler wsHandler) {
        this.openAIClient = openAIClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }

    private void callExtension(String payload, String subPrompt) {
        String subPayload = payload.substring(0, 50) + "...";

        LOGGER.info("Received from extension: {}", subPayload);
        long handle3 = System.currentTimeMillis();
        String sanitizedBody = payload
                .replaceAll("\\s+", " ")
                .replace("\"", "\\\"");
        String jsonPayload = jsonTemplateService.buildJsonPayload(TEXT_REQ, sanitizedBody, subPrompt);
        String response = openAIClient.sendToOpenAI(jsonPayload);
        long timeDiff3 = System.currentTimeMillis() - handle3;
        LOGGER.info("time gets response {}", timeDiff3);
        long handle4 = System.currentTimeMillis();
        wsHandler.broadcast(response);
        long timeDiff4 = System.currentTimeMillis() - handle4;
        LOGGER.info("time gets socket {}", timeDiff4);
    }

    @Override
    public void execute(String payload, String subPrompt) {
        callExtension(payload, subPrompt);
    }
}
