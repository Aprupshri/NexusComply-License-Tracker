package com.prodapt.license_tracker_backend.service.implementation;


import com.prodapt.license_tracker_backend.dto.DashboardStatsResponse;
import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.enums.DeviceLifecycle;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.entities.enums.Severity;
import com.prodapt.license_tracker_backend.repository.*;
import com.prodapt.license_tracker_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final DeviceRepository deviceRepository;
    private final LicenseRepository licenseRepository;
    private final LicenseAssignmentRepository assignmentRepository;
    private final AlertRepository alertRepository;
    private final VendorRepository vendorRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        log.info("Fetching dashboard statistics");

        // Device stats
        long totalDevices = deviceRepository.count();
        long activeDevices = deviceRepository.countByLifecycle(DeviceLifecycle.ACTIVE);

        // License stats
        long totalLicenses = licenseRepository.count();
        long activeLicenses = licenseRepository.countByActiveTrue();

        LocalDate today = LocalDate.now();
        LocalDate next30Days = today.plusDays(30);
        List<License> expiringLicensesList = licenseRepository.findByActiveTrueAndValidToBetween(today, next30Days);
        long expiringLicenses = expiringLicensesList.size();

        // Assignment stats
        long totalAssignments = assignmentRepository.count();
        long activeAssignments = assignmentRepository.countByActiveTrue();

        // Alert stats
        long totalAlerts = alertRepository.count();
        long unacknowledgedAlerts = alertRepository.countUnacknowledged();
        long criticalAlerts = alertRepository.countUnacknowledgedBySeverity(Severity.CRITICAL);

        // Vendor stats
        long totalVendors = vendorRepository.count();

        // Calculate average license utilization
        List<License> allLicenses = licenseRepository.findAll();
        double averageUtilization = allLicenses.stream()
                .filter(l -> l.getMaxUsage() > 0)
                .mapToDouble(l -> (l.getCurrentUsage() * 100.0) / l.getMaxUsage())
                .average()
                .orElse(0.0);

        // Devices without licenses
        long devicesWithoutLicenses = totalDevices - deviceRepository.countDevicesWithActiveLicenses();

        return DashboardStatsResponse.builder()
                .totalDevices(totalDevices)
                .activeDevices(activeDevices)
                .totalLicenses(totalLicenses)
                .activeLicenses(activeLicenses)
                .expiringLicenses(expiringLicenses)
                .totalAssignments(totalAssignments)
                .activeAssignments(activeAssignments)
                .totalAlerts(totalAlerts)
                .unacknowledgedAlerts(unacknowledgedAlerts)
                .criticalAlerts(criticalAlerts)
                .totalVendors(totalVendors)
                .averageLicenseUtilization(Math.round(averageUtilization * 100.0) / 100.0)
                .devicesWithoutLicenses(devicesWithoutLicenses)
                .build();
    }

    @Override
    public DashboardStatsResponse getDashboardStatsByRegion(Region region) {
        log.info("Fetching dashboard statistics for region: {}", region);

        // Device stats by region
        long totalDevices = deviceRepository.countByRegion(region);
        long activeDevices = deviceRepository.countByRegionAndLifecycle(region, DeviceLifecycle.ACTIVE);

        // License stats by region
        long totalLicenses = licenseRepository.countByRegion(region);
        long activeLicenses = licenseRepository.countByRegionAndActiveTrue(region);

        LocalDate today = LocalDate.now();
        LocalDate next30Days = today.plusDays(30);
        List<License> expiringLicensesList = licenseRepository.findByRegionAndActiveTrueAndValidToBetween(
                region, today, next30Days);
        long expiringLicenses = expiringLicensesList.size();

        // Assignment stats by region
        long activeAssignments = assignmentRepository.countActiveAssignmentsByRegion(region);

        // Alert stats by region
        long unacknowledgedAlerts = alertRepository.countByRegionAndAcknowledgedFalse(region);
        long criticalAlerts = alertRepository.countByRegionAndSeverityAndAcknowledgedFalse(
                region, Severity.CRITICAL);

        // Calculate average license utilization for region
        List<License> regionLicenses = licenseRepository.findByRegion(region);
        double averageUtilization = regionLicenses.stream()
                .filter(l -> l.getMaxUsage() > 0 && l.getActive())
                .mapToDouble(l -> (l.getCurrentUsage() * 100.0) / l.getMaxUsage())
                .average()
                .orElse(0.0);

        return DashboardStatsResponse.builder()
                .totalDevices(totalDevices)
                .activeDevices(activeDevices)
                .totalLicenses(totalLicenses)
                .activeLicenses(activeLicenses)
                .expiringLicenses(expiringLicenses)
                .activeAssignments(activeAssignments)
                .unacknowledgedAlerts(unacknowledgedAlerts)
                .criticalAlerts(criticalAlerts)
                .averageLicenseUtilization(Math.round(averageUtilization * 100.0) / 100.0)
                .build();
    }
}
