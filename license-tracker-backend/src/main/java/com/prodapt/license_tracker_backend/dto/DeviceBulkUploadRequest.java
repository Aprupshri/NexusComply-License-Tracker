package com.prodapt.license_tracker_backend.dto;


import com.prodapt.license_tracker_backend.entities.enums.DeviceLifecycle;
import com.prodapt.license_tracker_backend.entities.enums.DeviceType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceBulkUploadRequest {
    private String deviceId;
    private DeviceType deviceType;
    private String model;
    private String ipAddress;
    private String location;
    private Region region;
    private DeviceLifecycle lifecycle;
    private String softwareName;
    private String softwareVersion;
    private LocalDate purchasedDate;
    private Long vendorId;

    private int rowNumber;
}
