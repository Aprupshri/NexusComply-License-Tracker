package com.prodapt.license_tracker_backend.controller;


import com.prodapt.license_tracker_backend.dto.AssignmentRequest;
import com.prodapt.license_tracker_backend.dto.AssignmentResponse;
import com.prodapt.license_tracker_backend.dto.RevokeAssignmentRequest;
import com.prodapt.license_tracker_backend.service.LicenseAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Tag(name = "License Assignments", description = "License assignment management APIs")
@SecurityRequirement(name = "bearerAuth")
public class LicenseAssignmentController {

    private final LicenseAssignmentService assignmentService;

    @Operation(summary = "Assign license to device",
            description = "Create a new license assignment. Validates max usage before assignment.")
    @PostMapping
    public ResponseEntity<AssignmentResponse> assignLicense(@RequestBody AssignmentRequest request) {
        AssignmentResponse response = assignmentService.assignLicenseToDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Revoke license assignment",
            description = "Revoke license assignment from device with reason")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<AssignmentResponse> revokeAssignment(
            @PathVariable Long id,
            @RequestBody RevokeAssignmentRequest request) {
        AssignmentResponse response = assignmentService.revokeAssignment(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get active assignments by device",
            description = "Get all currently active assignments for a specific device")
    @GetMapping("/by-device/{deviceId}")
    public ResponseEntity<List<AssignmentResponse>> getActiveAssignmentsByDevice(@PathVariable Long deviceId) {
        List<AssignmentResponse> assignments = assignmentService.getActiveAssignmentsByDevice(deviceId);
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get all assignments by device",
            description = "Get all assignments (including revoked) for a specific device")
    @GetMapping("/by-device/{deviceId}/all")
    public ResponseEntity<List<AssignmentResponse>> getAllAssignmentsByDevice(@PathVariable Long deviceId) {
        List<AssignmentResponse> assignments = assignmentService.getAllAssignmentsByDevice(deviceId);
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get active assignments by license",
            description = "Get all currently active assignments for a specific license")
    @GetMapping("/by-license/{licenseId}")
    public ResponseEntity<List<AssignmentResponse>> getActiveAssignmentsByLicense(@PathVariable Long licenseId) {
        List<AssignmentResponse> assignments = assignmentService.getActiveAssignmentsByLicense(licenseId);
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get all assignments by license",
            description = "Get all assignments (including revoked) for a specific license")
    @GetMapping("/by-license/{licenseId}/all")
    public ResponseEntity<List<AssignmentResponse>> getAllAssignmentsByLicense(@PathVariable Long licenseId) {
        List<AssignmentResponse> assignments = assignmentService.getAllAssignmentsByLicense(licenseId);
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get all active assignments",
            description = "Get all currently active license assignments in the system")
    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAllActiveAssignments() {
        List<AssignmentResponse> assignments = assignmentService.getAllActiveAssignments();
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get assignment by ID",
            description = "Get specific assignment details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Long id) {
        AssignmentResponse assignment = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(assignment);
    }
}
