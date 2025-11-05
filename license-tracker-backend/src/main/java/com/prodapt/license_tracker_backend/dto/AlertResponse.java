package com.prodapt.license_tracker_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    private Long id;
    private String alertType;
    private String severity;
    private String message;
    private String region;
    private LocalDateTime generatedAt;
    private Boolean acknowledged;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private String icon;
    private String color;
}
