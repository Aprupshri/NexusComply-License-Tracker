package com.prodapt.license_tracker_backend.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokeAssignmentRequest {
    private String revokedBy;
    private String revocationReason;
}
