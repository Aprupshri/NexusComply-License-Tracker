package com.prodapt.license_tracker_backend.service;


import com.prodapt.license_tracker_backend.dto.AlertResponse;
import com.prodapt.license_tracker_backend.dto.AlertStatsResponse;
import com.prodapt.license_tracker_backend.entities.enums.AlertType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.entities.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AlertService {
    AlertResponse getAlertById(Long id);
    Page<AlertResponse> getAllAlerts(Pageable pageable);
    List<AlertResponse> getUnacknowledgedAlerts();
    List<AlertResponse> getAlertsBySeverity(Severity severity);
    List<AlertResponse> getAlertsByType(AlertType alertType);
    List<AlertResponse> getAlertsByRegion(Region region);
    AlertResponse acknowledgeAlert(Long id, String acknowledgedBy);
    void acknowledgeAllAlerts(String acknowledgedBy);
    AlertStatsResponse getStatistics();
    void generateLicenseExpiryAlerts();
    void generateSoftwareVersionAlerts();
    void generateLicenseCapacityAlerts();

    List<AlertResponse> getAlertsExpiringInDays(int days);
}
