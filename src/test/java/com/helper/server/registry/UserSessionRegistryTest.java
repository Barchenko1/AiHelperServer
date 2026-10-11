package com.helper.server.registry;

import com.helper.server.entity.UserSession;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class UserSessionRegistryTest {

    private final UserSessionRegistry registry = new UserSessionRegistry();
    private final AtomicInteger issued = new AtomicInteger();

    private UserSession login(String user, Duration ttl) {
        return registry.getOrCreate(user, ttl, t -> "token-" + user + "-" + issued.incrementAndGet());
    }

    @Test
    void loginCreatesSessionWithSixDigitCodeAndExpiry() {
        Instant before = Instant.now();
        UserSession s = login("pa", Duration.ofHours(3));

        assertThat(s.username).isEqualTo("pa");
        assertThat(s.token).isEqualTo("token-pa-1");
        assertThat(s.code).matches("\\d{6}");
        assertThat(s.expiresAt).isBetween(before.plus(Duration.ofHours(3)), Instant.now().plus(Duration.ofHours(3)));
        assertThat(s.isExpired()).isFalse();
    }

    @Test
    void loggingInAgainReusesTheLiveSession() {
        UserSession first = login("pa", Duration.ofHours(3));

        assertThat(login("pa", Duration.ofHours(3))).isSameAs(first);
        assertThat(issued.get()).isEqualTo(1);
    }

    @Test
    void expiredSessionIsReplacedAndOldCodeAndTokenStopWorking() {
        UserSession old = login("pa", Duration.ofSeconds(-1));

        UserSession fresh = login("pa", Duration.ofHours(3));

        assertThat(fresh).isNotSameAs(old);
        assertThat(registry.byToken(fresh.token)).isSameAs(fresh);
        assertThat(registry.byCode(fresh.code)).isSameAs(fresh);
        assertThat(registry.byToken(old.token)).isNull();
        if (!old.code.equals(fresh.code)) {
            assertThat(registry.byCode(old.code)).isNull();
        }
    }

    @Test
    void lookupsFindTheLiveSession() {
        UserSession s = login("pa", Duration.ofHours(3));

        assertThat(registry.byCode(s.code)).isSameAs(s);
        assertThat(registry.byToken(s.token)).isSameAs(s);
        assertThat(registry.byUsername("pa")).isSameAs(s);
    }

    @Test
    void lookupsOfUnknownValuesReturnNull() {
        assertThat(registry.byCode("000000")).isNull();
        assertThat(registry.byToken("nope")).isNull();
        assertThat(registry.byUsername("nobody")).isNull();
        assertThat(registry.byUsername(null)).isNull();
    }

    @Test
    void expiredSessionIsNotReturnedByAnyLookup() {
        UserSession s = login("pa", Duration.ofSeconds(-1));

        assertThat(registry.byCode(s.code)).isNull();
        assertThat(registry.byToken(s.token)).isNull();
        assertThat(registry.byUsername("pa")).isNull();
    }

    @Test
    void expiredSessionWithOneSocketIsEvictedOnLookup() {
        UserSession s = login("pa", Duration.ofSeconds(-1));
        registry.addSessionId("pa", mock(WebSocketSession.class));

        assertThat(registry.byCode(s.code)).isNull();
        // evicted: the token mapping is gone too
        assertThat(registry.byToken(s.token)).isNull();
        assertThat(registry.byUsername("pa")).isNull();
    }

    @Test
    void lookupWhereUserMappingOutlivedSessionReturnsNull() {
        UserSession s = login("pa", Duration.ofHours(3));
        registry.addSessionId("pa", mock(WebSocketSession.class));
        registry.remove("pa");

        assertThat(registry.byCode(s.code)).isNull();
        assertThat(registry.byToken(s.token)).isNull();
    }

    @Test
    void removeOnlyDropsTheSessionWhenExactlyOneSocketIsOpen() {
        login("pa", Duration.ofHours(3));
        registry.remove("pa");
        assertThat(registry.byUsername("pa")).as("no sockets: kept").isNotNull();

        registry.addSessionId("pa", mock(WebSocketSession.class));
        registry.addSessionId("pa", mock(WebSocketSession.class));
        registry.remove("pa");
        assertThat(registry.byUsername("pa")).as("two sockets: kept").isNotNull();
    }

    @Test
    void removeWithOneSocketButNoSessionIsHarmless() {
        registry.addSessionId("ghost", mock(WebSocketSession.class));
        registry.remove("ghost");
        assertThat(registry.byUsername("ghost")).isNull();
    }

    @Test
    void socketsAreTrackedPerUserAndCleanedUpWhenLastOneLeaves() {
        WebSocketSession a = mock(WebSocketSession.class);
        WebSocketSession b = mock(WebSocketSession.class);
        registry.addSessionId("pa", a);
        registry.addSessionId("pa", b);
        assertThat(registry.getSessions("pa")).containsExactlyInAnyOrder(a, b);

        registry.removeSessionId("pa", a);
        assertThat(registry.getSessions("pa")).containsExactly(b);

        registry.removeSessionId("pa", b);
        assertThat(registry.getSessions("pa")).isNull();
        assertThat(registry.getUsernameSessionsMap()).doesNotContainKey("pa");

        registry.removeSessionId("nobody", a); // no-op
    }

    @Test
    void historyIsExposedPerUser() {
        assertThat(registry.getHistoryMessages("pa")).isNull();
        registry.getHistory().put("pa", new CopyOnWriteArrayList<>(java.util.List.of("hi")));
        assertThat(registry.getHistoryMessages("pa")).containsExactly("hi");
    }

    @Test
    void codesAreUniqueAcrossUsers() {
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            codes.add(login("user" + i, Duration.ofHours(1)).code);
        }
        assertThat(codes).hasSize(200);
    }

    @Test
    void usernameIsRequired() {
        assertThatThrownBy(() -> login(null, Duration.ofHours(1))).isInstanceOf(NullPointerException.class);
    }
}
