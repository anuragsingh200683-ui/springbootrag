package com.example.employeemanagement.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception -> HTTP response translation for the whole
 * com.example.employeemanagement (EMS) module. Scoped via basePackages so it
 * only applies to EMS controllers and never interferes with the existing
 * com.example.aiapp.exception.GlobalExceptionHandler.
 *
 * Named EmsGlobalExceptionHandler (not just GlobalExceptionHandler) deliberately:
 * Spring derives a bean's default name from its simple class name alone, ignoring
 * package, so a same-named class here would collide with
 * com.example.aiapp.exception.GlobalExceptionHandler and fail context startup with
 * a ConflictingBeanDefinitionException (hit this exact error - see project memory).
 * Tried fixing it via @RestControllerAdvice(value = "...", basePackages = "...")
 * first, but value() and basePackages() are @AliasFor-mirrored to each other (not
 * a bean-name attribute at all), so setting them to different strings throws an
 * AnnotationConfigurationException at startup instead. Renaming the class is the
 * correct fix.
 *
 * @Order(HIGHEST_PRECEDENCE) makes sure this advice is evaluated before the
 * unscoped aiapp advice for any controller within the employeemanagement package.
 */
@RestControllerAdvice(basePackages = "com.example.employeemanagement")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmsGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EmsGlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<EmsApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("EMS resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<EmsApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        log.warn("EMS duplicate resource: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<EmsApiError> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        log.warn("EMS invalid request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<EmsApiError> handleAiServiceError(AiServiceException ex, HttpServletRequest req) {
        log.error("EMS AI service call failed", ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<EmsApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        log.warn("EMS validation failed: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EmsApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled EMS exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", req, null);
    }

    private ResponseEntity<EmsApiError> build(HttpStatus status, String message, HttpServletRequest req,
                                               Map<String, String> fieldErrors) {
        EmsApiError body = EmsApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
