package com.prodapt.license_tracker_backend.service;


import com.prodapt.license_tracker_backend.dto.SoftwareVersionRequest;
import com.prodapt.license_tracker_backend.dto.SoftwareVersionResponse;
import com.prodapt.license_tracker_backend.dto.SoftwareVersionStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SoftwareVersionService {
    SoftwareVersionResponse createSoftwareVersion(SoftwareVersionRequest request);
    SoftwareVersionResponse updateSoftwareVersion(Long id, SoftwareVersionRequest request);
    void deleteSoftwareVersion(Long id);
    SoftwareVersionResponse getSoftwareVersionById(Long id);
    Page<SoftwareVersionResponse> getAllSoftwareVersions(Pageable pageable);
    List<SoftwareVersionResponse> getSoftwareVersionsByDevice(Long deviceId);
    List<SoftwareVersionResponse> getSoftwareVersionsByStatus(String status);
    SoftwareVersionStatsResponse getStatistics();
    SoftwareVersionResponse checkForUpdates(Long id);
}
