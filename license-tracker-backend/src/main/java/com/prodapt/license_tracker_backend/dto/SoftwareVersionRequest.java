package com.prodapt.license_tracker_backend.dto;


import com.prodapt.license_tracker_backend.entities.enums.SoftwareVersionStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftwareVersionRequest {
    private Long deviceId;
    private String softwareName;
    private String currentVersion;
    private String latestVersion;
    private SoftwareVersionStatus status;
    private LocalDate lastChecked;
    private String notes;
    private String updateUrl;
    private LocalDate releaseDate;
}
