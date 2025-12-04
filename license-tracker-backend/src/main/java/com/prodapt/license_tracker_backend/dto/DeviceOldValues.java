package com.prodapt.license_tracker_backend.dto;

import com.prodapt.license_tracker_backend.entities.enums.DeviceLifecycle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DeviceOldValues {
    private DeviceLifecycle lifecycle;
    private String model;
    private String ipAddress;
    private String location;
    private String softwareName;
    private String softwareVersion;
}