package com.prodapt.license_tracker_backend.controller;

import com.prodapt.license_tracker_backend.scheduler.LicenseExpiryScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final LicenseExpiryScheduler licenseExpiryScheduler;

    @Operation(summary = "Manually trigger license expiry check",
            description = "Check and deactivate expired licenses immediately (Admin only)")
    @PostMapping("/check-expired-licenses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> checkExpiredLicenses() {
        licenseExpiryScheduler.checkExpiredLicensesManually();

        Map<String, String> response = new HashMap<>();
        response.put("message", "License expiry check completed successfully");
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}
