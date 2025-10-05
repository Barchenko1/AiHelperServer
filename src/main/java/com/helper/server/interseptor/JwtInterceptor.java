package com.helper.server.interseptor;

import com.helper.server.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

public class JwtInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwt;

    public JwtInterceptor(JwtUtil jwt) {
        this.jwt = jwt;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {
        String token = extractToken(req);
        if (token == null || token.isEmpty()) {
            res.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            String user = jwt.validateAndGetUser(token);
            if (user == null || user.trim().isEmpty()) {
                res.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attrs.put("userId", user);
            return true;
        } catch (Exception ex) {
            res.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler wsHandler, Exception ex) {
    }

    private String extractToken(ServerHttpRequest req) {
        URI uri = req.getURI();
        MultiValueMap<String, String> qp =
                UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String token = firstNonBlank(qp.getFirst("token"), qp.getFirst("access_token"));
        if (token != null && !token.isEmpty()) {
            return token.trim();
        }

        HttpHeaders h = req.getHeaders();
        String auth = h.getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }

        String alt = h.getFirst("X-Auth-Token");
        if (alt != null && !alt.trim().isEmpty()) {
            return alt.trim();
        }

        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        if (b != null && !b.trim().isEmpty()) return b;
        return null;
    }
}
