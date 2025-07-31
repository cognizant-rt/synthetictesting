package com.cognizant.vibe.synthetictesting.check.dto;

import java.time.Instant;

/**
 * A Data Transfer Object representing a single check result.
 * This is used to control the data exposed via the API.
 */
public record CheckResultDto(
        Long id,
        Instant timestamp,
        boolean success,
        long responseTimeMs,
        Integer statusCode,
        String errorMessage
) {}