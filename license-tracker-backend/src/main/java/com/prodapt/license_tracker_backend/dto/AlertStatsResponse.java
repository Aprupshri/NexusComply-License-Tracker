package com.prodapt.license_tracker_backend.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertStatsResponse {
    private long total;
    private long unacknowledged;
    private long critical;
    private long high;
    private long medium;
    private long low;
}
