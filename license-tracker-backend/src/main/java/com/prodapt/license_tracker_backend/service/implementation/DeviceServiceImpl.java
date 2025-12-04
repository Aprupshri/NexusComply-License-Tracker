package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

import static com.prodapt.license_tracker_backend.constants.DeviceConstants.*;
import static com.prodapt.license_tracker_backend.constants.DeviceConstants.CsvHeaders.ALL_HEADERS;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final VendorRepository vendorRepository;
    private final LicenseAssignmentRepository assignmentRepository;
    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(Formats.DATE_FORMAT);

    // Helper method to get current user information
    private Map<String, Object> getCurrentUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                userInfo.put(MapKeys.USERNAME, username);

                Long userId = fetchUserIdByUsername(username);
                userInfo.put(MapKeys.USER_ID, userId);
            }
        } catch (Exception e) {
            log.warn(LogMessages.GET_USER_INFO_ERROR, e);
        }

        userInfo.putIfAbsent(MapKeys.USERNAME, DefaultValues.SYSTEM_USERNAME);
        userInfo.putIfAbsent(MapKeys.USER_ID, null);

        return userInfo;
    }

    // Extracted method: Fetch user ID by username
    private Long fetchUserIdByUsername(String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return user.getId();
            }
        } catch (Exception e) {
            log.debug(LogMessages.FETCH_USER_ID_ERROR, username);
        }
        return null;
    }

    @Override
    @Transactional
    public DeviceResponse createDevice(DeviceRequest request) {
        log.info(LogMessages.CREATING_DEVICE, request.getDeviceId());

        if (deviceRepository.existsByDeviceId(request.getDeviceId())) {
            throw new ValidationException(ErrorMessages.DEVICE_ID_ALREADY_EXISTS + request.getDeviceId());
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
            String username = (String) userInfo.get(MapKeys.USERNAME);
            Long userId = (Long) userInfo.get(MapKeys.USER_ID);

            Map<String, Object> createDetails = new HashMap<>();
            createDetails.put(AuditDetailsKeys.DEVICE_ID, savedDevice.getDeviceId());
            createDetails.put(AuditDetailsKeys.DEVICE_TYPE, savedDevice.getDeviceType());
            createDetails.put(AuditDetailsKeys.MODEL, savedDevice.getModel());
            createDetails.put(AuditDetailsKeys.REGION, savedDevice.getRegion());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.DEVICE,
                    savedDevice.getId().toString(),
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(createDetails)
            );
        } catch (Exception e) {
            log.error(LogMessages.AUDIT_LOG_CREATE_FAILED, e);
        }

        log.info(LogMessages.DEVICE_CREATED, savedDevice.getDeviceId());

        return mapToResponse(savedDevice);
    }

    @Override
    @Transactional
    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        log.info(LogMessages.UPDATING_DEVICE, id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.DEVICE_NOT_FOUND_ID + id));

        // Capture old values
        DeviceOldValues oldValues = captureOldValues(device);

        // Update device fields
        updateDeviceFields(device, request);
        Device updatedDevice = deviceRepository.save(device);

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get(MapKeys.USERNAME);
        Long userId = (Long) userInfo.get(MapKeys.USER_ID);

        // Create device change context
        DeviceChangeContext context = new DeviceChangeContext(updatedDevice, username, userId);

        // Create audit log for the update
        createDeviceUpdateAuditLog(context, oldValues, request);

        // Auto-revoke licenses if lifecycle changed
        if (shouldAutoRevokeLicenses(oldValues.getLifecycle(), request.getLifecycle())) {
            log.info(LogMessages.LIFECYCLE_CHANGED,
                    oldValues.getLifecycle(), request.getLifecycle(), updatedDevice.getDeviceId());
            autoRevokeLicenses(updatedDevice, username, userId);
        }

        log.info(LogMessages.DEVICE_UPDATED, updatedDevice.getDeviceId());
        return mapToResponse(updatedDevice);
    }

    // Extracted method: Capture old values
    private DeviceOldValues captureOldValues(Device device) {
        return new DeviceOldValues(
                device.getLifecycle(),
                device.getModel(),
                device.getIpAddress(),
                device.getLocation(),
                device.getSoftwareName(),
                device.getSoftwareVersion()
        );
    }

    // Extracted method: Update device fields
    private void updateDeviceFields(Device device, DeviceRequest request) {
        device.setModel(request.getModel());
        device.setIpAddress(request.getIpAddress());
        device.setLocation(request.getLocation());
        device.setLifecycle(request.getLifecycle());
        device.setSoftwareName(request.getSoftwareName());
        device.setSoftwareVersion(request.getSoftwareVersion());
        device.setPurchasedDate(request.getPurchasedDate());
    }

    // Extracted method: Create device update audit log
    private void createDeviceUpdateAuditLog(DeviceChangeContext context, DeviceOldValues oldValues, DeviceRequest request) {
        try {
            Map<String, Object> updateDetails = new HashMap<>();
            updateDetails.put(AuditDetailsKeys.DEVICE_ID, context.getUpdatedDevice().getDeviceId());

            Map<String, Object> changes = trackDeviceChanges(oldValues, request);
            updateDetails.put(AuditDetailsKeys.CHANGES, changes);

            auditLogService.log(
                    context.getUserId(),
                    context.getUsername(),
                    EntityType.DEVICE,
                    context.getUpdatedDevice().getId().toString(),
                    AuditAction.UPDATE,
                    objectMapper.writeValueAsString(updateDetails)
            );
        } catch (Exception e) {
            log.error(LogMessages.AUDIT_LOG_UPDATE_FAILED, e);
        }
    }

    // Extracted method: Track device changes
    private Map<String, Object> trackDeviceChanges(DeviceOldValues oldValues, DeviceRequest request) {
        Map<String, Object> changes = new HashMap<>();

        if (!oldValues.getModel().equals(request.getModel())) {
            changes.put(AuditDetailsKeys.MODEL,
                    Map.of(AuditDetailsKeys.OLD, oldValues.getModel(), AuditDetailsKeys.NEW, request.getModel()));
        }
        if (!oldValues.getIpAddress().equals(request.getIpAddress())) {
            changes.put(MapKeys.IP_ADDRESS,
                    Map.of(AuditDetailsKeys.OLD, oldValues.getIpAddress(), AuditDetailsKeys.NEW, request.getIpAddress()));
        }
        if (!Objects.equals(oldValues.getLocation(), request.getLocation())) {
            changes.put(MapKeys.LOCATION,
                    Map.of(AuditDetailsKeys.OLD, oldValues.getLocation(), AuditDetailsKeys.NEW, request.getLocation()));
        }
        if (oldValues.getLifecycle() != request.getLifecycle()) {
            changes.put(MapKeys.LIFECYCLE,
                    Map.of(AuditDetailsKeys.OLD, oldValues.getLifecycle(), AuditDetailsKeys.NEW, request.getLifecycle()));
        }
        if (!Objects.equals(oldValues.getSoftwareName(), request.getSoftwareName())) {
            changes.put(MapKeys.SOFTWARE_NAME,
                    Map.of(AuditDetailsKeys.OLD, oldValues.getSoftwareName(), AuditDetailsKeys.NEW, request.getSoftwareName()));
        }
        if (!Objects.equals(oldValues.getSoftwareVersion(), request.getSoftwareVersion())) {
            changes.put(MapKeys.SOFTWARE_VERSION,
                    Map.of(AuditDetailsKeys.OLD, oldValues.getSoftwareVersion(), AuditDetailsKeys.NEW, request.getSoftwareVersion()));
        }

        return changes;
    }

    @Override
    @Transactional
    public void deleteDevice(Long id) {
        log.info(LogMessages.DELETING_DEVICE, id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.DEVICE_NOT_FOUND_ID + id));

        String deviceId = device.getDeviceId();

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get(MapKeys.USERNAME);
        Long userId = (Long) userInfo.get(MapKeys.USER_ID);

        // Check if device has active license assignments
        List<LicenseAssignment> activeAssignments = assignmentRepository.findByDeviceAndActiveTrue(device);
        if (!activeAssignments.isEmpty()) {
            log.warn(LogMessages.DEVICE_HAS_ACTIVE_ASSIGNMENTS, activeAssignments.size());
            autoRevokeLicenses(device, username, userId);

            // Audit log for deletion with active licenses
            try {
                Map<String, Object> deletionDetails = new HashMap<>();
                deletionDetails.put(AuditDetailsKeys.DEVICE_ID, deviceId);
                deletionDetails.put(AuditDetailsKeys.HAD_ACTIVE_LICENSES, true);
                deletionDetails.put(AuditDetailsKeys.REVOKED_LICENSES_COUNT, activeAssignments.size());

                auditLogService.log(
                        userId,
                        username,
                        EntityType.DEVICE,
                        id.toString(),
                        AuditAction.DELETE,
                        objectMapper.writeValueAsString(deletionDetails)
                );
            } catch (Exception e) {
                log.error(LogMessages.AUDIT_LOG_DELETE_FAILED, e);
            }
        } else {
            // Audit log for normal deletion
            try {
                Map<String, Object> deletionDetails = new HashMap<>();
                deletionDetails.put(AuditDetailsKeys.DEVICE_ID, deviceId);
                deletionDetails.put(AuditDetailsKeys.HAD_ACTIVE_LICENSES, false);

                auditLogService.log(
                        userId,
                        username,
                        EntityType.DEVICE,
                        id.toString(),
                        AuditAction.DELETE,
                        objectMapper.writeValueAsString(deletionDetails)
                );
            } catch (Exception e) {
                log.error(LogMessages.AUDIT_LOG_DELETE_FAILED, e);
            }
        }

        deviceRepository.delete(device);
        log.info(LogMessages.DEVICE_DELETED, deviceId);
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
            log.info(LogMessages.NO_ACTIVE_ASSIGNMENTS, device.getDeviceId());
            return;
        }

        log.warn(LogMessages.AUTO_REVOKING_LICENSES,
                activeAssignments.size(), device.getDeviceId(), device.getLifecycle());

        for (LicenseAssignment assignment : activeAssignments) {
            assignment.setActive(false);
            assignment.setRevokedOn(LocalDateTime.now());
            assignment.setRevokedBy(username);
            assignment.setRevocationReason(
                    String.format(Formats.AUTO_REVOKE_REASON_FORMAT, device.getLifecycle())
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
                revokeDetails.put(AuditDetailsKeys.LICENSE_KEY, license.getLicenseKey());
                revokeDetails.put(AuditDetailsKeys.DEVICE_ID, device.getDeviceId());
                revokeDetails.put(AuditDetailsKeys.REASON, DefaultValues.AUTO_REVOKE_REASON);
                revokeDetails.put(AuditDetailsKeys.DEVICE_LIFECYCLE, device.getLifecycle());
                revokeDetails.put(AuditDetailsKeys.TRIGGERED_BY, username);

                auditLogService.log(
                        userId,
                        username,
                        EntityType.ASSIGNMENT,
                        assignment.getId().toString(),
                        AuditAction.UNASSIGN,
                        objectMapper.writeValueAsString(revokeDetails)
                );
            } catch (Exception e) {
                log.error(LogMessages.AUDIT_LOG_AUTO_REVOKE_FAILED, e);
            }

            log.info(LogMessages.LICENSE_AUTO_REVOKED, license.getLicenseKey(), device.getDeviceId());
        }

        log.info(LogMessages.ALL_LICENSES_REVOKED, device.getDeviceId());
    }

    @Override
    @Transactional
    public BulkUploadResult bulkUploadDevices(MultipartFile file) throws IOException {
        log.info(LogMessages.STARTING_BULK_UPLOAD, file.getOriginalFilename());

        BulkUploadResult result = new BulkUploadResult();

        // Validate file
        if (file.isEmpty()) {
            result.addError(ErrorMessages.FILE_EMPTY);
            return result;
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(FileConstants.CSV_EXTENSION)) {
            result.addError(ErrorMessages.ONLY_CSV_SUPPORTED);
            return result;
        }

        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CSVFormat csvFormat = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setIgnoreSurroundingSpaces(true)
                    .build();

            CSVParser csvParser = new CSVParser(reader, csvFormat);

            // Parse CSV and collect device requests
            List<DeviceBulkUploadRequest> deviceRequests = parseAndCollectDeviceRequests(csvParser, result);
            result.setTotalRecords(deviceRequests.size());

            // Process all devices
            processAllDevices(deviceRequests, result);

            // Create audit log
            Map<String, Object> userInfo = getCurrentUserInfo();
            createBulkUploadAuditLog(file, result, userInfo);

            log.info(LogMessages.BULK_UPLOAD_COMPLETED,
                    result.getSuccessCount(), result.getFailureCount());

        } catch (Exception e) {
            log.error(LogMessages.ERROR_BULK_UPLOAD, e);
            result.addError(ErrorMessages.FAILED_TO_PROCESS_FILE + e.getMessage());
        }

        return result;
    }

    // Extracted method: Parse CSV and collect device requests
    private List<DeviceBulkUploadRequest> parseAndCollectDeviceRequests(CSVParser csvParser, BulkUploadResult result) {
        List<DeviceBulkUploadRequest> deviceRequests = new ArrayList<>();
        int rowNumber = 1;

        for (CSVRecord csvRecord : csvParser) {
            rowNumber++;
            try {
                DeviceBulkUploadRequest deviceRequest = parseCSVRecord(csvRecord, rowNumber);
                deviceRequests.add(deviceRequest);
            } catch (Exception e) {
                result.addError(String.format(Formats.CSV_ROW_ERROR_FORMAT, rowNumber, e.getMessage()));
            }
        }

        return deviceRequests;
    }

    // Extracted method: Process all devices
    private void processAllDevices(List<DeviceBulkUploadRequest> deviceRequests, BulkUploadResult result) {
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get(MapKeys.USERNAME);
        Long userId = (Long) userInfo.get(MapKeys.USER_ID);

        for (DeviceBulkUploadRequest request : deviceRequests) {
            try {
                processDeviceUpload(request, result, username, userId);
            } catch (Exception e) {
                result.addError(String.format(Formats.CSV_ROW_DEVICE_ERROR_FORMAT,
                        request.getRowNumber(), request.getDeviceId(), e.getMessage()));
            }
        }
    }

    // Extracted method: Create bulk upload audit log
    private void createBulkUploadAuditLog(MultipartFile file, BulkUploadResult result, Map<String, Object> userInfo) {
        try {
            Map<String, Object> bulkUploadDetails = new HashMap<>();
            bulkUploadDetails.put(AuditDetailsKeys.FILE_NAME, file.getOriginalFilename());
            bulkUploadDetails.put(AuditDetailsKeys.TOTAL_RECORDS, result.getTotalRecords());
            bulkUploadDetails.put(AuditDetailsKeys.SUCCESS_COUNT, result.getSuccessCount());
            bulkUploadDetails.put(AuditDetailsKeys.FAILURE_COUNT, result.getFailureCount());

            String username = (String) userInfo.get(MapKeys.USERNAME);
            Long userId = (Long) userInfo.get(MapKeys.USER_ID);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.DEVICE,
                    DefaultValues.BULK_UPLOAD_ENTITY_ID,
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(bulkUploadDetails)
            );
        } catch (Exception e) {
            log.error(LogMessages.AUDIT_LOG_BULK_UPLOAD_FAILED, e);
        }
    }

    private void processDeviceUpload(DeviceBulkUploadRequest request, BulkUploadResult result,
                                     String username, Long userId) {
        if (deviceRepository.existsByDeviceId(request.getDeviceId())) {
            throw new ValidationException(ErrorMessages.DEVICE_ID_ALREADY_EXISTS + request.getDeviceId());
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
        result.addSuccess(String.format(Formats.DEVICE_CREATED_SUCCESS_FORMAT, request.getDeviceId()));

        // Audit log for individual device creation in bulk upload
        try {
            Map<String, Object> details = new HashMap<>();
            details.put(AuditDetailsKeys.DEVICE_ID, savedDevice.getDeviceId());
            details.put(AuditDetailsKeys.DEVICE_TYPE, savedDevice.getDeviceType());
            details.put(AuditDetailsKeys.MODEL, savedDevice.getModel());
            details.put(AuditDetailsKeys.REGION, savedDevice.getRegion());
            details.put(AuditDetailsKeys.SOURCE, DefaultValues.BULK_UPLOAD_SOURCE);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.DEVICE,
                    savedDevice.getId().toString(),
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(details)
            );
        } catch (Exception e) {
            log.warn(LogMessages.AUDIT_LOG_FAILED_DEVICE, savedDevice.getDeviceId(), e);
        }
    }

    private DeviceBulkUploadRequest parseCSVRecord(CSVRecord csvRecord, int rowNumber) {
        DeviceBulkUploadRequest request = new DeviceBulkUploadRequest();
        request.setRowNumber(rowNumber);

        request.setDeviceId(getStringValue(csvRecord, CsvHeaders.DEVICE_ID, true));
        request.setDeviceType(parseEnum(csvRecord, CsvHeaders.DEVICE_TYPE, DeviceType.class, true));
        request.setModel(getStringValue(csvRecord, CsvHeaders.MODEL, true));
        request.setRegion(parseEnum(csvRecord, CsvHeaders.REGION, Region.class, true));
        request.setLifecycle(parseEnum(csvRecord, CsvHeaders.LIFECYCLE, DeviceLifecycle.class, true));

        request.setIpAddress(getStringValue(csvRecord, CsvHeaders.IP_ADDRESS, false));
        request.setLocation(getStringValue(csvRecord, CsvHeaders.LOCATION, false));
        request.setSoftwareName(getStringValue(csvRecord, CsvHeaders.SOFTWARE_NAME, false));
        request.setSoftwareVersion(getStringValue(csvRecord, CsvHeaders.SOFTWARE_VERSION, false));

        String purchasedDateStr = getStringValue(csvRecord, CsvHeaders.PURCHASED_DATE, false);
        if (purchasedDateStr != null && !purchasedDateStr.isEmpty()) {
            try {
                request.setPurchasedDate(LocalDate.parse(purchasedDateStr, dateFormatter));
            } catch (Exception e) {
                throw new ValidationException(ErrorMessages.INVALID_DATE_FORMAT);
            }
        }

        return request;
    }

    private String getStringValue(CSVRecord csvRecord, String columnName, boolean required) {
        try {
            String value = csvRecord.get(columnName);
            if (value == null || value.trim().isEmpty()) {
                if (required) {
                    throw new ValidationException(columnName + ErrorMessages.IS_REQUIRED);
                }
                return null;
            }
            return value.trim();
        } catch (IllegalArgumentException e) {
            if (required) {
                throw new ValidationException(columnName + ErrorMessages.COLUMN_NOT_FOUND);
            }
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(CSVRecord csvRecord, String columnName, Class<E> enumClass, boolean required) {
        String value = getStringValue(csvRecord, columnName, required);
        if (value == null) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format(ErrorMessages.INVALID_VALUE_FORMAT,
                    columnName, value, Arrays.toString(enumClass.getEnumConstants())));
        }
    }

    @Override
    public byte[] generateDeviceTemplate() throws IOException {
        log.info(LogMessages.GENERATING_TEMPLATE);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(ALL_HEADERS)
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format)) {
            csvPrinter.printRecord(
                    CsvTemplateData.SAMPLE_DEVICE_ID_1,
                    CsvTemplateData.SAMPLE_DEVICE_TYPE_1,
                    CsvTemplateData.SAMPLE_MODEL_1,
                    CsvTemplateData.SAMPLE_IP_1,
                    CsvTemplateData.SAMPLE_LOCATION_1,
                    CsvTemplateData.SAMPLE_REGION_1,
                    CsvTemplateData.SAMPLE_LIFECYCLE_1,
                    CsvTemplateData.SAMPLE_SOFTWARE_NAME_1,
                    CsvTemplateData.SAMPLE_SOFTWARE_VERSION_1,
                    CsvTemplateData.SAMPLE_PURCHASED_DATE_1
            );

            csvPrinter.printRecord(
                    CsvTemplateData.SAMPLE_DEVICE_ID_2,
                    CsvTemplateData.SAMPLE_DEVICE_TYPE_2,
                    CsvTemplateData.SAMPLE_MODEL_2,
                    CsvTemplateData.SAMPLE_IP_2,
                    CsvTemplateData.SAMPLE_LOCATION_2,
                    CsvTemplateData.SAMPLE_REGION_2,
                    CsvTemplateData.SAMPLE_LIFECYCLE_2,
                    CsvTemplateData.SAMPLE_SOFTWARE_NAME_2,
                    CsvTemplateData.SAMPLE_SOFTWARE_VERSION_2,
                    CsvTemplateData.SAMPLE_PURCHASED_DATE_2
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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.DEVICE_NOT_FOUND_ID + id));
        return mapToResponse(device);
    }

    @Override
    public DeviceResponse getDeviceByDeviceId(String deviceId) {
        Device device = deviceRepository.findById(Long.parseLong(deviceId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.DEVICE_NOT_FOUND_DEVICE_ID + deviceId));
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
