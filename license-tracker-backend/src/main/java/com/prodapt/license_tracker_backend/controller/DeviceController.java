package com.prodapt.license_tracker_backend.controller;

import com.prodapt.license_tracker_backend.dto.BulkUploadResult;
import com.prodapt.license_tracker_backend.dto.DeviceRequest;
import com.prodapt.license_tracker_backend.dto.DeviceResponse;
import com.prodapt.license_tracker_backend.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Device management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Get all devices", description = "Retrieve paginated list of all devices")
    @GetMapping
    public ResponseEntity<Page<DeviceResponse>> getAllDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<DeviceResponse> devices = deviceService.getAllDevices(pageable);
        return ResponseEntity.ok(devices);
    }

    @Operation(summary = "Get device by ID", description = "Retrieve a specific device by its database ID")
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable Long id) {
        DeviceResponse device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(device);
    }

    @Operation(summary = "Get device by Device ID", description = "Retrieve a specific device by its device ID")
    @GetMapping("/by-device-id/{deviceId}")
    public ResponseEntity<DeviceResponse> getDeviceByDeviceId(@PathVariable String deviceId) {
        DeviceResponse device = deviceService.getDeviceByDeviceId(deviceId);
        return ResponseEntity.ok(device);
    }

    @Operation(summary = "Create new device", description = "Add a new device to the system")
    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@RequestBody DeviceRequest request) {
        DeviceResponse device = deviceService.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    @Operation(summary = "Update device", description = "Update an existing device")
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(
            @PathVariable Long id,
            @RequestBody DeviceRequest request) {
        DeviceResponse device = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(device);
    }

    @Operation(summary = "Delete device", description = "Delete a device from the system")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Bulk upload devices", description = "Upload multiple devices via CSV file")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResult> bulkUploadDevices(
            @RequestParam("file") MultipartFile file) throws IOException {
        BulkUploadResult result = deviceService.bulkUploadDevices(file);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Download device template", description = "Download CSV template for bulk device upload")
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] template = deviceService.generateDeviceTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "device_upload_template.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(template);
    }
}
