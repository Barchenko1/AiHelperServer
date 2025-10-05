package com.helper.server.config;

import com.helper.server.interseptor.CodeOrJwtWsInterceptor;
import com.helper.server.util.JwtUtil;
import com.helper.server.util.CodeToTokenService;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketSecureConfig implements WebSocketConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketSecureConfig.class);

    private final WSHandler handler;
    private final JwtUtil jwtUtil;
    private final CodeToTokenService codeToTokenService;

    public WebSocketSecureConfig(WSHandler handler,
                                 JwtUtil jwtUtil,
                                 CodeToTokenService codeToTokenService) {
        this.handler = handler;
        this.jwtUtil = jwtUtil;
        this.codeToTokenService = codeToTokenService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .addInterceptors(new CodeOrJwtWsInterceptor(jwtUtil, codeToTokenService))
                .setAllowedOriginPatterns("*");
    }
}
