package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.dto.CreateVendorRequest;
import com.prodapt.license_tracker_backend.dto.UpdateVendorRequest;
import com.prodapt.license_tracker_backend.dto.VendorResponse;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.Vendor;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.exception.ValidationException;
import com.prodapt.license_tracker_backend.repository.LicenseRepository;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.repository.VendorRepository;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * Helper method to get current user information
     * Returns a map with userId and username, defaults to SYSTEM if not authenticated
     */
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
    public VendorResponse createVendor(CreateVendorRequest request) {
        log.info("Creating vendor: {}", request.getVendorName());

        // Check if vendor name already exists
        if (vendorRepository.existsByVendorName(request.getVendorName())) {
            throw new ValidationException("Vendor with name '" + request.getVendorName() + "' already exists");
        }

        Vendor vendor = Vendor.builder()
                .vendorName(request.getVendorName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .supportEmail(request.getSupportEmail())
                .build();

        Vendor savedVendor = vendorRepository.save(vendor);

        // Create audit log
        try {
            Map<String, Object> userInfo = getCurrentUserInfo();
            Long userId = (Long) userInfo.get("userId");
            String username = (String) userInfo.get("username");

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("vendorId", savedVendor.getId());
            auditDetails.put("vendorName", savedVendor.getVendorName());
            auditDetails.put("contactEmail", savedVendor.getContactEmail());
            auditDetails.put("contactPhone", savedVendor.getContactPhone());
            auditDetails.put("supportEmail", savedVendor.getSupportEmail());
            auditDetails.put("hasContactEmail", savedVendor.getContactEmail() != null);
            auditDetails.put("hasContactPhone", savedVendor.getContactPhone() != null);
            auditDetails.put("hasSupportEmail", savedVendor.getSupportEmail() != null);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.VENDOR,
                    savedVendor.getId().toString(),
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for vendor creation", e);
        }

        log.info("Vendor created successfully with ID: {}", savedVendor.getId());

        return mapToResponse(savedVendor);
    }

    @Override
    @Transactional
    public VendorResponse updateVendor(Long id, UpdateVendorRequest request) {
        log.info("Updating vendor with ID: {}", id);

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + id));

        // Store old values for audit
        String oldVendorName = vendor.getVendorName();
        String oldContactEmail = vendor.getContactEmail();
        String oldContactPhone = vendor.getContactPhone();
        String oldSupportEmail = vendor.getSupportEmail();

        // Check if new vendor name already exists (excluding current vendor)
        if (request.getVendorName() != null &&
                !request.getVendorName().equals(vendor.getVendorName()) &&
                vendorRepository.existsByVendorName(request.getVendorName())) {
            throw new ValidationException("Vendor with name '" + request.getVendorName() + "' already exists");
        }

        // Track if any changes were made
        boolean hasChanges = false;

        // Update fields
        if (request.getVendorName() != null && !request.getVendorName().equals(oldVendorName)) {
            vendor.setVendorName(request.getVendorName());
            hasChanges = true;
        }
        if (request.getContactEmail() != null && !request.getContactEmail().equals(oldContactEmail)) {
            vendor.setContactEmail(request.getContactEmail());
            hasChanges = true;
        }
        if (request.getContactPhone() != null && !request.getContactPhone().equals(oldContactPhone)) {
            vendor.setContactPhone(request.getContactPhone());
            hasChanges = true;
        }
        if (request.getSupportEmail() != null && !request.getSupportEmail().equals(oldSupportEmail)) {
            vendor.setSupportEmail(request.getSupportEmail());
            hasChanges = true;
        }

        // Only save if changes were made
        if (!hasChanges) {
            log.info("No changes detected for vendor: {}", id);
            return mapToResponse(vendor);
        }

        Vendor updatedVendor = vendorRepository.save(vendor);

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Create audit log with changes
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("vendorId", updatedVendor.getId());

            // Track specific changes
            Map<String, Map<String, String>> changes = new HashMap<>();

            if (!oldVendorName.equals(updatedVendor.getVendorName())) {
                changes.put("vendorName", Map.of(
                        "old", oldVendorName,
                        "new", updatedVendor.getVendorName()
                ));
            }

            if ((oldContactEmail == null && updatedVendor.getContactEmail() != null) ||
                    (oldContactEmail != null && !oldContactEmail.equals(updatedVendor.getContactEmail()))) {
                changes.put("contactEmail", Map.of(
                        "old", oldContactEmail != null ? oldContactEmail : "null",
                        "new", updatedVendor.getContactEmail() != null ? updatedVendor.getContactEmail() : "null"
                ));
            }

            if ((oldContactPhone == null && updatedVendor.getContactPhone() != null) ||
                    (oldContactPhone != null && !oldContactPhone.equals(updatedVendor.getContactPhone()))) {
                changes.put("contactPhone", Map.of(
                        "old", oldContactPhone != null ? oldContactPhone : "null",
                        "new", updatedVendor.getContactPhone() != null ? updatedVendor.getContactPhone() : "null"
                ));
            }

            if ((oldSupportEmail == null && updatedVendor.getSupportEmail() != null) ||
                    (oldSupportEmail != null && !oldSupportEmail.equals(updatedVendor.getSupportEmail()))) {
                changes.put("supportEmail", Map.of(
                        "old", oldSupportEmail != null ? oldSupportEmail : "null",
                        "new", updatedVendor.getSupportEmail() != null ? updatedVendor.getSupportEmail() : "null"
                ));
            }

            auditDetails.put("changes", changes);
            auditDetails.put("changeCount", changes.size());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.VENDOR,
                    updatedVendor.getId().toString(),
                    AuditAction.UPDATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for vendor update", e);
        }

        log.info("Vendor updated successfully: {}", updatedVendor.getId());

        return mapToResponse(updatedVendor);
    }

    @Override
    public VendorResponse getVendorById(Long id) {
        log.info("Fetching vendor with ID: {}", id);

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + id));

        return mapToResponse(vendor);
    }

    @Override
    public Page<VendorResponse> getAllVendors(Pageable pageable) {
        log.info("Fetching all vendors with pagination");

        return vendorRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<VendorResponse> getAllVendorsList() {
        log.info("Fetching all vendors as list");

        return vendorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteVendor(Long id) {
        log.info("Deleting vendor with ID: {}", id);

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + id));

        // Check if vendor is associated with any licenses
        long associatedLicensesCount = licenseRepository.countByVendorId(id);
        if (associatedLicensesCount > 0) {
            throw new ValidationException(
                    String.format("Cannot delete vendor '%s'. It is associated with %d license(s). " +
                                    "Please reassign or delete the licenses first.",
                            vendor.getVendorName(), associatedLicensesCount)
            );
        }

        // Store vendor info for audit before deletion
        String vendorName = vendor.getVendorName();
        String contactEmail = vendor.getContactEmail();
        String contactPhone = vendor.getContactPhone();
        String supportEmail = vendor.getSupportEmail();

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Delete the vendor
        vendorRepository.delete(vendor);

        // Create audit log
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("vendorId", id);
            auditDetails.put("vendorName", vendorName);
            auditDetails.put("contactEmail", contactEmail);
            auditDetails.put("contactPhone", contactPhone);
            auditDetails.put("supportEmail", supportEmail);
            auditDetails.put("hadAssociatedLicenses", false);
            auditDetails.put("associatedLicensesCount", 0);
            auditDetails.put("deletedAt", java.time.LocalDateTime.now().toString());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.VENDOR,
                    id.toString(),
                    AuditAction.DELETE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for vendor deletion", e);
        }

        log.info("Vendor deleted successfully: {}", id);
    }

    @Override
    public boolean existsByVendorName(String vendorName) {
        return vendorRepository.existsByVendorName(vendorName);
    }

    /**
     * Map vendor entity to response DTO
     *
     * @param vendor The vendor entity
     * @return VendorResponse DTO
     */
    private VendorResponse mapToResponse(Vendor vendor) {
        return VendorResponse.builder()
                .id(vendor.getId())
                .vendorName(vendor.getVendorName())
                .contactEmail(vendor.getContactEmail())
                .contactPhone(vendor.getContactPhone())
                .supportEmail(vendor.getSupportEmail())
                .build();
    }
}
