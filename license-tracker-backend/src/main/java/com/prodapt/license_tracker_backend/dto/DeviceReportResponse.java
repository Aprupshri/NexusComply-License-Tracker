package com.prodapt.license_tracker_backend.dto;



import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceReportResponse {
    private Long deviceId;
    private String deviceIdName;
    private String deviceType;
    private String model;
    private String ipAddress;
    private String location;
    private String region;
    private String lifecycle;
    private String softwareName;
    private String softwareVersion;
    private Integer assignedLicensesCount;
    private String purchasedDate;
}
