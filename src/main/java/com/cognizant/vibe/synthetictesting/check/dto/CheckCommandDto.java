package com.cognizant.vibe.synthetictesting.check.dto;

import com.cognizant.vibe.synthetictesting.check.entity.CommandType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckCommandDto {
    private Long id;
    private CommandType type;
    private String parameters;
    private long intervalSeconds;
}