package com.prodapt.license_tracker_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequest {
    private Long deviceId;
    private Long licenseId;
    private String assignedBy;  // Username or ID of person assigning
}
