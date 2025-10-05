package com.helper.server.websocket;

import com.helper.server.entity.UserSession;
import com.helper.server.registry.UserSessionRegistry;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WSHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WSHandler.class);

    private final UserSessionRegistry userSessionRegistry;

    public WSHandler(UserSessionRegistry userSessionRegistry) {
        this.userSessionRegistry = userSessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws IOException {
        LOGGER.info("🚀 Open: id={}, remote={}, protocol={}",
                session.getId(), session.getRemoteAddress(), session.getAcceptedProtocol());
        String userId = getUserId(session);
        userSessionRegistry.addSessionId(userId, session);
        if (userId == null || userId.isEmpty()) {
            LOGGER.warn("WS open without userId (closing): id={}, remote={}", session.getId(), session.getRemoteAddress());
            safeClose(session, CloseStatus.NOT_ACCEPTABLE.withReason("userId missing"));
            return;
        }
        userSessionRegistry.getUsernameSessionsMap().computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        LOGGER.info("🚀 WS open: id={}, user={}, remote={}, protocol={}",
                session.getId(), userId, session.getRemoteAddress(), session.getAcceptedProtocol());

        CopyOnWriteArrayList<String> history = userSessionRegistry.getHistoryMessages(userId);
        if (history != null && session.isOpen()) {
            for (String msg : history) {
                session.sendMessage(new TextMessage(msg));
            }
        } else {
            userSessionRegistry.getHistory().computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        }
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
        String userId = getUserId(session);
        if (userId == null) return;
        String incoming = message.getPayload();
        appendUserHistory(userId, incoming);
        sendToUser(userId, incoming);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        LOGGER.info("❌ Close: id={}, code={}, reason={}", session.getId(), status.getCode(), status.getReason());
        String userId = getUserId(session);
        if (userId != null) {
            Set<WebSocketSession> set = userSessionRegistry.getSessions(userId);
            if (set != null) {
                set.remove(session);
                userSessionRegistry.removeSessionId(userId, session);
                UserSession userSession = userSessionRegistry.byUsername(userId);
                if (set.isEmpty() && userSession == null || set.isEmpty() && userSession.isExpired()) {
                    userSessionRegistry.removeSessionId(userId, session);
                }
            }
        }
        LOGGER.info("❌ WS close: id={}, user={}, code={}, reason={}",
                session.getId(), userId, status.getCode(), status.getReason());
    }

    public void broadcastToUser(String userId, String message) {
        sendToUser(userId, message);
    }

    private void sendToUser(String userId, String message) {
        Set<WebSocketSession> sessions = userSessionRegistry.getSessions(userId);
        CopyOnWriteArrayList<String> history = userSessionRegistry.getHistoryMessages(userId);
        boolean isDuplicate = !history.isEmpty() && message.equals(history.get(history.size() - 1));
        if (!isDuplicate) {
            history.add(message);
            int max = 50;
            while (history.size() > max) history.remove(0);
        }
        TextMessage payload = new TextMessage(message);
        for (WebSocketSession s : sessions) {
            if (s != null && s.isOpen()) {
                try {
                    s.sendMessage(payload);
                } catch (IOException e) {
                    LOGGER.warn("Send failed to user={} session={}: {}", userId, s.getId(), e.getMessage());
                }
            }
        }
    }

    private String getUserId(WebSocketSession s) {
        return s.getAttributes().get("userId").toString();
    }

    private void appendUserHistory(String userId, String message) {
        userSessionRegistry.getHistory().computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(message);
        CopyOnWriteArrayList<String> list = userSessionRegistry.getHistoryMessages(userId);
        int max = 50;
        if (list.size() > max) {
            while (list.size() > max) list.remove(0);
        }
    }

    private void safeClose(WebSocketSession s, CloseStatus status) {
        try { s.close(status); } catch (Exception ignored) {}
    }

}
