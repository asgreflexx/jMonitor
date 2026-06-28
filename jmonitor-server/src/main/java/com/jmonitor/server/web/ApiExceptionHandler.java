package com.jmonitor.server.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.Map;

/**
 * Centralises API error mapping so individual controllers don't each repeat it
 * (Phase 4 review). A failure to reach/attach a target JVM surfaces as an
 * {@link IOException} from the service layer and maps to 502 Bad Gateway;
 * invalid client input maps to 400.
 *
 * <p>{@code ResponseStatusException}s thrown explicitly by controllers (e.g. a
 * 404 for an unknown heap dump) are handled by Spring's own machinery and pass
 * through unchanged.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleTargetIo(IOException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
