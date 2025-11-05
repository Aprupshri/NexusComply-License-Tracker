package com.prodapt.license_tracker_backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseResponse {
    private Long id;
    private String licenseKey;
    private String softwareName;
    private String licenseType;
    private Integer maxUsage;
    private Integer currentUsage;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String region;
    private String poNumber;
    private BigDecimal cost;
    private Boolean active;
    private Long vendorId;
    private String vendorName;
    private String description;  // NEW FIELD
}
