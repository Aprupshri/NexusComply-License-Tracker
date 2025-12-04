package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.dto.AlertResponse;
import com.prodapt.license_tracker_backend.dto.AlertStatsResponse;
import com.prodapt.license_tracker_backend.entities.Alert;
import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.SoftwareVersion;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.enums.*;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.repository.AlertRepository;
import com.prodapt.license_tracker_backend.repository.LicenseRepository;
import com.prodapt.license_tracker_backend.repository.SoftwareVersionRepository;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.service.AlertService;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final LicenseRepository licenseRepository;
    private final SoftwareVersionRepository softwareVersionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // Helper method to get current user info
    private Map<String, Object> getCurrentUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                userInfo.put("username", username);

                Long userId = fetchUserId(username);
                userInfo.put("userId", userId);
            }
        } catch (Exception e) {
            log.warn("Error getting current user info", e);
        }

        userInfo.putIfAbsent("username", "SYSTEM");
        userInfo.putIfAbsent("userId", null);

        return userInfo;
    }

    // Extracted method: Fetch user ID by username
    private Long fetchUserId(String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return user.getId();
            }
        } catch (Exception e) {
            log.debug("Could not fetch user ID for username: {}", username);
        }
        return null;
    }

    @Override
    public AlertResponse getAlertById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));
        return mapToResponse(alert);
    }

    @Override
    public Page<AlertResponse> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    public List<AlertResponse> getUnacknowledgedAlerts() {
        List<Alert> alerts = alertRepository.findRecentUnacknowledged(
                LocalDateTime.now().minusDays(90)
        );
        return alerts.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<AlertResponse> getAlertsBySeverity(Severity severity) {
        List<Alert> alerts = alertRepository.findByAcknowledgedAndSeverityOrderByGeneratedAtDesc(false, severity);
        return alerts.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<AlertResponse> getAlertsByType(AlertType alertType) {
        List<Alert> alerts = alertRepository.findByAcknowledgedAndAlertTypeOrderByGeneratedAtDesc(false, alertType);
        return alerts.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<AlertResponse> getAlertsByRegion(Region region) {
        List<Alert> alerts = alertRepository.findByRegionAndAcknowledgedOrderByGeneratedAtDesc(region, false);
        return alerts.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public AlertResponse acknowledgeAlert(Long id, String acknowledgedBy) {
        log.info("Acknowledging alert: {} by {}", id, acknowledgedBy);

        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));

        // Store old state for audit
        boolean wasAcknowledged = alert.getAcknowledged();

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(acknowledgedBy);

        Alert saved = alertRepository.save(alert);

        // Create audit log
        try {
            Map<String, Object> userInfo = getCurrentUserInfo();
            Long userId = (Long) userInfo.get("userId");
            String username = (String) userInfo.get("username");

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("alertId", saved.getId());
            auditDetails.put("alertType", saved.getAlertType().name());
            auditDetails.put("severity", saved.getSeverity().name());
            auditDetails.put("message", saved.getMessage());
            auditDetails.put("region", saved.getRegion() != null ? saved.getRegion().name() : null);
            auditDetails.put("acknowledgedBy", acknowledgedBy);
            auditDetails.put("acknowledgedAt", LocalDateTime.now().toString());
            auditDetails.put("wasAcknowledged", wasAcknowledged);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.ALERT,
                    saved.getId().toString(),
                    AuditAction.ACKNOWLEDGE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for alert acknowledgement", e);
        }

        log.info("Alert acknowledged successfully: {}", id);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void acknowledgeAllAlerts(String acknowledgedBy) {
        log.info("Acknowledging all alerts by: {}", acknowledgedBy);

        List<Alert> unacknowledged = alertRepository.findRecentUnacknowledged(
                LocalDateTime.now().minusDays(90)
        );

        int acknowledgedCount = unacknowledged.size();

        for (Alert alert : unacknowledged) {
            alert.setAcknowledged(true);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert.setAcknowledgedBy(acknowledgedBy);
        }

        alertRepository.saveAll(unacknowledged);

        // Create audit log for bulk acknowledgement
        try {
            Map<String, Object> userInfo = getCurrentUserInfo();
            Long userId = (Long) userInfo.get("userId");
            String username = (String) userInfo.get("username");

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "ACKNOWLEDGE_ALL");
            auditDetails.put("acknowledgedCount", acknowledgedCount);
            auditDetails.put("acknowledgedBy", acknowledgedBy);
            auditDetails.put("acknowledgedAt", LocalDateTime.now().toString());

            // Include severity breakdown
            Map<String, Long> severityBreakdown = unacknowledged.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getSeverity().name(),
                            Collectors.counting()
                    ));
            auditDetails.put("severityBreakdown", severityBreakdown);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.ALERT,
                    "BULK_ACKNOWLEDGE",
                    AuditAction.ACKNOWLEDGE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for bulk alert acknowledgement", e);
        }

        log.info("Acknowledged {} alerts", acknowledgedCount);
    }

    @Override
    public AlertStatsResponse getStatistics() {
        long total = alertRepository.count();
        long unacknowledged = alertRepository.countUnacknowledged();
        long critical = alertRepository.countUnacknowledgedBySeverity(Severity.CRITICAL);
        long high = alertRepository.countUnacknowledgedBySeverity(Severity.HIGH);
        long medium = alertRepository.countUnacknowledgedBySeverity(Severity.MEDIUM);
        long low = alertRepository.countUnacknowledgedBySeverity(Severity.LOW);

        return AlertStatsResponse.builder()
                .total(total)
                .unacknowledged(unacknowledged)
                .critical(critical)
                .high(high)
                .medium(medium)
                .low(low)
                .build();
    }

    @Override
    @Scheduled(cron = "0 0 9 * * *") // Run daily at 9 AM
    @Transactional
    public void generateLicenseExpiryAlerts() {
        log.info("🔍 Running scheduled license expiry check...");

        LocalDate today = LocalDate.now();
        LocalDate next90Days = today.plusDays(90);

        List<License> licenses = licenseRepository.findByActiveTrueAndValidToBetween(today, next90Days);

        int alertsGenerated = 0;
        List<String> generatedAlertIds = new ArrayList<>();

        for (License license : licenses) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(today, license.getValidTo());

            String alertKeyword = license.getLicenseKey();
            List<Alert> existingAlerts = alertRepository.findByAlertTypeAndMessageContaining(
                    AlertType.LICENSE_EXPIRING,
                    alertKeyword
            );

            boolean hasUnacknowledgedAlert = existingAlerts.stream()
                    .anyMatch(a -> !a.getAcknowledged() &&
                            a.getGeneratedAt().isAfter(LocalDateTime.now().minusDays(7)));

            if (!hasUnacknowledgedAlert) {
                Severity severity = determineSeverity(daysUntilExpiry);
                String message = generateExpiryMessage(license, daysUntilExpiry);

                Alert alert = Alert.builder()
                        .alertType(daysUntilExpiry <= 0 ? AlertType.LICENSE_EXPIRED : AlertType.LICENSE_EXPIRING)
                        .severity(severity)
                        .message(message)
                        .region(license.getRegion())
                        .acknowledged(false)
                        .build();

                Alert savedAlert = alertRepository.save(alert);
                alertsGenerated++;
                generatedAlertIds.add(savedAlert.getId().toString());
            }
        }

        // Create audit log for scheduled check
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("checkType", "LICENSE_EXPIRY_SCHEDULED");
            auditDetails.put("checkDate", today.toString());
            auditDetails.put("licensesChecked", licenses.size());
            auditDetails.put("alertsGenerated", alertsGenerated);
            auditDetails.put("generatedAlertIds", generatedAlertIds);
            auditDetails.put("dateRange", Map.of("from", today.toString(), "to", next90Days.toString()));

            auditLogService.log(
                    null,
                    "SYSTEM_SCHEDULER",
                    EntityType.ALERT,
                    "SCHEDULED_CHECK",
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for scheduled license expiry check", e);
        }

        log.info("✅ License expiry check completed. Generated {} new alerts", alertsGenerated);
    }

    @Override
    @Scheduled(cron = "0 0 10 * * *") // Run daily at 10 AM
    @Transactional
    public void generateSoftwareVersionAlerts() {
        log.info("🔍 Running scheduled software version check...");

        List<SoftwareVersion> outdatedVersions = softwareVersionRepository.findByStatus(SoftwareVersionStatus.OUTDATED);
        List<SoftwareVersion> criticalVersions = softwareVersionRepository.findByStatus(SoftwareVersionStatus.CRITICAL);

        int alertsGenerated = 0;
        List<String> generatedAlertIds = new ArrayList<>();

        for (SoftwareVersion version : criticalVersions) {
            String alertKeyword = version.getDevice().getDeviceId();
            List<Alert> existingAlerts = alertRepository.findByAlertTypeAndMessageContaining(
                    AlertType.SOFTWARE_VERSION_CRITICAL,
                    alertKeyword
            );

            boolean hasUnacknowledgedAlert = existingAlerts.stream()
                    .anyMatch(a -> !a.getAcknowledged() &&
                            a.getGeneratedAt().isAfter(LocalDateTime.now().minusDays(7)));

            if (!hasUnacknowledgedAlert) {
                String message = String.format(
                        "CRITICAL: Device %s running %s version %s is critically outdated. Latest version: %s",
                        version.getDevice().getDeviceId(),
                        version.getSoftwareName(),
                        version.getCurrentVersion(),
                        version.getLatestVersion()
                );

                Alert alert = Alert.builder()
                        .alertType(AlertType.SOFTWARE_VERSION_CRITICAL)
                        .severity(Severity.CRITICAL)
                        .message(message)
                        .region(version.getDevice().getRegion())
                        .acknowledged(false)
                        .build();

                Alert savedAlert = alertRepository.save(alert);
                alertsGenerated++;
                generatedAlertIds.add(savedAlert.getId().toString());
            }
        }

        // Create audit log for scheduled check
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("checkType", "SOFTWARE_VERSION_SCHEDULED");
            auditDetails.put("checkDate", LocalDate.now().toString());
            auditDetails.put("outdatedVersionsChecked", outdatedVersions.size());
            auditDetails.put("criticalVersionsChecked", criticalVersions.size());
            auditDetails.put("alertsGenerated", alertsGenerated);
            auditDetails.put("generatedAlertIds", generatedAlertIds);

            auditLogService.log(
                    null,
                    "SYSTEM_SCHEDULER",
                    EntityType.ALERT,
                    "SCHEDULED_CHECK",
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for scheduled software version check", e);
        }

        log.info("✅ Software version check completed. Generated {} new alerts", alertsGenerated);
    }

    @Override
    public List<AlertResponse> getAlertsExpiringInDays(int days) {
        log.info("Fetching alerts for licenses expiring in {} days", days);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);

        List<License> expiringLicenses = licenseRepository.findByActiveTrueAndValidToBetween(today, futureDate);

        List<Alert> alerts = new ArrayList<>();
        int newAlertsGenerated = 0;

        for (License license : expiringLicenses) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(today, license.getValidTo());
            Severity severity = determineSeverity(daysUntilExpiry);
            String message = generateExpiryMessage(license, daysUntilExpiry);

            String alertKeyword = license.getLicenseKey();
            List<Alert> existingAlerts = alertRepository.findByAlertTypeAndMessageContaining(
                    AlertType.LICENSE_EXPIRING,
                    alertKeyword
            );

            boolean hasRecentAlert = existingAlerts.stream()
                    .anyMatch(a -> a.getGeneratedAt().isAfter(LocalDateTime.now().minusDays(1)));

            if (!hasRecentAlert) {
                Alert alert = Alert.builder()
                        .alertType(AlertType.LICENSE_EXPIRING)
                        .severity(severity)
                        .message(message)
                        .region(license.getRegion())
                        .acknowledged(false)
                        .build();

                alerts.add(alertRepository.save(alert));
                newAlertsGenerated++;
            } else {
                alerts.addAll(existingAlerts.stream()
                        .filter(a -> !a.getAcknowledged())
                        .toList());
            }
        }

        // Create audit log for manual check
        if (newAlertsGenerated > 0) {
            try {
                Map<String, Object> userInfo = getCurrentUserInfo();
                Long userId = (Long) userInfo.get("userId");
                String username = (String) userInfo.get("username");

                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("checkType", "LICENSE_EXPIRY_MANUAL");
                auditDetails.put("daysParameter", days);
                auditDetails.put("licensesChecked", expiringLicenses.size());
                auditDetails.put("newAlertsGenerated", newAlertsGenerated);
                auditDetails.put("checkDate", LocalDate.now().toString());

                auditLogService.log(
                        userId,
                        username,
                        EntityType.ALERT,
                        "MANUAL_CHECK",
                        AuditAction.CREATE,
                        objectMapper.writeValueAsString(auditDetails)
                );
            } catch (Exception e) {
                log.error("Failed to create audit log for manual license expiry check", e);
            }
        }

        return alerts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Scheduled(cron = "0 0 11 * * *") // Run daily at 11 AM
    @Transactional
    public void generateLicenseCapacityAlerts() {
        log.info("🔍 Running scheduled license capacity check...");

        List<License> licenses = licenseRepository.findAll();
        List<String> generatedAlertIds = new ArrayList<>();

        for (License license : licenses) {
            processLicenseAlert(license, generatedAlertIds);
        }

        logAuditDetails(licenses.size(), generatedAlertIds);
        log.info("✅ License capacity check completed. Generated {} new alerts", generatedAlertIds.size());
    }

    // Extracted method: Process individual license alert
    private void processLicenseAlert(License license, List<String> generatedAlertIds) {
        if (license.getMaxUsage() <= 0) {
            return;
        }

        double usagePercentage = calculateUsagePercentage(license);

        if (usagePercentage >= 90) {
            processCapacityAlert(license, usagePercentage, AlertType.LICENSE_CAPACITY_CRITICAL,
                    Severity.CRITICAL, generatedAlertIds, 7);
        } else if (usagePercentage >= 80) {
            processCapacityAlert(license, usagePercentage, AlertType.LICENSE_CAPACITY_WARNING,
                    Severity.HIGH, generatedAlertIds, 7);
        }
    }

    // Extracted method: Calculate usage percentage
    private double calculateUsagePercentage(License license) {
        return (license.getCurrentUsage() * 100.0) / license.getMaxUsage();
    }

    // Extracted method: Process capacity alert with threshold
    private void processCapacityAlert(License license, double usagePercentage,
                                      AlertType alertType, Severity severity,
                                      List<String> generatedAlertIds, int daysThreshold) {
        String alertKeyword = license.getLicenseKey();
        List<Alert> existingAlerts = alertRepository.findByAlertTypeAndMessageContaining(alertType, alertKeyword);

        if (!hasUnacknowledgedAlert(existingAlerts, daysThreshold)) {
            String message = formatAlertMessage(license, usagePercentage, alertType);
            Alert alert = createAlert(alertType, severity, message, license);

            Alert savedAlert = alertRepository.save(alert);
            generatedAlertIds.add(savedAlert.getId().toString());
        }
    }

    // Extracted method: Check for unacknowledged alerts
    private boolean hasUnacknowledgedAlert(List<Alert> alerts, int daysThreshold) {
        return alerts.stream()
                .anyMatch(a -> !a.getAcknowledged() &&
                        a.getGeneratedAt().isAfter(LocalDateTime.now().minusDays(daysThreshold)));
    }

    // Extracted method: Format alert message based on type
    private String formatAlertMessage(License license, double usagePercentage, AlertType alertType) {
        String baseMessage = String.format(
                "License %s (%s) capacity at %.0f%% (%d/%d).",
                license.getLicenseKey(),
                license.getSoftwareName(),
                usagePercentage,
                license.getCurrentUsage(),
                license.getMaxUsage()
        );

        if (alertType == AlertType.LICENSE_CAPACITY_CRITICAL) {
            return baseMessage + " Immediate action required!";
        }
        return baseMessage + " Consider purchasing additional licenses.";
    }

    // Extracted method: Create alert entity
    private Alert createAlert(AlertType alertType, Severity severity, String message, License license) {
        return Alert.builder()
                .alertType(alertType)
                .severity(severity)
                .message(message)
                .region(license.getRegion())
                .acknowledged(false)
                .build();
    }

    // Extracted method: Log audit details
    private void logAuditDetails(int licensesChecked, List<String> generatedAlertIds) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("checkType", "LICENSE_CAPACITY_SCHEDULED");
            auditDetails.put("checkDate", LocalDate.now().toString());
            auditDetails.put("licensesChecked", licensesChecked);
            auditDetails.put("alertsGenerated", generatedAlertIds.size());
            auditDetails.put("generatedAlertIds", generatedAlertIds);

            List<License> allLicenses = licenseRepository.findAll();
            addCapacityStatistics(auditDetails, allLicenses);

            auditLogService.log(
                    null,
                    "SYSTEM_SCHEDULER",
                    EntityType.ALERT,
                    "SCHEDULED_CHECK",
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for scheduled license capacity check", e);
        }
    }

    // Extracted method: Add capacity statistics to audit
    private void addCapacityStatistics(Map<String, Object> auditDetails, List<License> licenses) {
        long criticalCapacity = licenses.stream()
                .filter(l -> calculateUsagePercentage(l) >= 90)
                .count();

        long warningCapacity = licenses.stream()
                .filter(l -> {
                    double usage = calculateUsagePercentage(l);
                    return usage >= 80 && usage < 90;
                })
                .count();

        auditDetails.put("criticalCapacityCount", criticalCapacity);
        auditDetails.put("warningCapacityCount", warningCapacity);
    }

    private Severity determineSeverity(long daysUntilExpiry) {
        if (daysUntilExpiry <= 0) {
            return Severity.CRITICAL;
        } else if (daysUntilExpiry <= 15) {
            return Severity.CRITICAL;
        } else if (daysUntilExpiry <= 30) {
            return Severity.HIGH;
        } else if (daysUntilExpiry <= 60) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }

    private String generateExpiryMessage(License license, long daysUntilExpiry) {
        if (daysUntilExpiry <= 0) {
            return String.format(
                    "EXPIRED: License %s (%s) expired %d days ago. Renewal required immediately!",
                    license.getLicenseKey(),
                    license.getSoftwareName(),
                    Math.abs(daysUntilExpiry)
            );
        } else if (daysUntilExpiry == 1) {
            return String.format(
                    "URGENT: License %s (%s) expires TOMORROW!",
                    license.getLicenseKey(),
                    license.getSoftwareName()
            );
        } else {
            return String.format(
                    "License %s (%s) expires in %d days. Valid until: %s",
                    license.getLicenseKey(),
                    license.getSoftwareName(),
                    daysUntilExpiry,
                    license.getValidTo()
            );
        }
    }

    private AlertResponse mapToResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType().name())
                .severity(alert.getSeverity().name())
                .message(alert.getMessage())
                .region(alert.getRegion() != null ? alert.getRegion().name() : null)
                .generatedAt(alert.getGeneratedAt())
                .acknowledged(alert.getAcknowledged())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .icon(getIconForSeverity(alert.getSeverity()))
                .color(getColorForSeverity(alert.getSeverity()))
                .build();
    }

    private String getIconForSeverity(Severity severity) {
        switch (severity) {
            case CRITICAL:
                return "⚠️";
            case HIGH:
                return "🔴";
            case MEDIUM:
                return "🟡";
            case LOW:
                return "🔵";
            default:
                return "ℹ️";
        }
    }

    private String getColorForSeverity(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "danger";
            case HIGH -> "warning";
            case MEDIUM -> "info";
            case LOW -> "secondary";
            default -> "secondary";
        };
    }
}
