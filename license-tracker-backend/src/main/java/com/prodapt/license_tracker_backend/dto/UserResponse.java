package com.prodapt.license_tracker_backend.dto;


import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String region;  // NEW FIELD
    private Boolean active;
    private Boolean passwordChangeRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

