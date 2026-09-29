package com.loanservicing.common;

import java.time.Instant;
import java.util.Map;

/** The JSON body React receives for every error. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {
}
