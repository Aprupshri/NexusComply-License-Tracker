package com.prodapt.license_tracker_backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentReportResponse {
    private Long assignmentId;
    private String licenseKey;
    private String softwareName;
    private String deviceId;
    private String deviceType;
    private String location;
    private String region;
    private LocalDateTime assignedOn;
    private String assignedBy;
    private Boolean active;
    private LocalDateTime revokedOn;
    private String revokedBy;
}
