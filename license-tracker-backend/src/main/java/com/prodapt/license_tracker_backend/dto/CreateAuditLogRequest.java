package com.prodapt.license_tracker_backend.dto;

import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuditLogRequest {
    private Long userId;
    private String username;
    private EntityType entityType;
    private String entityId;
    private AuditAction action;
    private String details;
    private String ipAddress;
    private String userAgent;
}
