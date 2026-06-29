package com.example.midterm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized REST error handling. Messages are resolved from the i18n message
 * bundles using the {@link MessageSource} and the request's resolved
 * {@link Locale} (driven by the {@code Accept-Language} header).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("404 Not Found: {}", ex.getMessage());
        String message = msg("error.notfound", ex.getResourceName(), String.valueOf(ex.getResourceId()));
        return error(HttpStatus.NOT_FOUND, msg("error.notfound.title"), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fieldErrors.put(err.getField(), err.getDefaultMessage())
        );
        log.warn("400 Validation failed for {} field(s)", fieldErrors.size());

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, msg("error.validation.title"));
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Constraint violation on a method parameter — e.g. {@code @Min(1)} on a
     * {@code @PathVariable} when the controller is {@code @Validated}. Without this
     * handler the violation would fall through to the 500 catch-all; here it
     * correctly maps to 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("400 Constraint violation: {}", ex.getMessage());
        // ex.getMessage() prefixes the method/param path (e.g. "getUserById.id: ..."); the
        // response carries only the human-readable constraint message.
        String detail = ex.getConstraintViolations().stream()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .findFirst()
                .orElse(ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, msg("error.badrequest.title"), detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("400 Bad Request: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, msg("error.badrequest.title"), ex.getMessage());
    }

    /**
     * Type coercion failure on a path/query parameter (e.g. /api/users/not-a-number).
     * Returns 400 rather than letting it fall through to the 500 catch-all.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("400 Bad Request: parameter '{}' has an invalid value '{}'", ex.getName(), ex.getValue());
        return error(HttpStatus.BAD_REQUEST, msg("error.badrequest.title"),
                msg("error.badrequest.param", ex.getName()));
    }

    /**
     * Unmatched route / missing static resource — return 404 instead of letting
     * it fall through to the 500 catch-all.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        log.warn("404 Not Found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, msg("error.notfound.title"), msg("error.notfound.noendpoint"));
    }

    /**
     * Other Spring-raised errors that already carry an HTTP status (e.g.
     * {@code ResponseStatusException}). Preserve that status rather than masking
     * it as 500 in the catch-all below.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Map<String, Object>> handleErrorResponse(ErrorResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        log.warn("{} {}", status.value(), ex.getMessage());
        return error(status, HttpStatus.valueOf(status.value()).getReasonPhrase(), ex.getMessage());
    }

    /**
     * Catch-all for unexpected errors. Logs the full stack trace (fail visibly,
     * not silently) and returns a consistent 500 body without leaking internals.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("500 Internal Server Error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, msg("error.internal.title"), msg("error.internal"));
    }

    /** Resolve an i18n message for the current request locale. */
    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /** Shared error envelope (timestamp + status + error title); callers add the rest. */
    private Map<String, Object> baseBody(HttpStatusCode status, String errorTitle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", errorTitle);
        return body;
    }

    /** Build a complete single-message error response. */
    private ResponseEntity<Map<String, Object>> error(HttpStatusCode status, String errorTitle, String message) {
        Map<String, Object> body = baseBody(status, errorTitle);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
