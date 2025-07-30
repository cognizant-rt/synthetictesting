package com.cognizant.vibe.synthetictesting.app.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * DTO for creating a new AppTarget.
 * Using a record for an immutable data carrier.
 */
public record CreateAppTargetRequest(
        @NotBlank(message = "Name cannot be blank")
        String name,

        @NotBlank(message = "Target URL or IP cannot be blank")
        String targetUrlOrIp,

        @NotNull(message = "Type cannot be null")
        TargetType type,

        boolean enabled
) {}
