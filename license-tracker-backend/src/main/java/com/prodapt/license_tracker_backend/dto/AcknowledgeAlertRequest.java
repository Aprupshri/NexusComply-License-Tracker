package com.prodapt.license_tracker_backend.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcknowledgeAlertRequest {
    private String acknowledgedBy;
}
