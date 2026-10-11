package com.helper.server.rest;

import com.helper.server.registry.ClientPresenceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Powers the UI's green/red lights: the CLI and the extension ping with their code, the UI asks who checked in.
 * A client with a wrong/expired code gets 401 on ping and so never shows up as connected.
 */
@RestController
@RequestMapping("/api/v1")
public class PresenceController {

    private final ClientPresenceRegistry presence;

    public PresenceController(ClientPresenceRegistry presence) {
        this.presence = presence;
    }

    @PostMapping("/ping")
    public ResponseEntity<Void> ping(@RequestHeader(value = "X-Client", defaultValue = "") String client,
                                     Principal principal) {
        if (!ClientPresenceRegistry.isKnownClient(client)) {
            return ResponseEntity.badRequest().build();
        }
        presence.record(principal.getName(), client);
        return ResponseEntity.ok().build();
    }

    /** Sent by a client that is stopping or switching to another code, so the UI turns red immediately. */
    @PostMapping("/ping/offline")
    public ResponseEntity<Void> offline(@RequestHeader(value = "X-Client", defaultValue = "") String client,
                                        Principal principal) {
        if (!ClientPresenceRegistry.isKnownClient(client)) {
            return ResponseEntity.badRequest().build();
        }
        presence.forget(principal.getName(), client);
        return ResponseEntity.ok().build();
    }

    /** Public on purpose (always 200): a logged-out UI gets session=false instead of a 401/Basic-auth popup. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean valid = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        Map<String, Object> clients = new HashMap<>();
        clients.put(ClientPresenceRegistry.CLI, null);
        clients.put(ClientPresenceRegistry.EXTENSION, null);
        if (valid) {
            clients.putAll(presence.secondsSinceSeen(auth.getName()));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("session", valid);
        body.put("clients", clients);
        return body;
    }
}
