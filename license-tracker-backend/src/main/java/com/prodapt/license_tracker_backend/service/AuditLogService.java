// src/main/java/com/prodapt/license_tracker_backend/service/AuditLogService.java
package com.prodapt.license_tracker_backend.service;

import com.prodapt.license_tracker_backend.dto.AuditLogResponse;
import com.prodapt.license_tracker_backend.dto.CreateAuditLogRequest;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    void log(CreateAuditLogRequest request);

    void log(Long userId, String username, EntityType entityType,
             String entityId, AuditAction action, String details);

    Page<AuditLogResponse> getAllAuditLogs(Pageable pageable);

    Page<AuditLogResponse> getAuditLogsByUser(Long userId, Pageable pageable);

    Page<AuditLogResponse> getAuditLogsByEntity(EntityType entityType, String entityId, Pageable pageable);

    Page<AuditLogResponse> getAuditLogsByFilters(
            EntityType entityType,
            AuditAction action,
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    Page<AuditLogResponse> searchAuditLogs(String searchTerm, Pageable pageable);

    AuditLogResponse getAuditLogById(Long logId);

    /**
     * NEW: Search by license key
     */
    Page<AuditLogResponse> searchByLicenseKey(String licenseKey, Pageable pageable);

    /**
     * NEW: Search by device ID
     */
    Page<AuditLogResponse> searchByDeviceId(String deviceId, Pageable pageable);

    /**
     * NEW: Advanced search with all filters
     */
    Page<AuditLogResponse> advancedSearch(
            String entityType,
            String action,
            String username,
            String licenseKey,
            String deviceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}
