package com.prodapt.license_tracker_backend.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceReportResponse {
    private String region;
    private Integer totalDevices;
    private Integer devicesWithLicenses;
    private Integer devicesWithoutLicenses;
    private Integer totalLicenses;
    private Integer activeLicenses;
    private Integer expiringLicenses;
    private Integer expiredLicenses;
    private Double compliancePercentage;
}
