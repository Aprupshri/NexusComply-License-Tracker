package com.prodapt.license_tracker_backend.dto;


import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftwareVersionResponse {
    private Long id;
    private Long deviceId;
    private String deviceIdName;
    private String deviceType;
    private String softwareName;
    private String currentVersion;
    private String latestVersion;
    private String status;
    private LocalDate lastChecked;
    private String notes;
    private String updateUrl;
    private LocalDate releaseDate;
    private Boolean updateRecommended;
    private String updateMessage;
}
