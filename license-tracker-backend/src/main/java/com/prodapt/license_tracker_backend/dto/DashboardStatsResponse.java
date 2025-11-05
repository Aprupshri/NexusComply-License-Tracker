package com.prodapt.license_tracker_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private Long totalDevices;
    private Long activeDevices;
    private Long totalLicenses;
    private Long activeLicenses;
    private Long expiringLicenses;
    private Long totalAssignments;
    private Long activeAssignments;
    private Long totalAlerts;
    private Long unacknowledgedAlerts;
    private Long criticalAlerts;
    private Long totalVendors;
    private Double averageLicenseUtilization;
    private Long devicesWithoutLicenses;
}
