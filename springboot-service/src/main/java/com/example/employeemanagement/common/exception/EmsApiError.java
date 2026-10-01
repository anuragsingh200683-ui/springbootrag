package com.example.employeemanagement.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error body returned by every EMS endpoint (module: com.example.employeemanagement).
 * Kept separate from com.example.aiapp.exception.ErrorResponse so the two modules stay
 * independently evolvable, even though the shape is intentionally similar.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmsApiError {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    /** Populated only for bean-validation failures: field name -> validation message. */
    private final Map<String, String> fieldErrors;
}
