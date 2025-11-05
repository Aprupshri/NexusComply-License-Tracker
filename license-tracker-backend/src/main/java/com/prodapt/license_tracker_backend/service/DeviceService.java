package com.prodapt.license_tracker_backend.service;


import com.prodapt.license_tracker_backend.dto.BulkUploadResult;
import com.prodapt.license_tracker_backend.dto.DeviceRequest;
import com.prodapt.license_tracker_backend.dto.DeviceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;

public interface DeviceService {
    DeviceResponse createDevice(DeviceRequest request);
    Page<DeviceResponse> getAllDevices(Pageable pageable);
    DeviceResponse getDeviceById(Long id);
    DeviceResponse getDeviceByDeviceId(String deviceId);
    DeviceResponse updateDevice(Long id, DeviceRequest request);
    void deleteDevice(Long id);

    @Transactional
    BulkUploadResult bulkUploadDevices(MultipartFile file) throws IOException;

    byte[] generateDeviceTemplate() throws IOException;
}
