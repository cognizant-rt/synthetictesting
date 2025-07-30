package com.cognizant.vibe.synthetictesting.check.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_command_id", nullable = false)
    private CheckCommand command;

    private Instant timestamp;

    private boolean success;
    private long responseTimeMs;
    private Integer statusCode; // for HTTP, nullable
    private String errorMessage;
}
