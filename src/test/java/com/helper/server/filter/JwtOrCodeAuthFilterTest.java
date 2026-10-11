package com.helper.server.filter;

import com.helper.server.entity.UserSession;
import com.helper.server.registry.UserSessionRegistry;
import com.helper.server.util.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtOrCodeAuthFilterTest {

    private final JwtUtil jwt = new JwtUtil("test-secret-that-is-long-enough-for-hs256!!");
    private final UserSessionRegistry sessions = new UserSessionRegistry();
    private final InMemoryUserDetailsManager uds = new InMemoryUserDetailsManager(
            User.withUsername("pa").password("x").roles("USER").build());
    private final JwtOrCodeAuthFilter filter = new JwtOrCodeAuthFilter(jwt, uds, sessions);
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest req = new MockHttpServletRequest();
    private final MockHttpServletResponse res = new MockHttpServletResponse();
    private UserSession session;

    @BeforeEach
    void login() {
        session = sessions.getOrCreate("pa", Duration.ofHours(3), t -> jwt.issue("pa", t));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private String authenticatedUser() throws Exception {
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    @Test
    void bearerTokenOfLiveSessionAuthenticates() throws Exception {
        req.addHeader("Authorization", "Bearer " + session.token);
        assertThat(authenticatedUser()).isEqualTo("pa");
    }

    @Test
    void bearerTokenNotInRegistryIsIgnored() throws Exception {
        req.addHeader("Authorization", "Bearer " + jwt.issue("pa", Duration.ofHours(1)));
        assertThat(authenticatedUser()).isNull();
    }

    @Test
    void codeHeaderAuthenticates() throws Exception {
        req.addHeader("X-Auth-Code", session.code);
        assertThat(authenticatedUser()).isEqualTo("pa");
    }

    @Test
    void codeInAuthorizationHeaderAuthenticates() throws Exception {
        req.addHeader("Authorization", "code " + session.code);
        assertThat(authenticatedUser()).isEqualTo("pa");
    }

    @Test
    void unknownBearerFallsBackToCodeHeader() throws Exception {
        req.addHeader("Authorization", "Bearer garbage");
        req.addHeader("X-Auth-Code", session.code);
        assertThat(authenticatedUser()).isEqualTo("pa");
    }

    @Test
    void wrongCodeDoesNotAuthenticate() throws Exception {
        req.addHeader("X-Auth-Code", "not-a-code");
        assertThat(authenticatedUser()).isNull();
    }

    @Test
    void emptyCodeDoesNotAuthenticate() throws Exception {
        req.addHeader("X-Auth-Code", "");
        assertThat(authenticatedUser()).isNull();
    }

    @Test
    void basicAuthHeaderIsNotACode() throws Exception {
        req.addHeader("Authorization", "Basic abc");
        assertThat(authenticatedUser()).isNull();
    }

    @Test
    void noHeadersLeaveRequestAnonymous() throws Exception {
        assertThat(authenticatedUser()).isNull();
    }

    @Test
    void existingAuthenticationIsKept() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("other", null, List.of()));
        req.addHeader("Authorization", "Bearer " + session.token);
        assertThat(authenticatedUser()).isEqualTo("other");
    }
}
