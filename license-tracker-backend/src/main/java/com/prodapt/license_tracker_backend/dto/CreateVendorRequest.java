package com.prodapt.license_tracker_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVendorRequest {

    @NotBlank(message = "Vendor name is required")
    private String vendorName;

    @Email(message = "Invalid contact email format")
    private String contactEmail;

    private String contactPhone;

    @Email(message = "Invalid support email format")
    private String supportEmail;
}
