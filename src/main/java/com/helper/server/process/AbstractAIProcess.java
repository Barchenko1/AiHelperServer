package com.helper.server.process;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAIProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAIProcess.class);

    private final IOpenAIClient openAIClient;
    private final IJsonTemplateService jsonTemplateService;
    private final WSHandler wsHandler;

    public AbstractAIProcess(IOpenAIClient openAIClient,
                            IJsonTemplateService jsonTemplateService,
                            WSHandler wsHandler) {
        this.openAIClient = openAIClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }
}
