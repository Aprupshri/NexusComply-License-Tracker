package com.prodapt.license_tracker_backend.dto;


import lombok.*;






@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String username;
    private String email;
    private String role;
    private String region;  // NEW FIELD
    private String fullName;
    private Boolean passwordChangeRequired;
}
