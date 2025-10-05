package com.helper.server.interseptor;

import com.helper.server.util.CodeToTokenService;
import com.helper.server.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class CodeOrJwtWsInterceptor implements HandshakeInterceptor {
    private final JwtUtil jwt;
    private final CodeToTokenService codeToTokenService;

    public CodeOrJwtWsInterceptor(JwtUtil jwt, CodeToTokenService codeToTokenService) {
        this.jwt = jwt; this.codeToTokenService = codeToTokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {
        MultiValueMap<String, String> qp = UriComponentsBuilder.fromUri(req.getURI()).build().getQueryParams();
        String token = qp.getFirst("token");

        try {
            if (token != null && !token.isEmpty()) {
                attrs.put("userId", jwt.validateAndGetUser(token));
                return true;
            }
            res.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        } catch (Exception e) {
            res.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override public void afterHandshake(ServerHttpRequest a, ServerHttpResponse b,
                                         WebSocketHandler c, Exception d) {

    }
}

