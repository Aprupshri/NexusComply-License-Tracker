package com.prodapt.license_tracker_backend.scheduler;


import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.LicenseAssignment;
import com.prodapt.license_tracker_backend.repository.LicenseAssignmentRepository;
import com.prodapt.license_tracker_backend.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LicenseExpiryScheduler {

    private final LicenseRepository licenseRepository;
    private final LicenseAssignmentRepository assignmentRepository;

    /**
     * Runs every day at 1:00 AM to check for expired licenses
     * Cron: "0 0 1 * * ?" = second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void checkExpiredLicenses() {
        log.info("=== Starting scheduled license expiry check ===");

        LocalDate today = LocalDate.now();

        // Find all active licenses that have expired
        List<License> expiredLicenses = licenseRepository.findByActiveTrueAndValidToBefore(today);

        if (expiredLicenses.isEmpty()) {
            log.info("No expired licenses found");
            return;
        }

        log.warn("Found {} expired license(s)", expiredLicenses.size());

        int deactivatedCount = 0;
        int assignmentsRevokedCount = 0;

        for (License license : expiredLicenses) {
            try {
                // Mark license as inactive
                license.setActive(false);
                licenseRepository.save(license);
                deactivatedCount++;

                log.info("License {} expired and deactivated. Valid until: {}",
                        license.getLicenseKey(), license.getValidTo());

                // Auto-revoke all active assignments
                List<LicenseAssignment> activeAssignments =
                        assignmentRepository.findByLicenseIdAndActiveTrue(license.getId());

                for (LicenseAssignment assignment : activeAssignments) {
                    assignment.setActive(false);
                    assignment.setRevokedOn(LocalDateTime.now());
                    assignment.setRevokedBy("SYSTEM");
                    assignment.setRevocationReason(
                            String.format("Auto-revoked: License expired on %s", license.getValidTo())
                    );
                    assignmentRepository.save(assignment);
                    assignmentsRevokedCount++;

                    log.info("Auto-revoked assignment {} (Device: {}, License: {})",
                            assignment.getId(),
                            assignment.getDevice().getDeviceId(),
                            license.getLicenseKey());
                }

                // Update license current usage
                license.setCurrentUsage(0);
                licenseRepository.save(license);

            } catch (Exception e) {
                log.error("Error processing expired license {}: {}",
                        license.getLicenseKey(), e.getMessage(), e);
            }
        }

        log.info("=== License expiry check completed ===");
        log.info("Licenses deactivated: {}", deactivatedCount);
        log.info("Assignments revoked: {}", assignmentsRevokedCount);
    }

    /**
     * Manual trigger method for testing or admin use
     */
    @Transactional
    public void checkExpiredLicensesManually() {
        log.info("Manual license expiry check triggered");
        checkExpiredLicenses();
    }
}
