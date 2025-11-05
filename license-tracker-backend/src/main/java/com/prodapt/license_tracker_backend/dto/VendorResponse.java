package com.prodapt.license_tracker_backend.dto;



import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorResponse {
    private Long id;
    private String vendorName;
    private String contactEmail;
    private String contactPhone;
    private String supportEmail;
}
