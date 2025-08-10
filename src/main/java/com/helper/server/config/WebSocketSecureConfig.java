package com.helper.server.config;

import com.helper.server.websocket.WSHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketSecureConfig implements WebSocketConfigurer {

    private final WSHandler handler;
    private final String webSocketSecret;

    public WebSocketSecureConfig(WSHandler handler, @Value("${WEBSOCKET_API_TOKEN}") String webSocketSecret) {
        this.handler = handler;
        this.webSocketSecret = webSocketSecret;
    }

    @Bean
    public DefaultHandshakeHandler handshakeHandler() {
        return new DefaultHandshakeHandler() {
            private String determineSubProtocol(java.util.List<String> requested, WebSocketHandler handler) {
                return requested.isEmpty() ? null : requested.get(0);
            }
        };
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .setHandshakeHandler(handshakeHandler())
                .addInterceptors(new SecretSubprotocolInterceptor(webSocketSecret))
                .setAllowedOriginPatterns("*");
    }

    private static class SecretSubprotocolInterceptor implements HandshakeInterceptor {
        private final String secret;
        SecretSubprotocolInterceptor(String secret) { this.secret = secret; }

        @Override
        public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                       @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
            List<String> origins = request.getHeaders().get("Origin");
            if (origins == null || !origins.contains("http://localhost:3000")) {
                return false;
            }
            List<String> protocolList = request.getHeaders().get("Sec-WebSocket-Protocol");
            String token = (protocolList != null && !protocolList.isEmpty()) ? protocolList.get(0) : null;

            if (!validateToken(token)) {
                return false;
            }

            response.getHeaders().add("Sec-WebSocket-Protocol", token);

            attributes.put("authorized", true);
            return true;
        }

        @Override
        public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, Exception ex) {
            String text = "";
        }

        boolean validateToken(String token) {
            return secret.equals(token);
        }
    }

}
