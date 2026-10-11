package com.helper.server.interseptor;

import com.helper.server.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CodeOrJwtWsInterceptorTest {

    private final JwtUtil jwt = new JwtUtil("test-secret-that-is-long-enough-for-hs256!!");
    private final CodeOrJwtWsInterceptor interceptor = new CodeOrJwtWsInterceptor(jwt);
    private final MockHttpServletResponse servletRes = new MockHttpServletResponse();
    private final Map<String, Object> attrs = new HashMap<>();

    private boolean handshake(String query) throws Exception {
        MockHttpServletRequest servletReq = new MockHttpServletRequest("GET", "/ws");
        servletReq.setQueryString(query);
        ServletServerHttpResponse res = new ServletServerHttpResponse(servletRes);
        boolean ok = interceptor.beforeHandshake(new ServletServerHttpRequest(servletReq), res, mock(WebSocketHandler.class), attrs);
        res.flush();
        return ok;
    }

    @Test
    void validTokenPutsUserIdIntoAttributes() throws Exception {
        assertThat(handshake("token=" + jwt.issue("pa", Duration.ofHours(1)))).isTrue();
        assertThat(attrs).containsEntry("userId", "pa");
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        assertThat(handshake(null)).isFalse();
        assertThat(servletRes.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void emptyTokenIsUnauthorized() throws Exception {
        assertThat(handshake("token=")).isFalse();
        assertThat(servletRes.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        assertThat(handshake("token=garbage")).isFalse();
        assertThat(servletRes.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(attrs).isEmpty();
    }

    @Test
    void afterHandshakeDoesNothing() {
        interceptor.afterHandshake(null, null, null, null);
    }
}
