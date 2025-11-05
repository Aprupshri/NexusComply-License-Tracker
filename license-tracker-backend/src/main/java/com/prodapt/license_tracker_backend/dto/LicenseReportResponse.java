package com.prodapt.license_tracker_backend.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseReportResponse {
    private Long licenseId;
    private String licenseKey;
    private String softwareName;
    private String licenseType;
    private Integer maxUsage;
    private Integer currentUsage;
    private Double usagePercentage;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer daysUntilExpiry;
    private String status; // ACTIVE, EXPIRING_SOON, EXPIRED, NEAR_CAPACITY
    private String region;
    private String vendorName;
    private String poNumber;
    private String cost;
}
