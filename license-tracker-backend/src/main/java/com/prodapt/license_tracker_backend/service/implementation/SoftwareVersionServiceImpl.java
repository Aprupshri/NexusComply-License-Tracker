package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.dto.SoftwareVersionRequest;
import com.prodapt.license_tracker_backend.dto.SoftwareVersionResponse;
import com.prodapt.license_tracker_backend.dto.SoftwareVersionStatsResponse;
import com.prodapt.license_tracker_backend.entities.Device;
import com.prodapt.license_tracker_backend.entities.SoftwareVersion;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.entities.enums.SoftwareVersionStatus;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.exception.ValidationException;
import com.prodapt.license_tracker_backend.repository.DeviceRepository;
import com.prodapt.license_tracker_backend.repository.SoftwareVersionRepository;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.SoftwareVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoftwareVersionServiceImpl implements SoftwareVersionService {

    private final SoftwareVersionRepository softwareVersionRepository;
    private final DeviceRepository deviceRepository;
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
    public SoftwareVersionResponse createSoftwareVersion(SoftwareVersionRequest request) {
        log.info("Creating software version for device: {}", request.getDeviceId());

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + request.getDeviceId()));

        // Check if software version already exists for this device
        softwareVersionRepository.findByDeviceIdAndSoftwareName(request.getDeviceId(), request.getSoftwareName())
                .ifPresent(sv -> {
                    throw new ValidationException("Software version already exists for this device and software");
                });

        SoftwareVersionStatus status = determineStatus(request.getCurrentVersion(), request.getLatestVersion());

        SoftwareVersion softwareVersion = SoftwareVersion.builder()
                .device(device)
                .softwareName(request.getSoftwareName())
                .currentVersion(request.getCurrentVersion())
                .latestVersion(request.getLatestVersion())
                .status(status)
                .lastChecked(LocalDate.now())
                .notes(request.getNotes())
                .updateUrl(request.getUpdateUrl())
                .releaseDate(request.getReleaseDate())
                .build();

        SoftwareVersion saved = softwareVersionRepository.save(softwareVersion);

        // Create audit log
        try {
            Map<String, Object> userInfo = getCurrentUserInfo();
            Long userId = (Long) userInfo.get("userId");
            String username = (String) userInfo.get("username");

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("softwareVersionId", saved.getId());
            auditDetails.put("deviceId", device.getId());
            auditDetails.put("deviceIdName", device.getDeviceId());
            auditDetails.put("deviceType", device.getDeviceType() != null ? device.getDeviceType().name() : null);
            auditDetails.put("softwareName", saved.getSoftwareName());
            auditDetails.put("currentVersion", saved.getCurrentVersion());
            auditDetails.put("latestVersion", saved.getLatestVersion());
            auditDetails.put("status", saved.getStatus().name());
            auditDetails.put("updateRecommended", status == SoftwareVersionStatus.OUTDATED || status == SoftwareVersionStatus.CRITICAL);
            auditDetails.put("releaseDate", saved.getReleaseDate() != null ? saved.getReleaseDate().toString() : null);
            auditDetails.put("updateUrl", saved.getUpdateUrl());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.SOFTWARE_VERSION,
                    saved.getId().toString(),
                    AuditAction.CREATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for software version creation", e);
        }

        log.info("Software version created successfully with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public SoftwareVersionResponse updateSoftwareVersion(Long id, SoftwareVersionRequest request) {
        log.info("Updating software version: {}", id);

        SoftwareVersion softwareVersion = softwareVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software version not found with id: " + id));

        // Store old values for audit
        String oldSoftwareName = softwareVersion.getSoftwareName();
        String oldCurrentVersion = softwareVersion.getCurrentVersion();
        String oldLatestVersion = softwareVersion.getLatestVersion();
        SoftwareVersionStatus oldStatus = softwareVersion.getStatus();
        String oldNotes = softwareVersion.getNotes();
        String oldUpdateUrl = softwareVersion.getUpdateUrl();
        LocalDate oldReleaseDate = softwareVersion.getReleaseDate();

        // Update fields
        softwareVersion.setSoftwareName(request.getSoftwareName());
        softwareVersion.setCurrentVersion(request.getCurrentVersion());
        softwareVersion.setLatestVersion(request.getLatestVersion());

        SoftwareVersionStatus newStatus = determineStatus(request.getCurrentVersion(), request.getLatestVersion());
        softwareVersion.setStatus(newStatus);
        softwareVersion.setLastChecked(LocalDate.now());
        softwareVersion.setNotes(request.getNotes());
        softwareVersion.setUpdateUrl(request.getUpdateUrl());
        softwareVersion.setReleaseDate(request.getReleaseDate());

        SoftwareVersion updated = softwareVersionRepository.save(softwareVersion);

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Create audit log with changes
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("softwareVersionId", updated.getId());
            auditDetails.put("deviceId", updated.getDevice().getId());
            auditDetails.put("deviceIdName", updated.getDevice().getDeviceId());

            // Track changes
            Map<String, Map<String, Object>> changes = new HashMap<>();

            if (!oldSoftwareName.equals(request.getSoftwareName())) {
                changes.put("softwareName", Map.of("old", oldSoftwareName, "new", request.getSoftwareName()));
            }
            if (!oldCurrentVersion.equals(request.getCurrentVersion())) {
                changes.put("currentVersion", Map.of("old", oldCurrentVersion, "new", request.getCurrentVersion()));
            }
            if ((oldLatestVersion == null && request.getLatestVersion() != null) ||
                    (oldLatestVersion != null && !oldLatestVersion.equals(request.getLatestVersion()))) {
                changes.put("latestVersion", Map.of(
                        "old", oldLatestVersion != null ? oldLatestVersion : "null",
                        "new", request.getLatestVersion() != null ? request.getLatestVersion() : "null"
                ));
            }
            if (oldStatus != newStatus) {
                changes.put("status", Map.of("old", oldStatus.name(), "new", newStatus.name()));
            }
            if ((oldNotes == null && request.getNotes() != null) ||
                    (oldNotes != null && !oldNotes.equals(request.getNotes()))) {
                changes.put("notes", Map.of(
                        "old", oldNotes != null ? oldNotes : "null",
                        "new", request.getNotes() != null ? request.getNotes() : "null"
                ));
            }
            if ((oldUpdateUrl == null && request.getUpdateUrl() != null) ||
                    (oldUpdateUrl != null && !oldUpdateUrl.equals(request.getUpdateUrl()))) {
                changes.put("updateUrl", Map.of(
                        "old", oldUpdateUrl != null ? oldUpdateUrl : "null",
                        "new", request.getUpdateUrl() != null ? request.getUpdateUrl() : "null"
                ));
            }
            if ((oldReleaseDate == null && request.getReleaseDate() != null) ||
                    (oldReleaseDate != null && !oldReleaseDate.equals(request.getReleaseDate()))) {
                changes.put("releaseDate", Map.of(
                        "old", oldReleaseDate != null ? oldReleaseDate.toString() : "null",
                        "new", request.getReleaseDate() != null ? request.getReleaseDate().toString() : "null"
                ));
            }

            auditDetails.put("changes", changes);
            auditDetails.put("changeCount", changes.size());
            auditDetails.put("lastChecked", LocalDate.now().toString());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.SOFTWARE_VERSION,
                    updated.getId().toString(),
                    AuditAction.UPDATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for software version update", e);
        }

        log.info("Software version updated successfully: {}", updated.getId());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSoftwareVersion(Long id) {
        log.info("Deleting software version: {}", id);

        SoftwareVersion softwareVersion = softwareVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software version not found with id: " + id));

        // Store info for audit before deletion
        Long softwareVersionId = softwareVersion.getId();
        String deviceIdName = softwareVersion.getDevice().getDeviceId();
        Long deviceId = softwareVersion.getDevice().getId();
        String softwareName = softwareVersion.getSoftwareName();
        String currentVersion = softwareVersion.getCurrentVersion();
        String latestVersion = softwareVersion.getLatestVersion();
        SoftwareVersionStatus status = softwareVersion.getStatus();

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Delete the software version
        softwareVersionRepository.delete(softwareVersion);

        // Create audit log
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("softwareVersionId", softwareVersionId);
            auditDetails.put("deviceId", deviceId);
            auditDetails.put("deviceIdName", deviceIdName);
            auditDetails.put("softwareName", softwareName);
            auditDetails.put("currentVersion", currentVersion);
            auditDetails.put("latestVersion", latestVersion);
            auditDetails.put("status", status.name());
            auditDetails.put("deletedAt", java.time.LocalDateTime.now().toString());

            auditLogService.log(
                    userId,
                    username,
                    EntityType.SOFTWARE_VERSION,
                    softwareVersionId.toString(),
                    AuditAction.DELETE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for software version deletion", e);
        }

        log.info("Software version deleted successfully: {}", id);
    }

    @Override
    public SoftwareVersionResponse getSoftwareVersionById(Long id) {
        SoftwareVersion softwareVersion = softwareVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software version not found with id: " + id));
        return mapToResponse(softwareVersion);
    }

    @Override
    public Page<SoftwareVersionResponse> getAllSoftwareVersions(Pageable pageable) {
        return softwareVersionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<SoftwareVersionResponse> getSoftwareVersionsByDevice(Long deviceId) {
        List<SoftwareVersion> versions = softwareVersionRepository.findByDeviceId(deviceId);
        return versions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SoftwareVersionResponse> getSoftwareVersionsByStatus(String status) {
        SoftwareVersionStatus versionStatus = SoftwareVersionStatus.valueOf(status);
        List<SoftwareVersion> versions = softwareVersionRepository.findByStatus(versionStatus);
        return versions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SoftwareVersionStatsResponse getStatistics() {
        long total = softwareVersionRepository.count();
        long upToDate = softwareVersionRepository.countUpToDateVersions();
        long outdated = softwareVersionRepository.countOutdatedVersions();
        long critical = softwareVersionRepository.countCriticalVersions();
        long unknown = total - upToDate - outdated - critical;

        return SoftwareVersionStatsResponse.builder()
                .total(total)
                .upToDate(upToDate)
                .outdated(outdated)
                .critical(critical)
                .unknown(unknown)
                .build();
    }

    @Override
    @Transactional
    public SoftwareVersionResponse checkForUpdates(Long id) {
        log.info("Checking for updates for software version: {}", id);

        SoftwareVersion softwareVersion = softwareVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Software version not found with id: " + id));

        // Store old values
        SoftwareVersionStatus oldStatus = softwareVersion.getStatus();
        LocalDate oldLastChecked = softwareVersion.getLastChecked();

        // Update last checked date
        softwareVersion.setLastChecked(LocalDate.now());

        // Re-evaluate status
        SoftwareVersionStatus newStatus = oldStatus;
        if (softwareVersion.getLatestVersion() != null) {
            newStatus = determineStatus(
                    softwareVersion.getCurrentVersion(),
                    softwareVersion.getLatestVersion()
            );
            softwareVersion.setStatus(newStatus);
        }

        SoftwareVersion updated = softwareVersionRepository.save(softwareVersion);

        // Get current user info
        Map<String, Object> userInfo = getCurrentUserInfo();
        String username = (String) userInfo.get("username");
        Long userId = (Long) userInfo.get("userId");

        // Create audit log for update check
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "CHECK_FOR_UPDATES");
            auditDetails.put("softwareVersionId", updated.getId());
            auditDetails.put("deviceId", updated.getDevice().getId());
            auditDetails.put("deviceIdName", updated.getDevice().getDeviceId());
            auditDetails.put("softwareName", updated.getSoftwareName());
            auditDetails.put("currentVersion", updated.getCurrentVersion());
            auditDetails.put("latestVersion", updated.getLatestVersion());
            auditDetails.put("oldStatus", oldStatus.name());
            auditDetails.put("newStatus", newStatus.name());
            auditDetails.put("statusChanged", oldStatus != newStatus);
            auditDetails.put("lastChecked", updated.getLastChecked().toString());
            auditDetails.put("previousCheck", oldLastChecked != null ? oldLastChecked.toString() : null);

            auditLogService.log(
                    userId,
                    username,
                    EntityType.SOFTWARE_VERSION,
                    updated.getId().toString(),
                    AuditAction.UPDATE,
                    objectMapper.writeValueAsString(auditDetails)
            );
        } catch (Exception e) {
            log.error("Failed to create audit log for update check", e);
        }

        log.info("Software version check completed: {}", updated.getId());

        return mapToResponse(updated);
    }

    /**
     * Determine version status based on current and latest versions
     */
    private SoftwareVersionStatus determineStatus(String currentVersion, String latestVersion) {
        if (latestVersion == null || latestVersion.isEmpty()) {
            return SoftwareVersionStatus.UNKNOWN;
        }

        if (currentVersion.equals(latestVersion)) {
            return SoftwareVersionStatus.UP_TO_DATE;
        }

        // Simple version comparison - in production, use a proper version comparison library
        try {
            double current = parseVersion(currentVersion);
            double latest = parseVersion(latestVersion);
            double difference = latest - current;

            if (difference >= 2.0) {
                return SoftwareVersionStatus.CRITICAL;
            } else if (difference > 0) {
                return SoftwareVersionStatus.OUTDATED;
            } else {
                return SoftwareVersionStatus.UP_TO_DATE;
            }
        } catch (Exception e) {
            log.warn("Unable to compare versions: {} vs {}", currentVersion, latestVersion);
            return SoftwareVersionStatus.UNKNOWN;
        }
    }

    /**
     * Simple version parser - parses major.minor format
     * In production, use a proper version comparison library like maven-artifact
     */
    private double parseVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return Double.parseDouble(parts[0] + "." + parts[1]);
        } else if (parts.length == 1) {
            return Double.parseDouble(parts[0]);
        }
        throw new IllegalArgumentException("Invalid version format: " + version);
    }

    /**
     * Map entity to response DTO
     */
    private SoftwareVersionResponse mapToResponse(SoftwareVersion sv) {
        Device device = sv.getDevice();

        boolean updateRecommended = sv.getStatus() == SoftwareVersionStatus.OUTDATED ||
                sv.getStatus() == SoftwareVersionStatus.CRITICAL;

        String updateMessage = generateUpdateMessage(sv.getStatus(), sv.getCurrentVersion(), sv.getLatestVersion());

        return SoftwareVersionResponse.builder()
                .id(sv.getId())
                .deviceId(device.getId())
                .deviceIdName(device.getDeviceId())
                .deviceType(device.getDeviceType() != null ? device.getDeviceType().name() : null)
                .softwareName(sv.getSoftwareName())
                .currentVersion(sv.getCurrentVersion())
                .latestVersion(sv.getLatestVersion())
                .status(sv.getStatus().name())
                .lastChecked(sv.getLastChecked())
                .notes(sv.getNotes())
                .updateUrl(sv.getUpdateUrl())
                .releaseDate(sv.getReleaseDate())
                .updateRecommended(updateRecommended)
                .updateMessage(updateMessage)
                .build();
    }

    /**
     * Generate user-friendly update message
     */
    private String generateUpdateMessage(SoftwareVersionStatus status, String current, String latest) {
        switch (status) {
            case UP_TO_DATE:
                return "Software is up to date";
            case OUTDATED:
                return String.format("Update available: %s → %s", current, latest);
            case CRITICAL:
                return String.format("Critical update required: %s → %s", current, latest);
            case UNKNOWN:
            default:
                return "Unable to determine update status";
        }
    }
}
