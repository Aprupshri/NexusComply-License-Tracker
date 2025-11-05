package com.prodapt.license_tracker_backend.repository;

import com.prodapt.license_tracker_backend.entities.AuditLog;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            EntityType entityType, String entityId, Pageable pageable);

    /**
     * Search audit logs with multiple filters
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR a.timestamp <= :endDate) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByFilters(
            @Param("entityType") EntityType entityType,
            @Param("action") AuditAction action,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Search audit logs by keyword in details JSON
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "LOWER(a.details) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.entityId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchAuditLogs(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Search audit logs by license key in details JSON
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "LOWER(a.details) LIKE LOWER(CONCAT('%', :licenseKey, '%')) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByLicenseKey(@Param("licenseKey") String licenseKey, Pageable pageable);

    /**
     * Search audit logs by device ID in details JSON
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "LOWER(a.details) LIKE LOWER(CONCAT('%', :deviceId, '%')) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * Advanced search with all filters including license key and device ID
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "(:licenseKey IS NULL OR LOWER(a.details) LIKE LOWER(CONCAT('%', :licenseKey, '%'))) AND " +
            "(:deviceId IS NULL OR LOWER(a.details) LIKE LOWER(CONCAT('%', :deviceId, '%'))) AND " +
            "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR a.timestamp <= :endDate) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchByAdvancedFilters(
            @Param("entityType") EntityType entityType,
            @Param("action") AuditAction action,
            @Param("username") String username,
            @Param("licenseKey") String licenseKey,
            @Param("deviceId") String deviceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
