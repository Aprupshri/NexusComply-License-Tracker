package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.dto.AssignmentRequest;
import com.prodapt.license_tracker_backend.dto.AssignmentResponse;
import com.prodapt.license_tracker_backend.dto.RevokeAssignmentRequest;
import com.prodapt.license_tracker_backend.entities.Device;
import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.LicenseAssignment;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.exception.ValidationException;
import com.prodapt.license_tracker_backend.repository.DeviceRepository;
import com.prodapt.license_tracker_backend.repository.LicenseAssignmentRepository;
import com.prodapt.license_tracker_backend.repository.LicenseRepository;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.LicenseAssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseAssignmentServiceImpl implements LicenseAssignmentService {
    
    private static final String USERID="userId";
    private static final String USERNAME="username";
    private static final String LICENSE_KEY="licenseKey";
    private static final String SOFTWARE_NAME="softwareName";
    private static final String FAILURE_REASON="failureReason";
    private static final String STATUS="status";
    private static final String DEVICE_ID="deviceId";
    private static final String DEVICE_ID_NAME="deviceIdName";
    private static final String IP_ADDRESS="ipAddress";
    private static final String ASSIGNMENT_ID="assignmentId";
    private static final String LICENSE_ID="licenseId";
    private static final String FAILURE="FAILURE";
    
    
    private final LicenseAssignmentRepository assignmentRepository;
    private final DeviceRepository deviceRepository;
    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * Helper method to get current user information including IP address
     */
    private Map<String, Object> getCurrentUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                userInfo.put(USERNAME, username);

                Long userId = fetchUserId(username);
                userInfo.put(USERID, userId);
            }
        } catch (Exception e) {
            log.warn("Error getting current user info", e);
        }

        userInfo.putIfAbsent(USERNAME, "SYSTEM");
        userInfo.putIfAbsent(USERID, null);

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

    /**
     * Extract client IP address from request
     */

    @Override
    @Transactional
    public AssignmentResponse assignLicenseToDevice(AssignmentRequest request) {
        log.info("Assigning license {} to device {}", request.getLicenseId(), request.getDeviceId());

        // Get current user info early
        Map<String, Object> userInfo = getCurrentUserInfo();
        Long userId = (Long) userInfo.get(USERID);
        String username = (String) userInfo.get(USERNAME);
        String ipAddress = (String) userInfo.get(IP_ADDRESS);

        try {
            // Validate assignment eligibility
            Device device = validateAndFetchDevice(request.getDeviceId());
            License license = validateAndFetchLicense(request.getLicenseId());

            // Perform all validations
            validateAssignmentEligibility(device, license, request, userId, username, ipAddress);

            // Create assignment
            long currentUsage = assignmentRepository.countActiveAssignmentsByLicenseId(request.getLicenseId());
            LicenseAssignment savedAssignment = createAssignmentRecord(device, license, request);

            // Update license usage
            updateLicenseUsage(license, currentUsage);

            // Log successful assignment (no nested try-catch)
            logSuccessfulAssignment(savedAssignment, device, license, currentUsage, userId, username, ipAddress);

            log.info("License assigned successfully. Assignment ID: {}, Current usage: {}/{}",
                    savedAssignment.getId(), currentUsage + 1, license.getMaxUsage());

            return mapToResponse(savedAssignment);

        } catch (ResourceNotFoundException | ValidationException e) {
            // Already logged in validation checks
            throw e;
        } catch (Exception e) {
            // Log unexpected errors (no nested try-catch)
            logUnexpectedError(request, userId, username, e);
            throw e;
        }
    }

    // Extracted: Validate and fetch device
    private Device validateAndFetchDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
    }

    // Extracted: Validate and fetch license
    private License validateAndFetchLicense(Long licenseId) {
        return licenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found with id: " + licenseId));
    }

    // Extracted: Perform all assignment validations
    private void validateAssignmentEligibility(Device device, License license, AssignmentRequest request,
                                               Long userId, String username, String ipAddress) {
        // 1. Validate license is active
        if (Boolean.FALSE.equals(license.getActive())) {
            logFailedAssignment(userId, username, ipAddress, device, license,
                    "INACTIVE_LICENSE", "Cannot assign inactive license");
            throw new ValidationException("Cannot assign inactive license");
        }

        // 2. Check if license is expired
        if (license.getValidTo().isBefore(LocalDate.now())) {
            logFailedAssignment(userId, username, ipAddress, device, license,
                    "EXPIRED_LICENSE", "License expired on: " + license.getValidTo());
            throw new ValidationException("Cannot assign expired license. Expiry date: " + license.getValidTo());
        }

        // 3. Check if license is not yet valid
        if (license.getValidFrom().isAfter(LocalDate.now())) {
            logFailedAssignment(userId, username, ipAddress, device, license,
                    "NOT_YET_VALID", "License valid from: " + license.getValidFrom());
            throw new ValidationException("License is not yet valid. Valid from: " + license.getValidFrom());
        }

        // 4. Check if already assigned
        if (assignmentRepository.existsByDeviceIdAndLicenseIdAndActiveTrue(request.getDeviceId(), request.getLicenseId())) {
            logFailedAssignment(userId, username, ipAddress, device, license,
                    "ALREADY_ASSIGNED", "License already assigned to this device");
            throw new ValidationException("This license is already assigned to this device");
        }

        // 5. Check max usage limit (CRITICAL BUSINESS RULE)
        long currentUsage = assignmentRepository.countActiveAssignmentsByLicenseId(request.getLicenseId());
        if (currentUsage >= license.getMaxUsage()) {
            logFailedAssignment(userId, username, ipAddress, device, license,
                    "CAPACITY_EXCEEDED",
                    String.format("Usage: %d/%d", currentUsage, license.getMaxUsage()));
            throw new ValidationException(
                    String.format("License usage limit reached. Current usage: %d, Max allowed: %d. " +
                                    "Please revoke an existing assignment or contact procurement to increase license capacity.",
                            currentUsage, license.getMaxUsage())
            );
        }
    }

    // Extracted: Create assignment record
    private LicenseAssignment createAssignmentRecord(Device device, License license, AssignmentRequest request) {
        LicenseAssignment assignment = LicenseAssignment.builder()
                .device(device)
                .license(license)
                .assignedBy(request.getAssignedBy() != null ? request.getAssignedBy() : getCurrentUserInfo().get(USERNAME).toString())
                .active(true)
                .build();

        return assignmentRepository.save(assignment);
    }

    // Extracted: Update license usage
    private void updateLicenseUsage(License license, long currentUsage) {
        license.setCurrentUsage((int) (currentUsage + 1));
        licenseRepository.save(license);
    }

    // Extracted: Log successful assignment (no nested try-catch)
    private void logSuccessfulAssignment(LicenseAssignment savedAssignment, Device device, License license,
                                         long currentUsage, Long userId, String username, String ipAddress) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put(ASSIGNMENT_ID, savedAssignment.getId());

            // Device details
            auditDetails.put(DEVICE_ID, device.getId());
            auditDetails.put(DEVICE_ID_NAME, device.getDeviceId());
            auditDetails.put("deviceType", device.getDeviceType() != null ? device.getDeviceType().name() : null);
            auditDetails.put("deviceLocation", device.getLocation());
            auditDetails.put("deviceRegion", device.getRegion() != null ? device.getRegion().name() : null);

            // License details
            auditDetails.put(LICENSE_ID, license.getId());
            auditDetails.put(LICENSE_KEY, license.getLicenseKey());
            auditDetails.put(SOFTWARE_NAME, license.getSoftwareName());
            auditDetails.put("licenseType", license.getLicenseType() != null ? license.getLicenseType().name() : null);

            // Usage tracking
            long newUsage = currentUsage + 1;
            auditDetails.put("usageBeforeAssignment", currentUsage);
            auditDetails.put("usageAfterAssignment", newUsage);
            auditDetails.put("maxUsage", license.getMaxUsage());
            auditDetails.put("utilizationPercentage", (newUsage * 100.0) / license.getMaxUsage());

            // Assignment details
            auditDetails.put("assignedBy", savedAssignment.getAssignedBy());
            auditDetails.put("assignedOn", savedAssignment.getAssignedOn().toString());
            auditDetails.put(IP_ADDRESS, ipAddress);
            auditDetails.put(STATUS, "SUCCESS");

            auditLogService.log(
                    userId,
                    username,
                    EntityType.ASSIGNMENT,
                    savedAssignment.getId().toString(),
                    AuditAction.ASSIGN,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for successful assignment", e);
            // Don't throw - just log, as assignment was already successful
        }
    }

    // Extracted: Log unexpected error (no nested try-catch)
    private void logUnexpectedError(AssignmentRequest request, Long userId, String username, Exception e) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put(DEVICE_ID, request.getDeviceId());
            auditDetails.put(LICENSE_ID, request.getLicenseId());
            auditDetails.put(FAILURE_REASON, "UNEXPECTED_ERROR");
            auditDetails.put("errorMessage", e.getMessage());
            auditDetails.put(STATUS, FAILURE);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.ASSIGNMENT,
                    "FAILED_ASSIGNMENT",
                    AuditAction.ASSIGN,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception auditError) {
            log.error("Failed to create audit log for unexpected error", auditError);
            // Don't throw - let original exception propagate
        }
    }

    /**
     * Helper method to log failed assignment attempts
     */
    private void logFailedAssignment(Long userId, String username, String ipAddress,
                                     Device device, License license,
                                     String failureReason, String errorMessage) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put(DEVICE_ID, device.getId());
            auditDetails.put(DEVICE_ID_NAME, device.getDeviceId());
            auditDetails.put(LICENSE_ID, license.getId());
            auditDetails.put(LICENSE_KEY, license.getLicenseKey());
            auditDetails.put(SOFTWARE_NAME, license.getSoftwareName());
            auditDetails.put(FAILURE_REASON, failureReason);
            auditDetails.put("errorMessage", errorMessage);
            auditDetails.put(IP_ADDRESS, ipAddress);
            auditDetails.put(STATUS, FAILURE);
            auditDetails.put("attemptedBy", username);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.ASSIGNMENT,
                    "FAILED_ASSIGNMENT",
                    AuditAction.ASSIGN,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for failed assignment", e);
        }
    }

    @Override
    @Transactional
    public AssignmentResponse revokeAssignment(Long assignmentId, RevokeAssignmentRequest request) {
        log.info("Revoking assignment ID: {}", assignmentId);

        // Get current user info for audit logging
        Map<String, Object> userInfo = getCurrentUserInfo();
        Long userId = (Long) userInfo.get(USERID);
        String username = (String) userInfo.get(USERNAME);
        String ipAddress = (String) userInfo.get(IP_ADDRESS);

        // 1. Fetch assignment
        LicenseAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        // 2. Validate assignment is active
        if (Boolean.FALSE.equals(assignment.getActive())) {
            // Log failed revocation attempt
            try {
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put(ASSIGNMENT_ID, assignmentId);
                auditDetails.put(DEVICE_ID, assignment.getDevice().getId());
                auditDetails.put(LICENSE_ID, assignment.getLicense().getId());
                auditDetails.put(FAILURE_REASON, "ALREADY_REVOKED");
                auditDetails.put("revokedOn", assignment.getRevokedOn() != null ? assignment.getRevokedOn().toString() : null);
                auditDetails.put("revokedBy", assignment.getRevokedBy());
                auditDetails.put(STATUS, FAILURE);

                auditLogService.log(
                        userId,
                        username,
                        EntityType.ASSIGNMENT,
                        assignmentId.toString(),
                        AuditAction.UNASSIGN,
                        objectMapper.writeValueAsString(auditDetails)
                );
            } catch (Exception e) {
                log.error("Failed to create audit log for failed revocation", e);
            }

            throw new ValidationException("Assignment is already revoked");
        }

        Device device = assignment.getDevice();
        License license = assignment.getLicense();

        // Store values before revocation for audit
        LocalDateTime assignedOn = assignment.getAssignedOn();
        String assignedBy = assignment.getAssignedBy();
        long usageBeforeRevocation = assignmentRepository.countActiveAssignmentsByLicenseId(license.getId());

        // 3. Mark as revoked
        assignment.setActive(false);
        assignment.setRevokedOn(LocalDateTime.now());
        assignment.setRevokedBy(request.getRevokedBy() != null ? request.getRevokedBy() : username);
        assignment.setRevocationReason(request.getRevocationReason() != null ?
                request.getRevocationReason() : "No reason provided");

        LicenseAssignment savedAssignment = assignmentRepository.save(assignment);

        // 4. Update license current usage
        long usageAfterRevocation = assignmentRepository.countActiveAssignmentsByLicenseId(license.getId());
        license.setCurrentUsage((int) usageAfterRevocation);
        licenseRepository.save(license);

        // 5. Create audit log for successful revocation
        try {
            long assignmentDurationDays = java.time.temporal.ChronoUnit.DAYS.between(
                    assignedOn.toLocalDate(),
                    savedAssignment.getRevokedOn().toLocalDate()
            );

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put(ASSIGNMENT_ID, savedAssignment.getId());

            // Device details
            auditDetails.put(DEVICE_ID, device.getId());
            auditDetails.put(DEVICE_ID_NAME, device.getDeviceId());
            auditDetails.put("deviceType", device.getDeviceType() != null ? device.getDeviceType().name() : null);

            // License details
            auditDetails.put(LICENSE_ID, license.getId());
            auditDetails.put(LICENSE_KEY, license.getLicenseKey());
            auditDetails.put(SOFTWARE_NAME, license.getSoftwareName());

            // Usage tracking
            auditDetails.put("usageBeforeRevocation", usageBeforeRevocation);
            auditDetails.put("usageAfterRevocation", usageAfterRevocation);
            auditDetails.put("maxUsage", license.getMaxUsage());
            auditDetails.put("utilizationPercentage", (usageAfterRevocation * 100.0) / license.getMaxUsage());

            // Revocation details
            auditDetails.put("revokedBy", savedAssignment.getRevokedBy());
            auditDetails.put("revokedOn", savedAssignment.getRevokedOn().toString());
            auditDetails.put("revocationReason", savedAssignment.getRevocationReason());

            // Assignment history
            auditDetails.put("originallyAssignedBy", assignedBy);
            auditDetails.put("originallyAssignedOn", assignedOn.toString());
            auditDetails.put("assignmentDurationDays", assignmentDurationDays);

            auditDetails.put(IP_ADDRESS, ipAddress);
            auditDetails.put(STATUS, "SUCCESS");

            auditLogService.log(
                    userId,
                    username,
                    EntityType.ASSIGNMENT,
                    savedAssignment.getId().toString(),
                    AuditAction.UNASSIGN,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for successful revocation", e);
        }

        log.info("Assignment revoked successfully. Reason: {}, Current usage: {}/{}",
                assignment.getRevocationReason(), usageAfterRevocation, license.getMaxUsage());

        return mapToResponse(savedAssignment);
    }

    @Override
    public List<AssignmentResponse> getActiveAssignmentsByDevice(Long deviceId) {
        log.info("Fetching active assignments for device: {}", deviceId);
        List<LicenseAssignment> assignments = assignmentRepository.findByDeviceIdAndActiveTrue(deviceId);
        return assignments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getActiveAssignmentsByLicense(Long licenseId) {
        log.info("Fetching active assignments for license: {}", licenseId);
        List<LicenseAssignment> assignments = assignmentRepository.findByLicenseIdAndActiveTrue(licenseId);
        return assignments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAllActiveAssignments() {
        log.info("Fetching all active assignments");
        List<LicenseAssignment> assignments = assignmentRepository.findByActiveTrue();
        return assignments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAllAssignmentsByDevice(Long deviceId) {
        log.info("Fetching all assignments (including revoked) for device: {}", deviceId);
        List<LicenseAssignment> assignments = assignmentRepository.findByDeviceId(deviceId);
        return assignments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAllAssignmentsByLicense(Long licenseId) {
        log.info("Fetching all assignments (including revoked) for license: {}", licenseId);
        List<LicenseAssignment> assignments = assignmentRepository.findByLicenseId(licenseId);
        return assignments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AssignmentResponse getAssignmentById(Long id) {
        log.info("Fetching assignment: {}", id);
        LicenseAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
        return mapToResponse(assignment);
    }

    /**
     * Maps LicenseAssignment entity to AssignmentResponse DTO
     */
    private AssignmentResponse mapToResponse(LicenseAssignment assignment) {
        Device device = assignment.getDevice();
        License license = assignment.getLicense();

        return AssignmentResponse.builder()
                .id(assignment.getId())
                // Device info
                .deviceId(device.getId())
                .deviceIdName(device.getDeviceId())
                .deviceType(device.getDeviceType() != null ? device.getDeviceType().name() : null)
                .deviceLocation(device.getLocation())
                // License info
                .licenseId(license.getId())
                .licenseKey(license.getLicenseKey())
                .softwareName(license.getSoftwareName())
                // Assignment info
                .assignedOn(assignment.getAssignedOn())
                .assignedBy(assignment.getAssignedBy())
                .revokedOn(assignment.getRevokedOn())
                .revokedBy(assignment.getRevokedBy())
                .revocationReason(assignment.getRevocationReason())
                .active(assignment.getActive())
                .build();
    }
}
