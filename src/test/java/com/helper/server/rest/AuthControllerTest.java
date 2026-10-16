package com.helper.server.rest;

import com.helper.server.registry.UserSessionRegistry;
import com.helper.server.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthenticationManager authManager = mock(AuthenticationManager.class);
    private final JwtUtil jwt = new JwtUtil("test-secret-that-is-long-enough-for-hs256!!");
    private final UserSessionRegistry sessions = new UserSessionRegistry();
    private final AuthController controller = controller(sessions, Duration.ofHours(3));

    private AuthController controller(UserSessionRegistry registry, Duration tokenTtl) {
        AuthController c = new AuthController(authManager, jwt, registry);
        ReflectionTestUtils.setField(c, "tokenTtl", tokenTtl);
        return c;
    }

    private void credentialsAccepted(String user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(authManager.authenticate(any())).thenReturn(auth);
    }

    @Test
    void loginUsesTheConfiguredTokenLifetime() {
        credentialsAccepted("pa");
        AuthController shortLived = controller(new UserSessionRegistry(), Duration.ofMinutes(30));
        Instant before = Instant.now();

        Instant expiresAt = Instant.parse(shortLived.login("pa", "secret", null).getBody().get("expiresAt"));

        assertThat(expiresAt).isBetween(before.plus(Duration.ofMinutes(30)), Instant.now().plus(Duration.ofMinutes(30)));
    }

    @Test
    void loginReturnsTokenCodeAndAThreeHourExpiry() {
        credentialsAccepted("pa");
        Instant before = Instant.now();

        Map<String, String> body = controller.login("pa", "secret", null).getBody();

        assertThat(jwt.validateAndGetUser(body.get("token"))).isEqualTo("pa");
        assertThat(body.get("code")).matches("\\d{6}");
        Instant expiresAt = Instant.parse(body.get("expiresAt"));
        assertThat(expiresAt).isBetween(before.plus(Duration.ofHours(3)), Instant.now().plus(Duration.ofHours(3)));
        assertThat(sessions.byToken(body.get("token"))).isNotNull();
    }

    @Test
    void secondLoginReturnsTheSameSession() {
        credentialsAccepted("pa");

        assertThat(controller.login("pa", "secret", null).getBody()).isEqualTo(controller.login("pa", "secret", null).getBody());
    }

    @Test
    void codeJoinsTheSameSessionAsTheLogin() {
        credentialsAccepted("pa");
        Map<String, String> first = controller.login("pa", "secret", null).getBody();

        var joined = controller.login(null, null, " " + first.get("code") + " ");

        assertThat(joined.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joined.getBody()).isEqualTo(first);
    }

    @Test
    void unknownCodeCannotJoin() {
        assertThat(controller.login(null, null, "000000").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.login(null, null, "000000").getBody()).isNull();
    }

    @Test
    void badCredentialsPropagate() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> controller.login("pa", "wrong", null)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutRemovesSessionOfTheCaller() {
        UserSessionRegistry registry = mock(UserSessionRegistry.class);
        AuthController c = controller(registry, Duration.ofHours(3));
        Principal principal = () -> "pa";

        assertThat(c.logout("Bearer x", "123456", principal).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(registry).remove("pa");
    }

    @Test
    void logoutWithoutPrincipalIsStillOk() {
        UserSessionRegistry registry = mock(UserSessionRegistry.class);
        AuthController c = controller(registry, Duration.ofHours(3));

        assertThat(c.logout(null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(registry);
    }
}
