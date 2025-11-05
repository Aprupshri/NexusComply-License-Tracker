package com.prodapt.license_tracker_backend.controller;

import com.prodapt.license_tracker_backend.dto.DashboardStatsResponse;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics APIs")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard statistics", description = "Retrieve overall dashboard statistics")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get dashboard statistics by region", description = "Retrieve dashboard statistics for specific region")
    @GetMapping("/stats/region/{region}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardStatsResponse> getDashboardStatsByRegion(@PathVariable String region) {
        DashboardStatsResponse stats = dashboardService.getDashboardStatsByRegion(Region.valueOf(region));
        return ResponseEntity.ok(stats);
    }
}
