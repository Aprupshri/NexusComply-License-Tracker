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

        // Store old values for audit
        String oldSoftwareName = license.getSoftwareName();
        String oldLicenseType = license.getLicenseType() != null ? license.getLicenseType().name() : null;
        Integer oldMaxUsage = license.getMaxUsage();
        String oldValidFrom = license.getValidFrom() != null ? license.getValidFrom().toString() : null;
        String oldValidTo = license.getValidTo() != null ? license.getValidTo().toString() : null;
        String oldRegion = license.getRegion() != null ? license.getRegion().name() : null;
        String oldPoNumber = license.getPoNumber();
        String oldCost = license.getCost() != null ? license.getCost().toString() : null;
        String oldDescription = license.getDescription();
        Long oldVendorId = license.getVendor() != null ? license.getVendor().getId() : null;
        String oldVendorName = license.getVendor() != null ? license.getVendor().getVendorName() : null;

        // Update license fields
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

        License updatedLicense = licenseRepository.save(license);

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Create audit log with changes
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("licenseKey", updatedLicense.getLicenseKey());
            auditDetails.put("licenseId", updatedLicense.getId());

            // Track changes
            Map<String, Map<String, Object>> changes = new HashMap<>();

            if (!oldSoftwareName.equals(request.getSoftwareName())) {
                changes.put("softwareName", Map.of("old", oldSoftwareName, "new", request.getSoftwareName()));
            }
            if (!oldLicenseType.equals(request.getLicenseType().name())) {
                changes.put("licenseType", Map.of("old", oldLicenseType, "new", request.getLicenseType().name()));
            }
            if (!oldMaxUsage.equals(request.getMaxUsage())) {
                changes.put("maxUsage", Map.of("old", oldMaxUsage, "new", request.getMaxUsage()));
            }
            if ((oldValidFrom == null && request.getValidFrom() != null) ||
                    (oldValidFrom != null && !oldValidFrom.equals(request.getValidFrom().toString()))) {
                changes.put("validFrom", Map.of("old", oldValidFrom, "new", request.getValidFrom().toString()));
            }
            if ((oldValidTo == null && request.getValidTo() != null) ||
                    (oldValidTo != null && !oldValidTo.equals(request.getValidTo().toString()))) {
                changes.put("validTo", Map.of("old", oldValidTo, "new", request.getValidTo().toString()));
            }
            if (!oldRegion.equals(request.getRegion().name())) {
                changes.put("region", Map.of("old", oldRegion, "new", request.getRegion().name()));
            }
            if ((oldPoNumber == null && request.getPoNumber() != null) ||
                    (oldPoNumber != null && !oldPoNumber.equals(request.getPoNumber()))) {
                changes.put("poNumber", Map.of("old", oldPoNumber, "new", request.getPoNumber()));
            }
            String newCost = request.getCost() != null ? request.getCost().toString() : null;
            if ((oldCost == null && newCost != null) ||
                    (oldCost != null && !oldCost.equals(newCost))) {
                changes.put("cost", Map.of("old", oldCost, "new", newCost));
            }
            if ((oldDescription == null && request.getDescription() != null) ||
                    (oldDescription != null && !oldDescription.equals(request.getDescription()))) {
                changes.put("description", Map.of(
                        "old", oldDescription != null ? oldDescription : "null",
                        "new", request.getDescription() != null ? request.getDescription() : "null"
                ));
            }
            if ((oldVendorId == null && request.getVendorId() != null) ||
                    (oldVendorId != null && !oldVendorId.equals(request.getVendorId()))) {
                String newVendorName = updatedLicense.getVendor() != null ? updatedLicense.getVendor().getVendorName() : null;
                changes.put("vendor", Map.of(
                        "oldVendorId", oldVendorId != null ? oldVendorId : "null",
                        "oldVendorName", oldVendorName != null ? oldVendorName : "null",
                        "newVendorId", request.getVendorId() != null ? request.getVendorId() : "null",
                        "newVendorName", newVendorName != null ? newVendorName : "null"
                ));
            }

            auditDetails.put("changes", changes);
            auditDetails.put("changeCount", changes.size());

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

        log.info("License updated successfully: {}", updatedLicense.getLicenseKey());

        return mapToResponse(updatedLicense);
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
