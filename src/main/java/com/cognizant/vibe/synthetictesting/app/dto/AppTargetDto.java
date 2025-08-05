package com.cognizant.vibe.synthetictesting.app.dto;

import com.cognizant.vibe.synthetictesting.app.entity.TargetType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppTargetDto {
    private Long id;
    private String name;
    private String targetUrlOrIp;
    private TargetType type;
    private boolean enabled;
}