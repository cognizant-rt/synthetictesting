package com.cognizant.vibe.synthetictesting.check.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new CheckCommand for a specific AppTarget.
 */
public record CreateCheckCommandRequest(
        @NotNull(message = "Command type cannot be null")
        CommandType type,

        String parameters,

        @Min(value = 5, message = "Interval must be at least 5 seconds")
        long intervalSeconds
) {}
