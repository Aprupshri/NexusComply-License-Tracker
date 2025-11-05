package com.prodapt.license_tracker_backend.repository;

import com.prodapt.license_tracker_backend.entities.Alert;
import com.prodapt.license_tracker_backend.entities.enums.AlertType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.entities.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByAcknowledgedOrderByGeneratedAtDesc(Boolean acknowledged, Pageable pageable);

    List<Alert> findByAcknowledgedAndSeverityOrderByGeneratedAtDesc(Boolean acknowledged, Severity severity);

    List<Alert> findByAcknowledgedAndAlertTypeOrderByGeneratedAtDesc(Boolean acknowledged, AlertType alertType);

    List<Alert> findByRegionAndAcknowledgedOrderByGeneratedAtDesc(Region region, Boolean acknowledged);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.acknowledged = false AND a.severity = :severity")
    long countUnacknowledgedBySeverity(Severity severity);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.acknowledged = false")
    long countUnacknowledged();

    @Query("SELECT a FROM Alert a WHERE a.acknowledged = false AND a.generatedAt >= :since ORDER BY a.generatedAt DESC")
    List<Alert> findRecentUnacknowledged(LocalDateTime since);

    @Query("SELECT a FROM Alert a WHERE a.alertType = :alertType AND a.message LIKE %:keyword%")
    List<Alert> findByAlertTypeAndMessageContaining(AlertType alertType, String keyword);

    long countByRegionAndAcknowledgedFalse(Region region);
    long countByRegionAndSeverityAndAcknowledgedFalse(Region region, Severity severity);

}
