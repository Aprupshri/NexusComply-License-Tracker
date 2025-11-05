package com.prodapt.license_tracker_backend.controller;


import com.prodapt.license_tracker_backend.dto.SoftwareVersionRequest;
import com.prodapt.license_tracker_backend.dto.SoftwareVersionResponse;
import com.prodapt.license_tracker_backend.dto.SoftwareVersionStatsResponse;
import com.prodapt.license_tracker_backend.service.SoftwareVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/software-versions")
@RequiredArgsConstructor
@Tag(name = "Software Versions", description = "Software version tracking APIs")
@SecurityRequirement(name = "bearerAuth")
public class SoftwareVersionController {

    private final SoftwareVersionService softwareVersionService;

    @Operation(summary = "Get all software versions", description = "Retrieve paginated list of software versions")
    @GetMapping
    public ResponseEntity<Page<SoftwareVersionResponse>> getAllSoftwareVersions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastChecked") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<SoftwareVersionResponse> versions = softwareVersionService.getAllSoftwareVersions(pageable);
        return ResponseEntity.ok(versions);
    }

    @Operation(summary = "Get software version by ID", description = "Retrieve specific software version details")
    @GetMapping("/{id}")
    public ResponseEntity<SoftwareVersionResponse> getSoftwareVersionById(@PathVariable Long id) {
        SoftwareVersionResponse version = softwareVersionService.getSoftwareVersionById(id);
        return ResponseEntity.ok(version);
    }

    @Operation(summary = "Get software versions by device", description = "Retrieve all software versions for a device")
    @GetMapping("/by-device/{deviceId}")
    public ResponseEntity<List<SoftwareVersionResponse>> getSoftwareVersionsByDevice(@PathVariable Long deviceId) {
        List<SoftwareVersionResponse> versions = softwareVersionService.getSoftwareVersionsByDevice(deviceId);
        return ResponseEntity.ok(versions);
    }

    @Operation(summary = "Get software versions by status", description = "Filter software versions by status")
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<SoftwareVersionResponse>> getSoftwareVersionsByStatus(@PathVariable String status) {
        List<SoftwareVersionResponse> versions = softwareVersionService.getSoftwareVersionsByStatus(status);
        return ResponseEntity.ok(versions);
    }

    @Operation(summary = "Get software version statistics", description = "Get counts by status")
    @GetMapping("/statistics")
    public ResponseEntity<SoftwareVersionStatsResponse> getStatistics() {
        SoftwareVersionStatsResponse stats = softwareVersionService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Create software version", description = "Add new software version tracking")
    @PostMapping
    public ResponseEntity<SoftwareVersionResponse> createSoftwareVersion(@RequestBody SoftwareVersionRequest request) {
        SoftwareVersionResponse version = softwareVersionService.createSoftwareVersion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    @Operation(summary = "Update software version", description = "Update existing software version")
    @PutMapping("/{id}")
    public ResponseEntity<SoftwareVersionResponse> updateSoftwareVersion(
            @PathVariable Long id,
            @RequestBody SoftwareVersionRequest request) {
        SoftwareVersionResponse version = softwareVersionService.updateSoftwareVersion(id, request);
        return ResponseEntity.ok(version);
    }

    @Operation(summary = "Check for updates", description = "Manually check for software updates")
    @PostMapping("/{id}/check-updates")
    public ResponseEntity<SoftwareVersionResponse> checkForUpdates(@PathVariable Long id) {
        SoftwareVersionResponse version = softwareVersionService.checkForUpdates(id);
        return ResponseEntity.ok(version);
    }

    @Operation(summary = "Delete software version", description = "Remove software version tracking")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSoftwareVersion(@PathVariable Long id) {
        softwareVersionService.deleteSoftwareVersion(id);
        return ResponseEntity.noContent().build();
    }
}
