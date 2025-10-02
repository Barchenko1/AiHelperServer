package com.helper.server.process;

import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcess.class);

    protected final WSHandler wsHandler;

    public AbstractProcess(WSHandler wsHandler) {
        this.wsHandler = wsHandler;
    }
}
