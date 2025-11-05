package com.prodapt.license_tracker_backend.service;


import com.prodapt.license_tracker_backend.dto.LicenseRequest;
import com.prodapt.license_tracker_backend.dto.LicenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LicenseService {
    LicenseResponse createLicense(LicenseRequest request);
    Page<LicenseResponse> getAllLicenses(Pageable pageable);
    LicenseResponse getLicenseById(Long id);
    LicenseResponse getLicenseByKey(String licenseKey);
    LicenseResponse updateLicense(Long id, LicenseRequest request);
    void deleteLicense(Long id);
}
