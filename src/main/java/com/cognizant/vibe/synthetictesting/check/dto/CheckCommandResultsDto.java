package com.cognizant.vibe.synthetictesting.check.dto;

import com.cognizant.vibe.synthetictesting.check.entity.CommandType;

import java.util.List;

/**
 * A Data Transfer Object that groups check results by their parent command.
 */
public record CheckCommandResultsDto(
        Long commandId,
        CommandType commandType,
        String commandParameters,
        List<CheckResultDto> results
) {}