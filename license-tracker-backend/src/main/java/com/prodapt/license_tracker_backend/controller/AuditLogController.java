package com.prodapt.license_tracker_backend.controller;

import com.prodapt.license_tracker_backend.dto.AuditLogResponse;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Logs", description = "Audit log management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "Get all audit logs", description = "Retrieve paginated list of all audit logs")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<AuditLogResponse> logs = auditLogService.getAllAuditLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get audit log by ID", description = "Retrieve specific audit log details")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR')")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable Long id) {
        AuditLogResponse log = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(log);
    }

    @Operation(summary = "Get audit logs by user", description = "Retrieve audit logs for specific user")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditLogService.getAuditLogsByUser(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get audit logs by entity", description = "Retrieve audit logs for specific entity")
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR', 'NETWORK_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditLogService.getAuditLogsByEntity(
                EntityType.valueOf(entityType), entityId, pageable);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Filter audit logs", description = "Filter audit logs by multiple criteria")
    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> filterAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        EntityType entityTypeEnum = entityType != null ? EntityType.valueOf(entityType) : null;
        AuditAction actionEnum = action != null ? AuditAction.valueOf(action) : null;

        Page<AuditLogResponse> logs = auditLogService.getAuditLogsByFilters(
                entityTypeEnum, actionEnum, userId, startDate, endDate, pageable);

        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Search audit logs", description = "Search audit logs by keyword")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditLogService.searchAuditLogs(searchTerm, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * NEW: Search by license key
     */
    @Operation(summary = "Search by license key", description = "Find all audit logs related to a specific license key")
    @GetMapping("/search/license-key")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> searchByLicenseKey(
            @RequestParam String licenseKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching audit logs by license key: {}", licenseKey);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditLogService.searchByLicenseKey(licenseKey, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * NEW: Search by device ID
     */
    @Operation(summary = "Search by device ID", description = "Find all audit logs related to a specific device")
    @GetMapping("/search/device-id")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR', 'NETWORK_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> searchByDeviceId(
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching audit logs by device ID: {}", deviceId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditLogService.searchByDeviceId(deviceId, pageable);
        return ResponseEntity.ok(logs);
    }


    @Operation(summary = "Advanced search", description = "Search audit logs with multiple advanced filters")
    @GetMapping("/search/advanced")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY_HEAD', 'COMPLIANCE_OFFICER', 'IT_AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> advancedSearch(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String licenseKey,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Advanced search - entityType: {}, action: {}, licenseKey: {}, deviceId: {}",
                entityType, action, licenseKey, deviceId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLogResponse> logs = auditLogService.advancedSearch(
                entityType, action, username, licenseKey, deviceId, startDate, endDate, pageable);

        return ResponseEntity.ok(logs);
    }
}
