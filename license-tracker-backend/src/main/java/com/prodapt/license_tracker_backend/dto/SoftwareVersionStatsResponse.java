package com.prodapt.license_tracker_backend.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftwareVersionStatsResponse {
    private long total;
    private long upToDate;
    private long outdated;
    private long critical;
    private long unknown;
}

