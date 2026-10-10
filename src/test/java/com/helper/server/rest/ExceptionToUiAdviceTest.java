package com.helper.server.rest;

import com.helper.server.websocket.WSHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ExceptionToUiAdviceTest {

    private final WSHandler ws = mock(WSHandler.class);
    private final ExceptionToUiAdvice advice = new ExceptionToUiAdvice(ws);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void sendsReadableErrorToTheLoggedInUsersUi() {
        loginAs("pbrchnk");

        var response = advice.failed(new RuntimeException(new java.io.IOException(
                "GPT transcribe error 429: {\"error\":{\"message\":\"You have no credits remaining.\"}}")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(ws).broadcastToUser(org.mockito.ArgumentMatchers.eq("pbrchnk"), sent.capture());
        assertThat(sent.getValue()).contains("⚠️").contains("You have no credits remaining.");
    }

    @Test
    void doesNotNotifyAnonymousCallers() {
        advice.failed(new RuntimeException("boom"));

        verify(ws, never()).broadcastToUser(anyString(), anyString());
    }

    @Test
    void failureToDeliverNeverMasksTheOriginalError() {
        loginAs("pbrchnk");
        doThrow(new NullPointerException("no websocket session")).when(ws).broadcastToUser(anyString(), anyString());

        var response = advice.failed(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void securityExceptionsAreLeftToSpringSecurity() {
        loginAs("pbrchnk");

        assertThatThrownBy(() -> advice.failed(new AccessDeniedException("no")))
                .isInstanceOf(AccessDeniedException.class);
        verify(ws, never()).broadcastToUser(anyString(), anyString());
    }
}
