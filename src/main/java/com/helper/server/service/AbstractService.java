package com.helper.server.service;

import com.helper.server.client.IAIClient;
import com.helper.server.template.IJsonTemplateService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

    protected final IAIClient gptClient;
    protected final IJsonTemplateService jsonTemplateService;
    protected final WSHandler wsHandler;

    public AbstractService(IAIClient gptClient,
                           IJsonTemplateService jsonTemplateService,
                           WSHandler wsHandler) {
        this.gptClient = gptClient;
        this.jsonTemplateService = jsonTemplateService;
        this.wsHandler = wsHandler;
    }
}
