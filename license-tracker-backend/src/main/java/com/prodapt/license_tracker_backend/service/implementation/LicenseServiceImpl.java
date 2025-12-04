package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.dto.LicenseRequest;
import com.prodapt.license_tracker_backend.dto.LicenseResponse;
import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.Vendor;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.repository.LicenseRepository;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.repository.VendorRepository;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.LicenseService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseServiceImpl implements LicenseService {

    private final LicenseRepository licenseRepository;
    private final VendorRepository vendorRepository;
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

    // Extracted method: Fetch user ID from repository
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
    @Transactional
    public LicenseResponse createLicense(LicenseRequest request) {
        log.info("Creating license: {}", request.getLicenseKey());

        if (licenseRepository.existsByLicenseKey(request.getLicenseKey())) {
            throw new ValidationException("License key already exists: " + request.getLicenseKey());
        }

        License license = License.builder()
                .licenseKey(request.getLicenseKey())
                .softwareName(request.getSoftwareName())
                .licenseType(request.getLicenseType())
                .maxUsage(request.getMaxUsage())
                .currentUsage(0)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .region(request.getRegion())
                .poNumber(request.getPoNumber())
                .cost(request.getCost())
                .description(request.getDescription())
                .active(true)
                .build();

        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + request.getVendorId()));
            license.setVendor(vendor);
        }

        License savedLicense = licenseRepository.save(license);

        // Create audit log
        try {
            Map<String, Object> userInfo = getCurrentUserInfo();
            Long userId = (Long) userInfo.get("userId");
            String username = (String) userInfo.get("username");

            long daysUntilExpiry = ChronoUnit.DAYS.between(
                    savedLicense.getValidFrom(),
                    savedLicense.getValidTo()
            );

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("licenseKey", savedLicense.getLicenseKey());
            auditDetails.put("softwareName", savedLicense.getSoftwareName());
            auditDetails.put("licenseType", savedLicense.getLicenseType().name());
            auditDetails.put("maxUsage", savedLicense.getMaxUsage());
            auditDetails.put("region", savedLicense.getRegion().name());
            auditDetails.put("validFrom", savedLicense.getValidFrom().toString());
            auditDetails.put("validTo", savedLicense.getValidTo().toString());
            auditDetails.put("daysUntilExpiry", daysUntilExpiry);
            auditDetails.put("vendorId", request.getVendorId());
            auditDetails.put("vendorName", savedLicense.getVendor() != null ? savedLicense.getVendor().getVendorName() : null);
            auditDetails.put("poNumber", savedLicense.getPoNumber());
            auditDetails.put("cost", savedLicense.getCost() != null ? savedLicense.getCost().toString() : null);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.LICENSE,
                    savedLicense.getId().toString(),
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for license creation", e);
        }

        log.info("License created successfully: {}", savedLicense.getLicenseKey());

        return mapToResponse(savedLicense);
    }


    @Override
    @Transactional
    public LicenseResponse updateLicense(Long id, LicenseRequest request) {
        log.info("Updating license with ID: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with id: " + id));

        // Capture old values before update
        Map<String, Object> oldValues = captureOldValues(license);

        // Update license
        updateLicenseFields(license, request);
        License updatedLicense = licenseRepository.save(license);

        // Get user info and create audit log
        Map<String, Object> userInfo = getCurrentUserInfo();
        createLicenseUpdateAuditLog(updatedLicense, oldValues, request, userInfo);

        log.info("License updated successfully: {}", updatedLicense.getLicenseKey());
        return mapToResponse(updatedLicense);
    }

    // Extracted method: Capture old values before update
    private Map<String, Object> captureOldValues(License license) {
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("softwareName", license.getSoftwareName());
        oldValues.put("licenseType", license.getLicenseType() != null ? license.getLicenseType().name() : null);
        oldValues.put("maxUsage", license.getMaxUsage());
        oldValues.put("validFrom", license.getValidFrom() != null ? license.getValidFrom().toString() : null);
        oldValues.put("validTo", license.getValidTo() != null ? license.getValidTo().toString() : null);
        oldValues.put("region", license.getRegion() != null ? license.getRegion().name() : null);
        oldValues.put("poNumber", license.getPoNumber());
        oldValues.put("cost", license.getCost() != null ? license.getCost().toString() : null);
        oldValues.put("description", license.getDescription());
        oldValues.put("vendorId", license.getVendor() != null ? license.getVendor().getId() : null);
        oldValues.put("vendorName", license.getVendor() != null ? license.getVendor().getVendorName() : null);
        return oldValues;
    }

    // Extracted method: Update license fields
    private void updateLicenseFields(License license, LicenseRequest request) {
        license.setSoftwareName(request.getSoftwareName());
        license.setLicenseType(request.getLicenseType());
        license.setMaxUsage(request.getMaxUsage());
        license.setValidFrom(request.getValidFrom());
        license.setValidTo(request.getValidTo());
        license.setRegion(request.getRegion());
        license.setPoNumber(request.getPoNumber());
        license.setCost(request.getCost());
        license.setDescription(request.getDescription());

        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + request.getVendorId()));
            license.setVendor(vendor);
        } else {
            license.setVendor(null);
        }
    }

    // Extracted method: Create audit log
    private void createLicenseUpdateAuditLog(License updatedLicense, Map<String, Object> oldValues,
                                             LicenseRequest request, Map<String, Object> userInfo) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("licenseKey", updatedLicense.getLicenseKey());
            auditDetails.put("licenseId", updatedLicense.getId());

            Map<String, Map<String, Object>> changes = trackChanges(oldValues, request, updatedLicense);
            auditDetails.put("changes", changes);
            auditDetails.put("changeCount", changes.size());

            String username = (String) userInfo.get("username");
            Long userId = (Long) userInfo.get("userId");

            auditLogService.log(
                    userId,
                    username,
                    EntityType.LICENSE,
                    updatedLicense.getId().toString(),
                    AuditAction.UPDATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for license update", e);
        }
    }

    // Extracted method: Track all changes
    private Map<String, Map<String, Object>> trackChanges(Map<String, Object> oldValues,
                                                          LicenseRequest request, License updatedLicense) {
        Map<String, Map<String, Object>> changes = new HashMap<>();

        trackSimpleChange(changes, "softwareName", oldValues.get("softwareName"), request.getSoftwareName());
        trackSimpleChange(changes, "licenseType", oldValues.get("licenseType"), request.getLicenseType().name());
        trackSimpleChange(changes, "maxUsage", oldValues.get("maxUsage"), request.getMaxUsage());
        trackDateChange(changes, "validFrom", (String) oldValues.get("validFrom"),
                request.getValidFrom() != null ? request.getValidFrom().toString() : null);
        trackDateChange(changes, "validTo", (String) oldValues.get("validTo"),
                request.getValidTo() != null ? request.getValidTo().toString() : null);
        trackSimpleChange(changes, "region", oldValues.get("region"), request.getRegion().name());
        trackNullableChange(changes, "poNumber", oldValues.get("poNumber"), request.getPoNumber());
        trackNullableChange(changes, "cost", oldValues.get("cost"),
                request.getCost() != null ? request.getCost().toString() : null);
        trackNullableChange(changes, "description", oldValues.get("description"), request.getDescription());
        trackVendorChange(changes, oldValues, request.getVendorId(), updatedLicense.getVendor());

        return changes;
    }

    // Extracted method: Track simple change
    private void trackSimpleChange(Map<String, Map<String, Object>> changes, String fieldName,
                                   Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.put(fieldName, Map.of("old", oldValue, "new", newValue));
        }
    }

    // Extracted method: Track nullable change
    private void trackNullableChange(Map<String, Map<String, Object>> changes, String fieldName,
                                     Object oldValue, Object newValue) {
        if ((oldValue == null && newValue != null) ||
                (oldValue != null && !oldValue.equals(newValue))) {
            changes.put(fieldName, Map.of("old", oldValue != null ? oldValue : "null",
                    "new", newValue != null ? newValue : "null"));
        }
    }

    // Extracted method: Track date change
    private void trackDateChange(Map<String, Map<String, Object>> changes, String fieldName,
                                 String oldValue, String newValue) {
        if ((oldValue == null && newValue != null) ||
                (oldValue != null && !oldValue.equals(newValue))) {
            changes.put(fieldName, Map.of("old", oldValue, "new", newValue));
        }
    }

    // Extracted method: Track vendor change
    private void trackVendorChange(Map<String, Map<String, Object>> changes, Map<String, Object> oldValues,
                                   Long newVendorId, Vendor newVendor) {
        Long oldVendorId = (Long) oldValues.get("vendorId");
        String oldVendorName = (String) oldValues.get("vendorName");

        if (!Objects.equals(oldVendorId, newVendorId)) {
            String newVendorName = newVendor != null ? newVendor.getVendorName() : null;
            changes.put("vendor", Map.of(
                    "oldVendorId", oldVendorId != null ? oldVendorId : "null",
                    "oldVendorName", oldVendorName != null ? oldVendorName : "null",
                    "newVendorId", newVendorId != null ? newVendorId : "null",
                    "newVendorName", newVendorName != null ? newVendorName : "null"
            ));
        }
    }

    @Override
    public Page<LicenseResponse> getAllLicenses(Pageable pageable) {
        return licenseRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    public LicenseResponse getLicenseById(Long id) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with id: " + id));
        return mapToResponse(license);
    }

    @Override
    public LicenseResponse getLicenseByKey(String licenseKey) {
        License license = licenseRepository.findByLicenseKey(licenseKey)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with key: " + licenseKey));
        return mapToResponse(license);
    }

    @Override
    @Transactional
    public void deleteLicense(Long id) {
        log.info("Deleting license with ID: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with id: " + id));

        if (license.getCurrentUsage() > 0) {
            throw new ValidationException("Cannot delete license with active assignments");
        }

        String licenseKey = license.getLicenseKey();
        String softwareName = license.getSoftwareName();
        String region = license.getRegion() != null ? license.getRegion().name() : null;
        String vendorName = license.getVendor() != null ? license.getVendor().getVendorName() : null;

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Delete the license
        licenseRepository.delete(license);

        // Create audit log
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("licenseKey", licenseKey);
            auditDetails.put("softwareName", softwareName);
            auditDetails.put("region", region);
            auditDetails.put("vendorName", vendorName);
            auditDetails.put("hadActiveAssignments", false);
            auditDetails.put("currentUsage", 0);
            auditDetails.put("deletedAt", java.time.LocalDateTime.now().toString());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.LICENSE,
                    id.toString(),
                    AuditAction.DELETE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for license deletion", e);
        }

        log.info("License deleted successfully: {}", licenseKey);
    }

    private LicenseResponse mapToResponse(License license) {
        return LicenseResponse.builder()
                .id(license.getId())
                .licenseKey(license.getLicenseKey())
                .softwareName(license.getSoftwareName())
                .licenseType(license.getLicenseType() != null ? license.getLicenseType().name() : null)
                .maxUsage(license.getMaxUsage())
                .currentUsage(license.getCurrentUsage())
                .validFrom(license.getValidFrom())
                .validTo(license.getValidTo())
                .region(license.getRegion() != null ? license.getRegion().name() : null)
                .poNumber(license.getPoNumber())
                .cost(license.getCost())
                .active(license.getActive())
                .vendorId(license.getVendor() != null ? license.getVendor().getId() : null)
                .vendorName(license.getVendor() != null ? license.getVendor().getVendorName() : null)
                .description(license.getDescription())
                .build();
    }
}
