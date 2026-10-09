package com.todolist.shared.web;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body for every REST endpoint.
 *
 * @param timestamp instant the failure was handled
 * @param status HTTP status code
 * @param error application error code
 * @param message user facing message, free of internal detail
 * @param path request path that failed
 * @param fieldErrors field name to validation message, empty when not a validation failure
 */
public record ApiErrorResponse(Instant timestamp, int status, String error, String message,
    String path, Map<String, String> fieldErrors) {
}
