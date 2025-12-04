package com.prodapt.license_tracker_backend.dto;

import com.prodapt.license_tracker_backend.entities.Device;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class DeviceChangeContext {
    private Device updatedDevice;
    private String username;
    private Long userId;
}