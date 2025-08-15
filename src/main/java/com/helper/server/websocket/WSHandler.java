package com.helper.server.websocket;

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

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final CopyOnWriteArrayList<String> messageHistory = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws IOException {
        sessions.add(session);
        LOGGER.info("🚀 Open: id={}, remote={}, protocol={}",
                session.getId(), session.getRemoteAddress(), session.getAcceptedProtocol());
        for (String message : messageHistory) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        String incoming = message.getPayload();
        messageHistory.add(incoming);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(incoming));
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        LOGGER.info("❌ Close: id={}, code={}, reason={}", session.getId(), status.getCode(), status.getReason());
        sessions.remove(session);
    }

    public void broadcast(String message) {
        sessions.forEach(s -> {
            if (s.isOpen()) {
                try {
                    messageHistory.add(message);
                    s.sendMessage(new TextMessage(message));
                } catch (Exception ignored) {

                }
            }
        });
    }

}
