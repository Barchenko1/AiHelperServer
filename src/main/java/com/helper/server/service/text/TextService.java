package com.helper.server.service.text;

import com.helper.server.entity.ExtensionPayload;
import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.service.AbstractService;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.security.Principal;

import static com.helper.server.unil.Constant.LANGUAGE_TEXT;

@Service
public class TextService extends AbstractService implements ITextService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextService.class);

    @Autowired
    public TextService(@Qualifier("openAITextClient") IOpenAIClient openAIClient,
                            IJsonTemplateService jsonTemplateService,
                            WSHandler wsHandler) {
        super(openAIClient, jsonTemplateService, wsHandler);
    }

    @Override
    public void sendText(Principal principal, ExtensionPayload payload) {
        String subPayload = payload.getText().length() > 50 ? payload.getText().substring(0, 50): payload.getText();
        LOGGER.info("Received from extension: {}", subPayload);
        long handle3 = System.currentTimeMillis();
        String sanitizedBody = payload.getText()
                .replaceAll("\\s+", " ")
                .replace("\"", "\\\"");
        String completePrompt = payload.getPrompt() + " " + LANGUAGE_TEXT.formatted(payload.getLanguage());
        String jsonPayload = jsonTemplateService.buildJsonTextPayload(sanitizedBody, completePrompt);
        String response = openAIClient.sendToOpenAI(jsonPayload);
        long timeDiff3 = System.currentTimeMillis() - handle3;
        LOGGER.info("time gets response {}", timeDiff3);
        long handle4 = System.currentTimeMillis();
        wsHandler.broadcastToUser(principal.getName(), response);
        long timeDiff4 = System.currentTimeMillis() - handle4;
        LOGGER.info("time gets socket {}", timeDiff4);
    }
}
