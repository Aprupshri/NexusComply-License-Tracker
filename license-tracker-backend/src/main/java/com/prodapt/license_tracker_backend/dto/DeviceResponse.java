package com.prodapt.license_tracker_backend.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private Long id;
    private String deviceId;
    private String deviceType;
    private String model;
    private String ipAddress;
    private String location;
    private String region;
    private String lifecycle;
    private String softwareName;
    private String softwareVersion;
    private String vendorName;
    private LocalDate purchasedDate;
}
