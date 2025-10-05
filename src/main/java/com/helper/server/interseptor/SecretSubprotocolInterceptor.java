package com.helper.server.interseptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

public class SecretSubprotocolInterceptor implements HandshakeInterceptor {
    private final String secret;
    SecretSubprotocolInterceptor(String secret) { this.secret = secret; }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
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
    }

    boolean validateToken(String token) {
        return secret.equals(token);
    }
}
