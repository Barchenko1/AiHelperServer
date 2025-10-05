package com.helper.server.registry;

import com.helper.server.entity.UserSession;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UserSessionRegistry {
    private static final SecureRandom RNG = new SecureRandom();

    private final Map<String, UserSession> byUser = new ConcurrentHashMap<>();
    private final Map<String, String> codeToUser = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> usernameSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> usernameHistory = new ConcurrentHashMap<>();

    public synchronized UserSession getOrCreate(String username, Duration ttl,
                                          java.util.function.Function<Duration, String> tokenSupplier) {
        Objects.requireNonNull(username, "username");
        UserSession current = byUser.get(username);
        if (current != null && !current.isExpired()) {
            return current;
        }
        String token = tokenSupplier.apply(ttl);
        String code = sixDigitsUnique();
        Instant exp = Instant.now().plus(ttl);
        UserSession fresh = new UserSession(username, token, code, exp);

        if (current != null) {
            codeToUser.remove(current.code);
            tokenToUser.remove(current.token);
        }
        byUser.put(username, fresh);
        codeToUser.put(code, username);
        tokenToUser.put(token, username);
        return fresh;
    }

    public UserSession byCode(String code) {
        String user = codeToUser.get(code);
        if (user == null) return null;
        UserSession e = byUser.get(user);
        if (e == null || e.isExpired()) {
            if (e != null) remove(user);
            return null;
        }
        return e;
    }

    public UserSession byToken(String token) {
        String user = tokenToUser.get(token);
        if (user == null) return null;
        UserSession e = byUser.get(user);
        if (e == null || e.isExpired()) {
            if (e != null) remove(user);
            return null;
        }
        return e;
    }

    public UserSession byUsername(String username) {
        if (username == null) return null;
        UserSession e = byUser.get(username);
        if (e == null || e.isExpired()) {
            if (e != null) remove(username);
            return null;
        }
        return e;
    }

    public void addSessionId(String username, WebSocketSession session) {
        usernameSessionsMap
                .computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void removeSessionId(String username, WebSocketSession session) {
        Set<WebSocketSession> set = usernameSessionsMap.get(username);
        if (set == null) return;
        set.remove(session);
        if (set.isEmpty()) {
            usernameSessionsMap.remove(username, set);
        }
    }

    public synchronized void remove(String username) {
        Set<WebSocketSession> sessions = usernameSessionsMap.get(username);
        if (sessions != null && sessions.size() == 1) {
            UserSession e = byUser.remove(username);
            if (e != null) {
                codeToUser.remove(e.code);
                tokenToUser.remove(e.token);
            }
        }
    }

    public Set<WebSocketSession> getSessions(String username) {
        return usernameSessionsMap.get(username);
    }

    public Map<String, Set<WebSocketSession>> getUsernameSessionsMap() {
        return usernameSessionsMap;
    }

    public CopyOnWriteArrayList<String> getHistoryMessages(String username) {
        return usernameHistory.get(username);
    }

    public Map<String, CopyOnWriteArrayList<String>> getHistory() {
        return usernameHistory;
    }

    private String sixDigitsUnique() {
        for (int i = 0; i < 5; i++) {
            String c = String.format("%06d", RNG.nextInt(1_000_000));
            if (!codeToUser.containsKey(c)) return c;
        }
        return String.format("%06d", RNG.nextInt(1_000_000));
    }

}
