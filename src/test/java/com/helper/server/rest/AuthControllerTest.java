package com.helper.server.rest;

import com.helper.server.registry.UserSessionRegistry;
import com.helper.server.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

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
    private final AuthController controller = new AuthController(authManager, jwt, sessions);

    private void credentialsAccepted(String user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(authManager.authenticate(any())).thenReturn(auth);
    }

    @Test
    void tokenIsValidForThreeHours() {
        assertThat(AuthController.TOKEN_TTL).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void loginReturnsTokenCodeAndAThreeHourExpiry() {
        credentialsAccepted("pa");
        Instant before = Instant.now();

        Map<String, String> body = controller.login("pa", "secret");

        assertThat(jwt.validateAndGetUser(body.get("token"))).isEqualTo("pa");
        assertThat(body.get("code")).matches("\\d{6}");
        Instant expiresAt = Instant.parse(body.get("expiresAt"));
        assertThat(expiresAt).isBetween(before.plus(Duration.ofHours(3)), Instant.now().plus(Duration.ofHours(3)));
        assertThat(sessions.byToken(body.get("token"))).isNotNull();
    }

    @Test
    void secondLoginReturnsTheSameSession() {
        credentialsAccepted("pa");

        assertThat(controller.login("pa", "secret")).isEqualTo(controller.login("pa", "secret"));
    }

    @Test
    void badCredentialsPropagate() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> controller.login("pa", "wrong")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutRemovesSessionOfTheCaller() {
        UserSessionRegistry registry = mock(UserSessionRegistry.class);
        AuthController c = new AuthController(authManager, jwt, registry);
        Principal principal = () -> "pa";

        assertThat(c.logout("Bearer x", "123456", principal).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(registry).remove("pa");
    }

    @Test
    void logoutWithoutPrincipalIsStillOk() {
        UserSessionRegistry registry = mock(UserSessionRegistry.class);
        AuthController c = new AuthController(authManager, jwt, registry);

        assertThat(c.logout(null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(registry);
    }
}
