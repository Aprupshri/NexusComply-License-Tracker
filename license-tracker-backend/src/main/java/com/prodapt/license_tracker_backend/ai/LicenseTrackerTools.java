package com.prodapt.license_tracker_backend.ai;

import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.Device;
import com.prodapt.license_tracker_backend.entities.Vendor;
import com.prodapt.license_tracker_backend.entities.LicenseAssignment;
import com.prodapt.license_tracker_backend.repository.*;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseTrackerTools {

    private final LicenseRepository licenseRepository;
    private final DeviceRepository deviceRepository;
    private final VendorRepository vendorRepository;
    private final LicenseAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    @Tool("Fetches a summary of all licenses including total count, active, expiring, and expired licenses")
    public String getLicenseSummary() {
        log.info("AI Tool: Executing getLicenseSummary");
        try {
            List<License> allLicenses = licenseRepository.findAll();

            long totalLicenses = allLicenses.size();
            long activeLicenses = allLicenses.stream()
                    .filter(l -> l.getActive() && l.getValidTo().isAfter(LocalDate.now()))
                    .count();

            long expiringIn30Days = allLicenses.stream()
                    .filter(l -> {
                        LocalDate today = LocalDate.now();
                        LocalDate expiryDate = l.getValidTo();
                        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);
                        return daysUntilExpiry > 0 && daysUntilExpiry <= 30;
                    })
                    .count();

            long expiredLicenses = allLicenses.stream()
                    .filter(l -> l.getValidTo().isBefore(LocalDate.now()))
                    .count();

            return String.format(
                    "License Summary:\n" +
                            "• Total Licenses: %d\n" +
                            "• Active Licenses: %d\n" +
                            "• Expiring in 30 days: %d\n" +
                            "• Expired Licenses: %d",
                    totalLicenses, activeLicenses, expiringIn30Days, expiredLicenses
            );

        } catch (Exception e) {
            log.error("AI Tool: Error fetching license summary", e);
            return "Sorry, I encountered an error fetching license summary.";
        }
    }

    @Tool("Fetches licenses expiring within a specified number of days")
    public String getExpiringLicenses(int days) {
        log.info("AI Tool: Executing getExpiringLicenses with days={}", days);
        try {
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(days);

            List<License> expiringLicenses = licenseRepository.findAll().stream()
                    .filter(l -> {
                        LocalDate expiryDate = l.getValidTo();
                        return expiryDate.isAfter(today) && expiryDate.isBefore(futureDate);
                    })
                    .toList();

            if (expiringLicenses.isEmpty()) {
                return String.format("Good news! No licenses are expiring in the next %d days.", days);
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("Found %d licenses expiring in the next %d days:\n\n",
                    expiringLicenses.size(), days));

            expiringLicenses.forEach(license -> {
                long daysUntilExpiry = ChronoUnit.DAYS.between(today, license.getValidTo());
                result.append(String.format(
                        "• %s (%s) - Expires in %d days (%s)\n",
                        license.getLicenseKey(),
                        license.getSoftwareName(),
                        daysUntilExpiry,
                        license.getValidTo()
                ));
            });

            return result.toString();

        } catch (Exception e) {
            log.error("AI Tool: Error fetching expiring licenses", e);
            return "Sorry, I encountered an error fetching expiring licenses.";
        }
    }

    @Tool("Fetches renewal cost forecast for licenses expiring in the next specified days")
    public String getRenewalForecast(int days) {
        log.info("AI Tool: Executing getRenewalForecast with days={}", days);
        try {
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(days);

            List<License> expiringLicenses = licenseRepository.findAll().stream()
                    .filter(l -> {
                        LocalDate expiryDate = l.getValidTo();
                        return expiryDate.isAfter(today) && expiryDate.isBefore(futureDate);
                    })
                    .toList();

            if (expiringLicenses.isEmpty()) {
                return String.format("No licenses are expiring in the next %d days, so no renewal costs are expected.", days);
            }

            BigDecimal totalCost = expiringLicenses.stream()
                    .map(l -> l.getCost() != null ? l.getCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder result = new StringBuilder();
            result.append(String.format("Renewal Forecast for Next %d Days:\n\n", days));
            result.append(String.format("• Licenses to Renew: %d\n", expiringLicenses.size()));
            result.append(String.format("• Total Estimated Cost: ₹%.2f\n\n", totalCost.doubleValue()));

            result.append("Breakdown by Software:\n");
            expiringLicenses.forEach(license -> result.append(String.format(
                    "• %s: ₹%.2f (Expires: %s)\n",
                    license.getSoftwareName(),
                    license.getCost() != null ? license.getCost().doubleValue() : 0.0,
                    license.getValidTo()
            )));

            return result.toString();

        } catch (Exception e) {
            log.error("AI Tool: Error calculating renewal forecast", e);
            return "Sorry, I encountered an error calculating renewal forecast.";
        }
    }

    @Tool("Fetches device summary including total devices, lifecycle statuses, and outdated software")
    public String getDeviceSummary() {
        log.info("AI Tool: Executing getDeviceSummary");
        try {
            List<Device> devices = deviceRepository.findAll();

            long totalDevices = devices.size();
            long activeDevices = devices.stream()
                    .filter(d -> d.getLifecycle().name().equals("ACTIVE"))
                    .count();
            long decommissionedDevices = devices.stream()
                    .filter(d -> d.getLifecycle().name().equals("DECOMMISSIONED"))
                    .count();

            return String.format(
                    "Device Summary:\n" +
                            "• Total Devices: %d\n" +
                            "• Active Devices: %d\n" +
                            "• Decommissioned: %d\n" +
                            "• Obsolete: %d",
                    totalDevices,
                    activeDevices,
                    decommissionedDevices,
                    totalDevices - activeDevices - decommissionedDevices
            );

        } catch (Exception e) {
            log.error("AI Tool: Error fetching device summary", e);
            return "Sorry, I encountered an error fetching device summary.";
        }
    }

    @Tool("Fetches vendor information including name and contact details")
    public String getVendorInformation(String vendorName) {
        log.info("AI Tool: Executing getVendorInformation for vendor={}", vendorName);
        try {
            List<Vendor> vendors = vendorRepository.findAll();

            Vendor vendor = vendors.stream()
                    .filter(v -> v.getVendorName().toLowerCase().contains(vendorName.toLowerCase()))
                    .findFirst()
                    .orElse(null);

            if (vendor == null) {
                return String.format("No vendor found matching '%s'.", vendorName);
            }

            return String.format(
                    "Vendor Information:\n" +
                            "• Name: %s\n" +
                            "• Contact Email: %s\n" +
                            "• Contact Phone: %s\n" +
                            "• Support Email: %s",
                    vendor.getVendorName(),
                    vendor.getContactEmail(),
                    vendor.getContactPhone(),
                    vendor.getSupportEmail()
            );

        } catch (Exception e) {
            log.error("AI Tool: Error fetching vendor information", e);
            return "Sorry, I encountered an error fetching vendor information.";
        }
    }

    @Tool("Fetches license utilization and capacity information")
    public String getLicenseUtilization() {
        log.info("AI Tool: Executing getLicenseUtilization");
        try {
            List<License> licenses = licenseRepository.findAll();

            StringBuilder result = new StringBuilder();
            result.append("License Utilization Report:\n\n");

            licenses.stream()
                    .filter(License::getActive)
                    .forEach(license -> {
                        double utilizationPct = (license.getCurrentUsage() * 100.0) / license.getMaxUsage();
                        String status = switch ((int) utilizationPct / 10) {
                            case 9, 10 -> "⚠️ CRITICAL";
                            case 7, 8 -> "⚠️ WARNING";
                            default -> "✅ OK";
                        };

                        result.append(String.format(
                                "• %s: %d/%d (%.1f%%) - %s\n",
                                license.getSoftwareName(),
                                license.getCurrentUsage(),
                                license.getMaxUsage(),
                                utilizationPct,
                                status
                        ));
                    });

            return result.toString();

        } catch (Exception e) {
            log.error("AI Tool: Error calculating license utilization", e);
            return "Sorry, I encountered an error calculating license utilization.";
        }
    }

    @Tool("Fetches active license assignments for a specific device")
    public String getDeviceLicenses(String deviceId) {
        log.info("AI Tool: Executing getDeviceLicenses for device={}", deviceId);
        try {
            Device device = deviceRepository.findAll().stream()
                    .filter(d -> d.getDeviceId().toLowerCase().contains(deviceId.toLowerCase()))
                    .findFirst()
                    .orElse(null);

            if (device == null) {
                return String.format("No device found matching '%s'.", deviceId);
            }

            List<LicenseAssignment> assignments = assignmentRepository
                    .findByDeviceIdAndActiveTrue(device.getId());

            if (assignments.isEmpty()) {
                return String.format("Device '%s' has no active license assignments.", device.getDeviceId());
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("Active Licenses for Device '%s':\n\n", device.getDeviceId()));

            assignments.forEach(assignment -> {
                License license = assignment.getLicense();
                result.append(String.format(
                        "• %s (%s) - Assigned on %s\n",
                        license.getSoftwareName(),
                        license.getLicenseKey(),
                        assignment.getAssignedOn()
                ));
            });

            return result.toString();

        } catch (Exception e) {
            log.error("AI Tool: Error fetching device licenses", e);
            return "Sorry, I encountered an error fetching device licenses.";
        }
    }
}
