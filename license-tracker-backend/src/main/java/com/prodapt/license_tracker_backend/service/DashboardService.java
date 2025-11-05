package com.prodapt.license_tracker_backend.service;


import com.prodapt.license_tracker_backend.dto.DashboardStatsResponse;
import com.prodapt.license_tracker_backend.entities.enums.Region;

public interface DashboardService {
    DashboardStatsResponse getDashboardStats();
    DashboardStatsResponse getDashboardStatsByRegion(Region region);
}
