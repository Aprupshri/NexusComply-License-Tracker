package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.aspect.Auditable;
import com.prodapt.license_tracker_backend.dto.*;
import com.prodapt.license_tracker_backend.entities.Device;
import com.prodapt.license_tracker_backend.entities.LicenseAssignment;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.enums.*;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.exception.ValidationException;
import com.prodapt.license_tracker_backend.repository.*;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final VendorRepository vendorRepository;
    private final LicenseAssignmentRepository assignmentRepository;
    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository; // ADD THIS
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private String[] CSV_HEADERS = {
            "deviceId", "deviceType", "model", "ipAddress", "location",
            "region", "lifecycle", "softwareName", "softwareVersion", "purchasedDate"
    };

    private DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ADD THIS HELPER METHOD
    private Map<String, Object> getCurrentUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                userInfo.put("username", username);

                // Try to get user ID
                try {
                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user != null) {
                        userInfo.put("userId", user.getId());
                    }
                } catch (Exception e) {
                    log.debug("Could not fetch user ID for username: {}", username);
                }
            }
        } catch (Exception e) {
            log.warn("Error getting current user info", e);
        }

        userInfo.putIfAbsent("username", "SYSTEM");
        userInfo.putIfAbsent("userId", null);

        return userInfo;
    }

    @Override
    @Transactional
    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        log.info("Updating device with ID: {}", id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + id));

        // Store old values for comparison
        DeviceLifecycle oldLifecycle = device.getLifecycle();
        String oldModel = device.getModel();
        String oldIpAddress = device.getIpAddress();
        String oldLocation = device.getLocation();
        String oldSoftwareName = device.getSoftwareName();
        String oldSoftwareVersion = device.getSoftwareVersion();

        // Update device fields
        device.setModel(request.getModel());
        device.setIpAddress(request.getIpAddress());
        device.setLocation(request.getLocation());
        device.setLifecycle(request.getLifecycle());
        device.setSoftwareName(request.getSoftwareName());
        device.setSoftwareVersion(request.getSoftwareVersion());
        device.setPurchasedDate(request.getPurchasedDate());

        Device updatedDevice = deviceRepository.save(device);

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Create audit log for the update
        try {
            Map<String, Object> updateDetails = new HashMap<>();
            updateDetails.put("deviceId", updatedDevice.getDeviceId());
            updateDetails.put("changes", new HashMap<String, Object>() {{
                if (!oldModel.equals(request.getModel())) {
                    put("model", Map.of("old", oldModel, "new", request.getModel()));
                }
                if (!oldIpAddress.equals(request.getIpAddress())) {
                    put("ipAddress", Map.of("old", oldIpAddress, "new", request.getIpAddress()));
                }
                if ((oldLocation == null && request.getLocation() != null) ||
                        (oldLocation != null && !oldLocation.equals(request.getLocation()))) {
                    put("location", Map.of("old", oldLocation, "new", request.getLocation()));
                }
                if (oldLifecycle != request.getLifecycle()) {
                    put("lifecycle", Map.of("old", oldLifecycle, "new", request.getLifecycle()));
                }
                if ((oldSoftwareName == null && request.getSoftwareName() != null) ||
                        (oldSoftwareName != null && !oldSoftwareName.equals(request.getSoftwareName()))) {
                    put("softwareName", Map.of("old", oldSoftwareName, "new", request.getSoftwareName()));
                }
                if ((oldSoftwareVersion == null && request.getSoftwareVersion() != null) ||
                        (oldSoftwareVersion != null && !oldSoftwareVersion.equals(request.getSoftwareVersion()))) {
                    put("softwareVersion", Map.of("old", oldSoftwareVersion, "new", request.getSoftwareVersion()));
                }
            }});

            auditLogService.log(
                    userId,
                    username,
                    EntityType.DEVICE,
                    updatedDevice.getId().toString(),
                    AuditAction.UPDATE,
                    objectMapper.writeValueAsString(updateDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for device update", e);
            // Don't throw exception, just log it
        }

        // Auto-revoke licenses if lifecycle changed to DECOMMISSIONED or OBSOLETE
        if (shouldAutoRevokeLicenses(oldLifecycle, request.getLifecycle())) {
            log.info("Lifecycle changed from {} to {}. Auto-revoking licenses for device: {}",
                    oldLifecycle, request.getLifecycle(), updatedDevice.getDeviceId());
            autoRevokeLicenses(updatedDevice, username, userId);
        }

        log.info("Device updated successfully: {}", updatedDevice.getDeviceId());

        return mapToResponse(updatedDevice);
    }

    @Override
    @Transactional
    public void deleteDevice(Long id) {
        log.info("Deleting device with ID: {}", id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + id));

        String deviceId = device.getDeviceId();

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Check if device has active license assignments
        List<LicenseAssignment> activeAssignments = assignmentRepository.findByDeviceAndActiveTrue(device);
        if (!activeAssignments.isEmpty()) {
            log.warn("Device has {} active license assignments. Auto-revoking before deletion.",
                    activeAssignments.size());
            autoRevokeLicenses(device, username, userId);

            // Audit log for deletion with active licenses
            try {
                Map<String, Object> deletionDetails = new HashMap<>();
                deletionDetails.put("deviceId", deviceId);
                deletionDetails.put("hadActiveLicenses", true);
                deletionDetails.put("revokedLicensesCount", activeAssignments.size());

                auditLogService.log(
                        userId,
                        username,
                        EntityType.DEVICE,
                        id.toString(),
                        AuditAction.DELETE,
                        objectMapper.writeValueAsString(deletionDetails)
                );
            } catch (Exception e) {
                log.error("Failed to create deletion audit log", e);
            }
        } else {
            // Audit log for normal deletion
            try {
                Map<String, Object> deletionDetails = new HashMap<>();
                deletionDetails.put("deviceId", deviceId);
                deletionDetails.put("hadActiveLicenses", false);

                auditLogService.log(
                        userId,
                        username,
                        EntityType.DEVICE,
                        id.toString(),
                        AuditAction.DELETE,
                        objectMapper.writeValueAsString(deletionDetails)
                );
            } catch (Exception e) {
                log.error("Failed to create deletion audit log", e);
            }
        }

        deviceRepository.delete(device);
        log.info("Device deleted successfully: {}", deviceId);
    }

    private boolean shouldAutoRevokeLicenses(DeviceLifecycle oldLifecycle, DeviceLifecycle newLifecycle) {
        if (newLifecycle == DeviceLifecycle.DECOMMISSIONED || newLifecycle == DeviceLifecycle.OBSOLETE) {
            return oldLifecycle != newLifecycle;
        }
        return false;
    }

    private void autoRevokeLicenses(Device device, String username, Long userId) {
        List<LicenseAssignment> activeAssignments = assignmentRepository.findByDeviceAndActiveTrue(device);

        if (activeAssignments.isEmpty()) {
            log.info("No active license assignments found for device: {}", device.getDeviceId());
            return;
        }

        log.warn("Auto-revoking {} license assignment(s) for device: {} (Lifecycle: {})",
                activeAssignments.size(), device.getDeviceId(), device.getLifecycle());

        for (LicenseAssignment assignment : activeAssignments) {
            assignment.setActive(false);
            assignment.setRevokedOn(LocalDateTime.now());
            assignment.setRevokedBy(username);
            assignment.setRevocationReason(
                    String.format("Auto-revoked: Device lifecycle changed to %s", device.getLifecycle())
            );

            assignmentRepository.save(assignment);

            // Update license usage
            var license = assignment.getLicense();
            long currentUsage = assignmentRepository.countByLicenseAndActiveTrue(license);
            license.setCurrentUsage((int) currentUsage);
            licenseRepository.save(license);

            // Audit log for each auto-revoked assignment
            try {
                Map<String, Object> revokeDetails = new HashMap<>();
                revokeDetails.put("licenseKey", license.getLicenseKey());
                revokeDetails.put("deviceId", device.getDeviceId());
                revokeDetails.put("reason", "AUTO_REVOKE");
                revokeDetails.put("deviceLifecycle", device.getLifecycle());
                revokeDetails.put("triggeredBy", username);

                auditLogService.log(
                        userId,
                        username,
                        EntityType.ASSIGNMENT,
                        assignment.getId().toString(),
                        AuditAction.UNASSIGN,
                        objectMapper.writeValueAsString(revokeDetails)
                );
            } catch (Exception e) {
                log.error("Failed to create audit log for auto-revoked assignment", e);
            }

            log.info("License {} auto-revoked from device {}",
                    license.getLicenseKey(), device.getDeviceId());
        }

        log.info("Successfully auto-revoked all licenses for device: {}", device.getDeviceId());
    }

    // Keep existing method that calls with current user info
    private void autoRevokeLicenses(Device device) {
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");
        autoRevokeLicenses(device, username, userId);
    }

    @Override
    @Transactional
    public DeviceResponse createDevice(DeviceRequest request) {
        log.info("Creating device: {}", request.getDeviceId());

        if (deviceRepository.existsByDeviceId(request.getDeviceId())) {
            throw new ValidationException("Device ID already exists: " + request.getDeviceId());
        }

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .deviceType(request.getDeviceType())
                .model(request.getModel())
                .ipAddress(request.getIpAddress())
                .location(request.getLocation())
                .region(request.getRegion())
                .lifecycle(request.getLifecycle())
                .softwareName(request.getSoftwareName())
                .softwareVersion(request.getSoftwareVersion())
                .purchasedDate(request.getPurchasedDate())
                .build();

        Device savedDevice = deviceRepository.save(device);

        // Manual audit log creation
        try {
            Map<String, Object> userInfo = getCurrentUserInfo();
            String username = (String) userInfo.get("username");
            Long userId = (Long) userInfo.get("userId");

            Map<String, Object> createDetails = new HashMap<>();
            createDetails.put("deviceId", savedDevice.getDeviceId());
            createDetails.put("deviceType", savedDevice.getDeviceType());
            createDetails.put("model", savedDevice.getModel());
            createDetails.put("region", savedDevice.getRegion());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.DEVICE,
                    savedDevice.getId().toString(),
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(createDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for device creation", e);
        }

        log.info("Device created successfully: {}", savedDevice.getDeviceId());

        return mapToResponse(savedDevice);
    }

    @Override
    @Transactional
    public BulkUploadResult bulkUploadDevices(MultipartFile file) throws IOException {
        log.info("Starting bulk upload for file: {}", file.getOriginalFilename());

        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        BulkUploadResult result = new BulkUploadResult();

        // Validate file
        if (file.isEmpty()) {
            result.addError("File is empty");
            return result;
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            result.addError("Only CSV files are supported");
            return result;
        }

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            List<DeviceBulkUploadRequest> deviceRequests = new ArrayList<>();
            int rowNumber = 1;

            for (CSVRecord record : csvParser) {
                rowNumber++;
                try {
                    DeviceBulkUploadRequest deviceRequest = parseCSVRecord(record, rowNumber);
                    deviceRequests.add(deviceRequest);
                } catch (Exception e) {
                    result.addError(String.format("Row %d: %s", rowNumber, e.getMessage()));
                }
            }

            result.setTotalRecords(deviceRequests.size());

            // Process devices
            for (DeviceBulkUploadRequest request : deviceRequests) {
                try {
                    processDeviceUpload(request, result, username, userId);
                } catch (Exception e) {
                    result.addError(String.format("Row %d (Device: %s): %s",
                            request.getRowNumber(), request.getDeviceId(), e.getMessage()));
                }
            }

            // Create audit log for bulk upload
            try {
                Map<String, Object> bulkUploadDetails = new HashMap<>();
                bulkUploadDetails.put("fileName", file.getOriginalFilename());
                bulkUploadDetails.put("totalRecords", result.getTotalRecords());
                bulkUploadDetails.put("successCount", result.getSuccessCount());
                bulkUploadDetails.put("failureCount", result.getFailureCount());

                auditLogService.log(
                        userId,
                        username,
                        EntityType.DEVICE,
                        "BULK_UPLOAD",
                        AuditAction.CREATE,
                        objectMapper.writeValueAsString(bulkUploadDetails)
                );
            } catch (Exception e) {
                log.error("Failed to create bulk upload audit log", e);
            }

            log.info("Bulk upload completed. Success: {}, Failure: {}",
                    result.getSuccessCount(), result.getFailureCount());

        } catch (Exception e) {
            log.error("Error processing bulk upload", e);
            result.addError("Failed to process file: " + e.getMessage());
        }

        return result;
    }

    private void processDeviceUpload(DeviceBulkUploadRequest request, BulkUploadResult result,
                                     String username, Long userId) {
        if (deviceRepository.existsByDeviceId(request.getDeviceId())) {
            throw new ValidationException("Device ID already exists: " + request.getDeviceId());
        }

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .deviceType(request.getDeviceType())
                .model(request.getModel())
                .ipAddress(request.getIpAddress())
                .location(request.getLocation())
                .region(request.getRegion())
                .lifecycle(request.getLifecycle())
                .softwareName(request.getSoftwareName())
                .softwareVersion(request.getSoftwareVersion())
                .purchasedDate(request.getPurchasedDate())
                .build();

        Device savedDevice = deviceRepository.save(device);
        result.addSuccess(String.format("Device %s created successfully", request.getDeviceId()));

        // Audit log for individual device creation in bulk upload
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("deviceId", savedDevice.getDeviceId());
            details.put("deviceType", savedDevice.getDeviceType());
            details.put("model", savedDevice.getModel());
            details.put("region", savedDevice.getRegion());
            details.put("source", "BULK_UPLOAD");

            auditLogService.log(
                    userId,
                    username,
                    EntityType.DEVICE,
                    savedDevice.getId().toString(),
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(details)
            );
        } catch (Exception e) {
            log.warn("Failed to create audit log for device: {}", savedDevice.getDeviceId(), e);
        }
    }

    private DeviceBulkUploadRequest parseCSVRecord(CSVRecord record, int rowNumber) {
        DeviceBulkUploadRequest request = new DeviceBulkUploadRequest();
        request.setRowNumber(rowNumber);

        request.setDeviceId(getStringValue(record, "deviceId", true));
        request.setDeviceType(parseEnum(record, "deviceType", DeviceType.class, true));
        request.setModel(getStringValue(record, "model", true));
        request.setRegion(parseEnum(record, "region", Region.class, true));
        request.setLifecycle(parseEnum(record, "lifecycle", DeviceLifecycle.class, true));

        request.setIpAddress(getStringValue(record, "ipAddress", false));
        request.setLocation(getStringValue(record, "location", false));
        request.setSoftwareName(getStringValue(record, "softwareName", false));
        request.setSoftwareVersion(getStringValue(record, "softwareVersion", false));

        String purchasedDateStr = getStringValue(record, "purchasedDate", false);
        if (purchasedDateStr != null && !purchasedDateStr.isEmpty()) {
            try {
                request.setPurchasedDate(LocalDate.parse(purchasedDateStr, DATE_FORMATTER));
            } catch (Exception e) {
                throw new ValidationException("Invalid date format for purchasedDate. Expected: yyyy-MM-dd");
            }
        }

        return request;
    }

    private String getStringValue(CSVRecord record, String columnName, boolean required) {
        try {
            String value = record.get(columnName);
            if (value == null || value.trim().isEmpty()) {
                if (required) {
                    throw new ValidationException(columnName + " is required");
                }
                return null;
            }
            return value.trim();
        } catch (IllegalArgumentException e) {
            if (required) {
                throw new ValidationException(columnName + " column not found in CSV");
            }
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(CSVRecord record, String columnName, Class<E> enumClass, boolean required) {
        String value = getStringValue(record, columnName, required);
        if (value == null) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format("Invalid %s value: %s. Valid values: %s",
                    columnName, value, java.util.Arrays.toString(enumClass.getEnumConstants())));
        }
    }

    @Override
    public byte[] generateDeviceTemplate() throws IOException {
        log.info("Generating device upload template");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(CSV_HEADERS)
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format)) {
            csvPrinter.printRecord(
                    "RTR-BLR-101",
                    "ROUTER",
                    "Cisco ASR 9000",
                    "192.168.1.101",
                    "Bangalore DC - Rack A5",
                    "BANGALORE",
                    "ACTIVE",
                    "IOS XR",
                    "7.5.2",
                    "2024-01-15"
            );

            csvPrinter.printRecord(
                    "SW-CHN-201",
                    "SWITCH",
                    "Cisco Catalyst 9500",
                    "192.168.2.201",
                    "Chennai DC - Rack B3",
                    "CHENNAI",
                    "ACTIVE",
                    "IOS-XE",
                    "17.6.3",
                    "2024-02-20"
            );

            csvPrinter.flush();
        }

        return out.toByteArray();
    }

    @Override
    public Page<DeviceResponse> getAllDevices(Pageable pageable) {
        return deviceRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    public DeviceResponse getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + id));
        return mapToResponse(device);
    }

    @Override
    public DeviceResponse getDeviceByDeviceId(String deviceId) {
        Device device = deviceRepository.findById(Long.parseLong(deviceId))
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with deviceId: " + deviceId));
        return mapToResponse(device);
    }

    private DeviceResponse mapToResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceType(device.getDeviceType() != null ? device.getDeviceType().name() : null)
                .model(device.getModel())
                .ipAddress(device.getIpAddress())
                .location(device.getLocation())
                .region(device.getRegion() != null ? device.getRegion().name() : null)
                .lifecycle(device.getLifecycle() != null ? device.getLifecycle().name() : null)
                .softwareName(device.getSoftwareName())
                .softwareVersion(device.getSoftwareVersion())
                .purchasedDate(device.getPurchasedDate())
                .build();
    }
}
