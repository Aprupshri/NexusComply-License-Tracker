package com.prodapt.license_tracker_backend.service.implementation;

import com.prodapt.license_tracker_backend.dto.*;
import com.prodapt.license_tracker_backend.entities.*;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.repository.*;
import com.prodapt.license_tracker_backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final LicenseRepository licenseRepository;
    private final DeviceRepository deviceRepository;
    private final LicenseAssignmentRepository assignmentRepository;

    @Override
    public List<LicenseReportResponse> getLicenseReport(String vendor, String software, String region, String status) {
        log.info("Generating license report with filters - vendor: {}, software: {}, region: {}, status: {}",
                vendor, software, region, status);

        List<License> licenses = licenseRepository.findAll();

        return licenses.stream()
                .filter(l -> vendor == null || vendor.isEmpty() ||
                        (l.getVendor() != null && l.getVendor().getVendorName().equalsIgnoreCase(vendor)))
                .filter(l -> software == null || software.isEmpty() ||
                        l.getSoftwareName().toLowerCase().contains(software.toLowerCase()))
                .filter(l -> region == null || region.isEmpty() ||
                        l.getRegion().name().equalsIgnoreCase(region))
                .map(this::mapToLicenseReport)
                .filter(l -> status == null || status.isEmpty() || l.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    @Override
    public List<DeviceReportResponse> getDeviceReport(String deviceType, String region, String lifecycle) {
        log.info("Generating device report with filters - deviceType: {}, region: {}, lifecycle: {}",
                deviceType, region, lifecycle);

        List<Device> devices = deviceRepository.findAll();

        return devices.stream()
                .filter(d -> deviceType == null || deviceType.isEmpty() ||
                        d.getDeviceType().name().equalsIgnoreCase(deviceType))
                .filter(d -> region == null || region.isEmpty() ||
                        d.getRegion().name().equalsIgnoreCase(region))
                .filter(d -> lifecycle == null || lifecycle.isEmpty() ||
                        d.getLifecycle().name().equalsIgnoreCase(lifecycle))
                .map(this::mapToDeviceReport)
                .toList();
    }

    @Override
    public List<AssignmentReportResponse> getAssignmentReport(String region, Boolean active) {
        log.info("Generating assignment report with filters - region: {}, active: {}", region, active);

        List<LicenseAssignment> assignments = assignmentRepository.findAll();

        return assignments.stream()
                .filter(a -> region == null || region.isEmpty() ||
                        a.getDevice().getRegion().name().equalsIgnoreCase(region))
                .filter(a -> active == null || a.getActive().equals(active))
                .map(this::mapToAssignmentReport)
                .toList();
    }

    @Override
    public List<ComplianceReportResponse> getComplianceReport() {
        log.info("Generating compliance report for all regions");

        return List.of(
                getComplianceReportByRegion(Region.BANGALORE),
                getComplianceReportByRegion(Region.CHENNAI),
                getComplianceReportByRegion(Region.DELHI),
                getComplianceReportByRegion(Region.MUMBAI),
                getComplianceReportByRegion(Region.HYDERABAD),
                getComplianceReportByRegion(Region.KOLKATA)
        );
    }

    @Override
    public ComplianceReportResponse getComplianceReportByRegion(Region region) {
        List<Device> devices = deviceRepository.findByRegion(region);
        List<License> licenses = licenseRepository.findByRegion(region);

        long devicesWithLicenses = devices.stream()
                .filter(assignmentRepository::existsByDeviceAndActiveTrue)
                .count();

        long activeLicenses = licenses.stream().filter(License::getActive).count();
        long expiringLicenses = licenses.stream()
                .filter(l -> l.getActive() &&
                        l.getValidTo().isAfter(LocalDate.now()) &&
                        l.getValidTo().isBefore(LocalDate.now().plusDays(30)))
                .count();
        long expiredLicenses = licenses.stream()
                .filter(l -> l.getValidTo().isBefore(LocalDate.now()))
                .count();

        double compliancePercentage = devices.isEmpty() ? 0.0 :
                (devicesWithLicenses * 100.0) / devices.size();

        return ComplianceReportResponse.builder()
                .region(region.name())
                .totalDevices(devices.size())
                .devicesWithLicenses((int) devicesWithLicenses)
                .devicesWithoutLicenses((int) (devices.size() - devicesWithLicenses))
                .totalLicenses(licenses.size())
                .activeLicenses((int) activeLicenses)
                .expiringLicenses((int) expiringLicenses)
                .expiredLicenses((int) expiredLicenses)
                .compliancePercentage(Math.round(compliancePercentage * 100.0) / 100.0)
                .build();
    }

    private LicenseReportResponse mapToLicenseReport(License license) {
        LocalDate today = LocalDate.now();
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, license.getValidTo());

        String status;
        if (daysUntilExpiry < 0) {
            status = "EXPIRED";
        } else if (daysUntilExpiry <= 30) {
            status = "EXPIRING_SOON";
        } else if (license.getMaxUsage() > 0 &&
                (license.getCurrentUsage() * 100.0 / license.getMaxUsage()) >= 90) {
            status = "NEAR_CAPACITY";
        } else {
            status = "ACTIVE";
        }

        double usagePercentage = license.getMaxUsage() > 0 ?
                (license.getCurrentUsage() * 100.0) / license.getMaxUsage() : 0.0;

        return LicenseReportResponse.builder()
                .licenseId(license.getId())
                .licenseKey(license.getLicenseKey())
                .softwareName(license.getSoftwareName())
                .licenseType(license.getLicenseType().name())
                .maxUsage(license.getMaxUsage())
                .currentUsage(license.getCurrentUsage())
                .usagePercentage(Math.round(usagePercentage * 100.0) / 100.0)
                .validFrom(license.getValidFrom())
                .validTo(license.getValidTo())
                .daysUntilExpiry((int) daysUntilExpiry)
                .status(status)
                .region(license.getRegion().name())
                .vendorName(license.getVendor() != null ? license.getVendor().getVendorName() : "N/A")
                .poNumber(license.getPoNumber())
                .cost(license.getCost() != null ? license.getCost().toString() : "0")
                .build();
    }

    private DeviceReportResponse mapToDeviceReport(Device device) {
        long assignedLicenses = assignmentRepository.countByDeviceAndActiveTrue(device);

        return DeviceReportResponse.builder()
                .deviceId(device.getId())
                .deviceIdName(device.getDeviceId())
                .deviceType(device.getDeviceType().name())
                .model(device.getModel())
                .ipAddress(device.getIpAddress())
                .location(device.getLocation())
                .region(device.getRegion().name())
                .lifecycle(device.getLifecycle().name())
                .softwareName(device.getSoftwareName())
                .softwareVersion(device.getSoftwareVersion())
                .assignedLicensesCount((int) assignedLicenses)
                .purchasedDate(device.getPurchasedDate() != null ? device.getPurchasedDate().toString() : "N/A")
                .build();
    }

    private AssignmentReportResponse mapToAssignmentReport(LicenseAssignment assignment) {
        return AssignmentReportResponse.builder()
                .assignmentId(assignment.getId())
                .licenseKey(assignment.getLicense().getLicenseKey())
                .softwareName(assignment.getLicense().getSoftwareName())
                .deviceId(assignment.getDevice().getDeviceId())
                .deviceType(assignment.getDevice().getDeviceType().name())
                .location(assignment.getDevice().getLocation())
                .region(assignment.getDevice().getRegion().name())
                .assignedOn(assignment.getAssignedOn())
                .assignedBy(assignment.getAssignedBy())
                .active(assignment.getActive())
                .revokedOn(assignment.getRevokedOn())
                .revokedBy(assignment.getRevokedBy())
                .build();
    }
}
