package com.prodapt.license_tracker_backend.controller;


import com.prodapt.license_tracker_backend.dto.*;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation and export APIs")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Get license report", description = "Generate license report with filters")
    @GetMapping("/licenses")
    public ResponseEntity<List<LicenseReportResponse>> getLicenseReport(
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String software,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status) {

        List<LicenseReportResponse> report = reportService.getLicenseReport(vendor, software, region, status);
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Get device report", description = "Generate device report with filters")
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceReportResponse>> getDeviceReport(
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String lifecycle) {

        List<DeviceReportResponse> report = reportService.getDeviceReport(deviceType, region, lifecycle);
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Get assignment report", description = "Generate assignment report with filters")
    @GetMapping("/assignments")
    public ResponseEntity<List<AssignmentReportResponse>> getAssignmentReport(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Boolean active) {

        List<AssignmentReportResponse> report = reportService.getAssignmentReport(region, active);
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Get compliance report", description = "Generate compliance report for all regions")
    @GetMapping("/compliance")
    public ResponseEntity<List<ComplianceReportResponse>> getComplianceReport() {
        List<ComplianceReportResponse> report = reportService.getComplianceReport();
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Get compliance report by region", description = "Generate compliance report for specific region")
    @GetMapping("/compliance/{region}")
    public ResponseEntity<ComplianceReportResponse> getComplianceReportByRegion(@PathVariable String region) {
        ComplianceReportResponse report = reportService.getComplianceReportByRegion(Region.valueOf(region));
        return ResponseEntity.ok(report);
    }
}
