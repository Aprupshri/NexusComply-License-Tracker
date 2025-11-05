package com.prodapt.license_tracker_backend.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {
    private Long id;

    // Device info
    private Long deviceId;
    private String deviceIdName;
    private String deviceType;
    private String deviceLocation;

    // License info
    private Long licenseId;
    private String licenseKey;
    private String softwareName;

    // Assignment info
    private LocalDateTime assignedOn;
    private String assignedBy;
    private LocalDateTime revokedOn;
    private String revokedBy;
    private String revocationReason;
    private Boolean active;
}
