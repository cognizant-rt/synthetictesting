package com.cognizant.vibe.synthetictesting.check.entity;

import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckCommand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_target_id", nullable = false)
    private AppTarget app;

    @Enumerated(EnumType.STRING)
    private CommandType type; // GET, PING, TCP_PORT

    private String parameters; // optional headers, port

    private long intervalSeconds;
}