package com.helper.server.rest;

import com.helper.server.registry.ClientPresenceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PresenceControllerTest {

    private final ClientPresenceRegistry registry = new ClientPresenceRegistry();
    private final PresenceController controller = new PresenceController(registry);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static UsernamePasswordAuthenticationToken loginAs(String user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    @Test
    void pingRecordsTheClientForTheLoggedInUser() {
        var auth = loginAs("pbrchnk");

        var response = controller.ping("cli", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registry.secondsSinceSeen("pbrchnk")).containsKey("cli");
    }

    @Test
    void pingFromUnknownClientIsRejected() {
        var auth = loginAs("pbrchnk");

        assertThat(controller.ping("toaster", auth).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.ping("", auth).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(registry.secondsSinceSeen("pbrchnk")).isEmpty();
    }

    @Test
    void offlineRemovesTheClientImmediately() {
        var auth = loginAs("pbrchnk");
        controller.ping("extension", auth);

        var response = controller.offline("extension", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registry.secondsSinceSeen("pbrchnk")).doesNotContainKey("extension");
    }

    @Test
    void offlineFromUnknownClientIsRejected() {
        assertThat(controller.offline("toaster", loginAs("pbrchnk")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusShowsSessionAndWhichClientsCheckedIn() {
        var auth = loginAs("pbrchnk");
        controller.ping("extension", auth);

        Map<String, Object> status = controller.status();

        assertThat(status).containsEntry("session", true);
        Map<String, Object> clients = (Map<String, Object>) status.get("clients");
        assertThat(clients.get("extension")).isNotNull();
        assertThat(clients).containsEntry("cli", null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusWithoutLoginIsInvalidAndHidesClients() {
        registry.record("pbrchnk", "cli");

        Map<String, Object> status = controller.status();

        assertThat(status).containsEntry("session", false);
        assertThat(((Map<String, Object>) status.get("clients")).values()).containsOnlyNulls();
    }

    @Test
    void anonymousIsNotASession() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(controller.status()).containsEntry("session", false);
    }
}
