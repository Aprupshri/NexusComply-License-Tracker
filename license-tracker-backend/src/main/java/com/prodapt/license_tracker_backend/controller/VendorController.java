package com.prodapt.license_tracker_backend.controller;

import com.prodapt.license_tracker_backend.dto.CreateVendorRequest;
import com.prodapt.license_tracker_backend.dto.UpdateVendorRequest;
import com.prodapt.license_tracker_backend.dto.VendorResponse;
import com.prodapt.license_tracker_backend.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor management APIs")
@SecurityRequirement(name = "bearerAuth")
public class VendorController {

    private final VendorService vendorService;

    @Operation(summary = "Create vendor", description = "Create a new vendor")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_LEAD')")
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        VendorResponse vendor = vendorService.createVendor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vendor);
    }

    @Operation(summary = "Get all vendors", description = "Retrieve paginated list of vendors")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<VendorResponse>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "vendorName") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<VendorResponse> vendors = vendorService.getAllVendors(pageable);
        return ResponseEntity.ok(vendors);
    }

    @Operation(summary = "Get all vendors list", description = "Retrieve all vendors without pagination")
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VendorResponse>> getAllVendorsList() {
        List<VendorResponse> vendors = vendorService.getAllVendorsList();
        return ResponseEntity.ok(vendors);
    }

    @Operation(summary = "Get vendor by ID", description = "Retrieve vendor details by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long id) {
        VendorResponse vendor = vendorService.getVendorById(id);
        return ResponseEntity.ok(vendor);
    }

    @Operation(summary = "Update vendor", description = "Update vendor details")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_LEAD')")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVendorRequest request) {
        VendorResponse vendor = vendorService.updateVendor(id, request);
        return ResponseEntity.ok(vendor);
    }

    @Operation(summary = "Delete vendor", description = "Delete a vendor")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Vendor deleted successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Check vendor name exists", description = "Check if vendor name already exists")
    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_LEAD')")
    public ResponseEntity<Map<String, Boolean>> checkVendorNameExists(@RequestParam String vendorName) {
        boolean exists = vendorService.existsByVendorName(vendorName);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}
