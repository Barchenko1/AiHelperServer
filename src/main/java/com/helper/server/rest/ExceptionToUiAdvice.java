package com.helper.server.rest;

import com.helper.server.util.UiErrors;
import com.helper.server.websocket.WSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Any failure while handling a request is pushed to the user's UI so they see an error
 * instead of waiting on "Processing..." forever.
 */
@RestControllerAdvice
public class ExceptionToUiAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionToUiAdvice.class);

    private final WSHandler wsHandler;

    public ExceptionToUiAdvice(WSHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Void> tooLarge(MaxUploadSizeExceededException e) {
        notifyUser(UiErrors.toUiMessage("Upload too large (limit " + (e.getMaxUploadSize() / (1024 * 1024)) + "MB)"));
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Void> failed(RuntimeException e) {
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw e;
        }
        LOGGER.error("Request failed", e);
        notifyUser(UiErrors.toUiMessage(e));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    private void notifyUser(String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;
        try {
            wsHandler.broadcastToUser(auth.getName(), message);
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not deliver error to UI: {}", ex.toString());
        }
    }
}
