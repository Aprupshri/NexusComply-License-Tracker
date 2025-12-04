package com.prodapt.license_tracker_backend.controller;


import com.prodapt.license_tracker_backend.dto.LicenseRequest;
import com.prodapt.license_tracker_backend.dto.LicenseResponse;
import com.prodapt.license_tracker_backend.service.implementation.LicenseServiceImpl;
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

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Licenses", description = "License management APIs")
@SecurityRequirement(name = "bearerAuth")
public class LicenseController {

    private final LicenseServiceImpl licenseService;

    @Operation(summary = "Get all licenses", description = "Retrieve paginated list of all licenses")
    @GetMapping
    public ResponseEntity<Page<LicenseResponse>> getAllLicenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<LicenseResponse> licenses = licenseService.getAllLicenses(pageable);
        return ResponseEntity.ok(licenses);
    }

    @Operation(summary = "Get license by ID", description = "Retrieve a specific license by its database ID")
    @GetMapping("/{id}")
    public ResponseEntity<LicenseResponse> getLicenseById(@PathVariable Long id) {
        LicenseResponse license = licenseService.getLicenseById(id);
        return ResponseEntity.ok(license);
    }

    @Operation(summary = "Get license by key", description = "Retrieve a specific license by its license key")
    @GetMapping("/by-key/{licenseKey}")
    public ResponseEntity<LicenseResponse> getLicenseByKey(@PathVariable String licenseKey) {
        LicenseResponse license = licenseService.getLicenseByKey(licenseKey);
        return ResponseEntity.ok(license);
    }

    @Operation(summary = "Create new license", description = "Add a new license to the system")
    @PostMapping
    public ResponseEntity<LicenseResponse> createLicense(@RequestBody LicenseRequest request) {
        LicenseResponse license = licenseService.createLicense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(license);
    }

    @Operation(summary = "Update license", description = "Update an existing license")
    @PutMapping("/{id}")
    public ResponseEntity<LicenseResponse> updateLicense(
            @PathVariable Long id,
            @RequestBody LicenseRequest request) {
        LicenseResponse license = licenseService.updateLicense(id, request);
        return ResponseEntity.ok(license);
    }

    @Operation(summary = "Delete license", description = "Delete a license from the system")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLicense(@PathVariable Long id) {
        licenseService.deleteLicense(id);
        return ResponseEntity.noContent().build();
    }
}
