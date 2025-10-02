package com.helper.server.service;

import com.helper.server.openaiclient.IOpenAIClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

    protected final IOpenAIClient openAIClient;
    protected final IJsonTemplateService jsonTemplateService;
    protected final WSHandler wsHandler;

    public AbstractService(IOpenAIClient openAIClient,
                           IJsonTemplateService jsonTemplateService,
                           WSHandler wsHandler) {
        this.openAIClient = openAIClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }
}
