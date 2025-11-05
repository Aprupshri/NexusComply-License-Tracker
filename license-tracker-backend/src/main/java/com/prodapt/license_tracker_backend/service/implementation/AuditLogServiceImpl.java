// src/main/java/com/prodapt/license_tracker_backend/service/implementation/AuditLogServiceImpl.java
package com.prodapt.license_tracker_backend.service.implementation;

import com.prodapt.license_tracker_backend.dto.AuditLogResponse;
import com.prodapt.license_tracker_backend.dto.CreateAuditLogRequest;
import com.prodapt.license_tracker_backend.entities.AuditLog;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.repository.AuditLogRepository;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional
    public void log(CreateAuditLogRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(request.getUserId())
                    .username(request.getUsername())
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .action(request.getAction())
                    .details(request.getDetails())
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit log created: {} {} by user {}",
                    request.getAction(), request.getEntityType(), request.getUsername());
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    @Override
    @Async
    @Transactional
    public void log(Long userId, String username, EntityType entityType,
                    String entityId, AuditAction action, String details) {
        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .userId(userId)
                .username(username)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .details(details)
                .build();

        log(request);
    }

    @Override
    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        log.info("Fetching all audit logs");
        return auditLogRepository.findByOrderByTimestampDesc(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByUser(Long userId, Pageable pageable) {
        log.info("Fetching audit logs for user: {}", userId);
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByEntity(EntityType entityType, String entityId, Pageable pageable) {
        log.info("Fetching audit logs for entity: {} - {}", entityType, entityId);
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                        entityType, entityId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByFilters(
            EntityType entityType,
            AuditAction action,
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.info("Fetching audit logs with filters");
        return auditLogRepository.findByFilters(
                        entityType, action, userId, startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> searchAuditLogs(String searchTerm, Pageable pageable) {
        log.info("Searching audit logs with term: {}", searchTerm);
        return auditLogRepository.searchAuditLogs(searchTerm, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> searchByLicenseKey(String licenseKey, Pageable pageable) {
        log.info("Searching audit logs by license key: {}", licenseKey);
        return auditLogRepository.findByLicenseKey(licenseKey, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> searchByDeviceId(String deviceId, Pageable pageable) {
        log.info("Searching audit logs by device ID: {}", deviceId);
        return auditLogRepository.findByDeviceId(deviceId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> advancedSearch(
            String entityType,
            String action,
            String username,
            String licenseKey,
            String deviceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.info("Advanced search - entityType: {}, action: {}, licenseKey: {}, deviceId: {}",
                entityType, action, licenseKey, deviceId);

        EntityType entityTypeEnum = entityType != null ? EntityType.valueOf(entityType.toUpperCase()) : null;
        AuditAction actionEnum = action != null ? AuditAction.valueOf(action.toUpperCase()) : null;

        return auditLogRepository.searchByAdvancedFilters(
                        entityTypeEnum, actionEnum, username, licenseKey, deviceId, startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public AuditLogResponse getAuditLogById(Long logId) {
        log.info("Fetching audit log with ID: {}", logId);
        AuditLog auditLog = auditLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + logId));
        return mapToResponse(auditLog);
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .logId(auditLog.getLogId())
                .userId(auditLog.getUserId())
                .username(auditLog.getUsername())
                .entityType(auditLog.getEntityType() != null ? auditLog.getEntityType().name() : null)
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction() != null ? auditLog.getAction().name() : null)
                .timestamp(auditLog.getTimestamp())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .build();
    }
}
