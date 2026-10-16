package com.helper.server.rest;

import com.helper.server.entity.UserSession;
import com.helper.server.registry.UserSessionRegistry;
import com.helper.server.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserSessionRegistry sessions;
    @Value("${auth.token-ttl}")
    private Duration tokenTtl;

    public AuthController(AuthenticationManager authManager,
                          JwtUtil jwtUtil,
                          UserSessionRegistry sessions) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.sessions = sessions;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, String>> login(@RequestParam(required = false) String username,
                                                     @RequestParam(required = false) String password,
                                                     @RequestParam(required = false) String code) {
        UserSession entry = code != null ? sessions.byCode(code.trim()) : loginWithPassword(username, password);
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(sessionBody(entry));
    }

    private UserSession loginWithPassword(String username, String password) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        return sessions.getOrCreate(
                auth.getName(),
                tokenTtl,
                t -> jwtUtil.issue(auth.getName(), t)
        );
    }

    private Map<String, String> sessionBody(UserSession entry) {
        return Map.of(
                "token", entry.token,
                "code",  entry.code,
                "expiresAt", entry.expiresAt.toString()
        );
    }

    @PostMapping(value = "/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestHeader(value = "X-Auth-Code", required = false) String codeHeader,
                                       Principal principal) {
        if (principal != null) {
            sessions.remove(principal.getName());
        }
        return ResponseEntity.ok().build();
    }
}
