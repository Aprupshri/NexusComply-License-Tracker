package com.prodapt.license_tracker_backend.dto;

import com.prodapt.license_tracker_backend.entities.enums.LicenseType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseRequest {
    private String licenseKey;
    private String softwareName;
    private LicenseType licenseType;
    private Integer maxUsage;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Region region;
    private String poNumber;
    private BigDecimal cost;
    private Long vendorId;
    private String description;  // NEW FIELD
}
