package com.cognizant.vibe.synthetictesting.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String targetUrlOrIp;
    @Enumerated(EnumType.STRING)
    private TargetType type;
    private boolean enabled;

}
