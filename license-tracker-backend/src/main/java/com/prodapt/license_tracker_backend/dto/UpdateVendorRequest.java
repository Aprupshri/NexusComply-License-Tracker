package com.prodapt.license_tracker_backend.dto;

import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVendorRequest {

    private String vendorName;

    @Email(message = "Invalid contact email format")
    private String contactEmail;

    private String contactPhone;

    @Email(message = "Invalid support email format")
    private String supportEmail;
}
