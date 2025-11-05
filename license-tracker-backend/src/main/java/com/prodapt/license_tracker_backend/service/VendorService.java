package com.prodapt.license_tracker_backend.service;


import com.prodapt.license_tracker_backend.dto.CreateVendorRequest;
import com.prodapt.license_tracker_backend.dto.UpdateVendorRequest;
import com.prodapt.license_tracker_backend.dto.VendorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorService {
    VendorResponse createVendor(CreateVendorRequest request);
    VendorResponse updateVendor(Long id, UpdateVendorRequest request);
    VendorResponse getVendorById(Long id);
    Page<VendorResponse> getAllVendors(Pageable pageable);
    List<VendorResponse> getAllVendorsList();
    void deleteVendor(Long id);
    boolean existsByVendorName(String vendorName);
}
