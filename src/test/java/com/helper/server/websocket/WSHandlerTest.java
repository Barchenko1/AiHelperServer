package com.helper.server.websocket;

import com.helper.server.registry.UserSessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WSHandlerTest {

    private final UserSessionRegistry registry = new UserSessionRegistry();
    private final WSHandler handler = new WSHandler(registry);

    private static WebSocketSession socket(String userId) {
        WebSocketSession s = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", userId);
        when(s.getAttributes()).thenReturn(attrs);
        when(s.isOpen()).thenReturn(true);
        when(s.getId()).thenReturn("id-" + userId);
        return s;
    }

    @Test
    void firstConnectionRegistersSocketAndStartsEmptyHistory() throws Exception {
        WebSocketSession s = socket("pa");

        handler.afterConnectionEstablished(s);

        assertThat(registry.getSessions("pa")).containsExactly(s);
        assertThat(registry.getHistoryMessages("pa")).isEmpty();
        verify(s, never()).sendMessage(any());
    }

    @Test
    void reconnectReplaysHistory() throws Exception {
        registry.getHistory().put("pa", new CopyOnWriteArrayList<>(List.of("one", "two")));
        WebSocketSession s = socket("pa");

        handler.afterConnectionEstablished(s);

        verify(s).sendMessage(new TextMessage("one"));
        verify(s).sendMessage(new TextMessage("two"));
    }

    @Test
    void closedSocketDoesNotGetHistoryReplay() throws Exception {
        registry.getHistory().put("pa", new CopyOnWriteArrayList<>(List.of("one")));
        WebSocketSession s = socket("pa");
        when(s.isOpen()).thenReturn(false);

        handler.afterConnectionEstablished(s);

        verify(s, never()).sendMessage(any());
    }

    @Test
    void connectionWithoutUserIdIsClosed() throws Exception {
        WebSocketSession s = socket("");
        doThrow(new IOException("already gone")).when(s).close(any());

        handler.afterConnectionEstablished(s);

        verify(s).close(CloseStatus.NOT_ACCEPTABLE.withReason("userId missing"));
        assertThat(registry.getHistoryMessages("")).isNull();
    }

    @Test
    void broadcastSendsToEveryOpenSocketAndRemembersMessage() throws Exception {
        WebSocketSession a = socket("pa");
        WebSocketSession b = socket("pa");
        WebSocketSession closed = socket("pa");
        handler.afterConnectionEstablished(a);
        handler.afterConnectionEstablished(b);
        handler.afterConnectionEstablished(closed);
        when(closed.isOpen()).thenReturn(false);

        handler.broadcastToUser("pa", "answer");

        verify(a).sendMessage(new TextMessage("answer"));
        verify(b).sendMessage(new TextMessage("answer"));
        verify(closed, never()).sendMessage(any());
        assertThat(registry.getHistoryMessages("pa")).containsExactly("answer");
    }

    @Test
    void repeatedMessageIsNotStoredTwice() throws Exception {
        handler.afterConnectionEstablished(socket("pa"));

        handler.broadcastToUser("pa", "same");
        handler.broadcastToUser("pa", "same");

        assertThat(registry.getHistoryMessages("pa")).containsExactly("same");
    }

    @Test
    void historyIsCappedAtFiftyMessages() throws Exception {
        handler.afterConnectionEstablished(socket("pa"));

        for (int i = 0; i < 60; i++) handler.broadcastToUser("pa", "m" + i);

        assertThat(registry.getHistoryMessages("pa")).hasSize(50).startsWith("m10").endsWith("m59");
    }

    @Test
    void failingSocketDoesNotStopDeliveryToOthers() throws Exception {
        WebSocketSession broken = socket("pa");
        WebSocketSession ok = socket("pa");
        handler.afterConnectionEstablished(broken);
        handler.afterConnectionEstablished(ok);
        doThrow(new IOException("pipe")).when(broken).sendMessage(any());

        handler.broadcastToUser("pa", "answer");

        verify(ok).sendMessage(new TextMessage("answer"));
    }

    @Test
    void incomingTextIsStoredOnceAndEchoedToUser() throws Exception {
        WebSocketSession s = socket("pa");
        handler.afterConnectionEstablished(s);

        handler.handleTextMessage(s, new TextMessage("hello"));

        assertThat(registry.getHistoryMessages("pa")).containsExactly("hello");
        verify(s).sendMessage(new TextMessage("hello"));
    }

    @Test
    void incomingTextHistoryIsCappedAtFifty() throws Exception {
        WebSocketSession s = socket("pa");
        handler.afterConnectionEstablished(s);

        for (int i = 0; i < 55; i++) handler.handleTextMessage(s, new TextMessage("m" + i));

        assertThat(registry.getHistoryMessages("pa")).hasSize(50).endsWith("m54");
    }

    @Test
    void closingTheLastSocketForgetsIt() throws Exception {
        WebSocketSession s = socket("pa");
        handler.afterConnectionEstablished(s);

        handler.afterConnectionClosed(s, CloseStatus.NORMAL);

        assertThat(registry.getSessions("pa")).isNull();
    }

    @Test
    void closingOneOfTwoSocketsKeepsTheOther() throws Exception {
        registry.getOrCreate("pa", Duration.ofHours(1), t -> "tok");
        WebSocketSession a = socket("pa");
        WebSocketSession b = socket("pa");
        handler.afterConnectionEstablished(a);
        handler.afterConnectionEstablished(b);

        handler.afterConnectionClosed(a, CloseStatus.NORMAL);

        assertThat(registry.getSessions("pa")).containsExactly(b);
    }

    @Test
    void closingLastSocketOfLiveSessionKeepsLogin() throws Exception {
        registry.getOrCreate("pa", Duration.ofHours(1), t -> "tok");
        WebSocketSession s = socket("pa");
        handler.afterConnectionEstablished(s);

        handler.afterConnectionClosed(s, CloseStatus.NORMAL);

        assertThat(registry.byUsername("pa")).isNotNull();
    }

    @Test
    void closingUnknownSocketIsHarmless() {
        handler.afterConnectionClosed(socket("nobody"), CloseStatus.GOING_AWAY);
        assertThat(registry.getSessions("nobody")).isNull();
    }

    @Test
    void handleTextIgnoredWithoutUser() throws Exception {
        WebSocketSession s = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", new Object() { public String toString() { return null; } });
        when(s.getAttributes()).thenReturn(attrs);

        handler.handleTextMessage(s, new TextMessage("x"));
        handler.afterConnectionClosed(s, CloseStatus.NORMAL);

        assertThat(registry.getHistory()).isEmpty();
    }
}
