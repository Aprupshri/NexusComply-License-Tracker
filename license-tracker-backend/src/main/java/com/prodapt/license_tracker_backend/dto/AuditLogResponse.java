package com.prodapt.license_tracker_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long logId;
    private Long userId;
    private String username;
    private String entityType;
    private String entityId;
    private String action;
    private LocalDateTime timestamp;
    private String details;
    private String ipAddress;
    private String userAgent;
}
