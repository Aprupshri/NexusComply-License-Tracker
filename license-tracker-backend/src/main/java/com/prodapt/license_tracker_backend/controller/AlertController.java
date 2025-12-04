package com.prodapt.license_tracker_backend.controller;

import com.prodapt.license_tracker_backend.dto.AcknowledgeAlertRequest;
import com.prodapt.license_tracker_backend.dto.AlertResponse;
import com.prodapt.license_tracker_backend.dto.AlertStatsResponse;
import com.prodapt.license_tracker_backend.entities.enums.AlertType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.entities.enums.Severity;
import com.prodapt.license_tracker_backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;
    private static final String MESSAGE="message";


    @Operation(summary = "Get alerts expiring in X days", description = "Filter alerts by days until expiry")
    @GetMapping(params = "days")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER')")
    public ResponseEntity<List<AlertResponse>> getAlertsExpiringInDays(@RequestParam int days) {
        List<AlertResponse> alerts = alertService.getAlertsExpiringInDays(days);
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get all alerts", description = "Retrieve paginated list of alerts")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER', 'IT_AUDITOR', 'SECURITY_HEAD')")
    public ResponseEntity<Page<AlertResponse>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "generatedAt") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<AlertResponse> alerts = alertService.getAllAlerts(pageable);
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get alert by ID", description = "Retrieve specific alert details")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER')")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable Long id) {
        AlertResponse alert = alertService.getAlertById(id);
        return ResponseEntity.ok(alert);
    }

    @Operation(summary = "Get unacknowledged alerts", description = "Retrieve all unacknowledged alerts")
    @GetMapping("/unacknowledged")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER')")
    public ResponseEntity<List<AlertResponse>> getUnacknowledgedAlerts() {
        List<AlertResponse> alerts = alertService.getUnacknowledgedAlerts();
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get alerts by severity", description = "Filter alerts by severity level")
    @GetMapping("/by-severity/{severity}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER')")
    public ResponseEntity<List<AlertResponse>> getAlertsBySeverity(@PathVariable String severity) {
        List<AlertResponse> alerts = alertService.getAlertsBySeverity(Severity.valueOf(severity));
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get alerts by type", description = "Filter alerts by alert type")
    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER')")
    public ResponseEntity<List<AlertResponse>> getAlertsByType(@PathVariable String type) {
        List<AlertResponse> alerts = alertService.getAlertsByType(AlertType.valueOf(type));
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get alerts by region", description = "Filter alerts by region")
    @GetMapping("/by-region/{region}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER')")
    public ResponseEntity<List<AlertResponse>> getAlertsByRegion(@PathVariable String region) {
        List<AlertResponse> alerts = alertService.getAlertsByRegion(Region.valueOf(region));
        return ResponseEntity.ok(alerts);
    }

    @Operation(summary = "Get alert statistics", description = "Get alert counts by severity")
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertStatsResponse> getStatistics() {
        AlertStatsResponse stats = alertService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Acknowledge alert", description = "Mark alert as acknowledged")
    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER')")
    public ResponseEntity<AlertResponse> acknowledgeAlert(
            @PathVariable Long id,
            @RequestBody AcknowledgeAlertRequest request) {
        AlertResponse alert = alertService.acknowledgeAlert(id, request.getAcknowledgedBy());
        return ResponseEntity.ok(alert);
    }

    @Operation(summary = "Acknowledge all alerts", description = "Mark all alerts as acknowledged")
    @PutMapping("/acknowledge-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'NETWORK_ADMIN', 'OPERATIONS_MANAGER')")
    public ResponseEntity<Map<String, String>> acknowledgeAllAlerts(@RequestBody AcknowledgeAlertRequest request) {
        alertService.acknowledgeAllAlerts(request.getAcknowledgedBy());
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, "All alerts acknowledged successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trigger license expiry check", description = "Manually trigger license expiry alerts")
    @PostMapping("/generate/license-expiry")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<Map<String, String>> generateLicenseExpiryAlerts() {
        alertService.generateLicenseExpiryAlerts();
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, "License expiry alerts generated successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trigger software version check", description = "Manually trigger software version alerts")
    @PostMapping("/generate/software-version")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'NETWORK_ENGINEER')")
    public ResponseEntity<Map<String, String>> generateSoftwareVersionAlerts() {
        alertService.generateSoftwareVersionAlerts();
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, "Software version alerts generated successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trigger capacity check", description = "Manually trigger license capacity alerts")
    @PostMapping("/generate/capacity")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, String>> generateCapacityAlerts() {
        alertService.generateLicenseCapacityAlerts();
        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, "License capacity alerts generated successfully");
        return ResponseEntity.ok(response);
    }
}
