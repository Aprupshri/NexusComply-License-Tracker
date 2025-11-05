package com.prodapt.license_tracker_backend.dto;


import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.entities.enums.UserRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private UserRole role;
    private Region region;
}
