package com.prodapt.license_tracker_backend.service;


import com.prodapt.license_tracker_backend.dto.ChangePasswordRequest;
import com.prodapt.license_tracker_backend.dto.CreateUserRequest;
import com.prodapt.license_tracker_backend.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;



public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse assignRole(Long userId, String role);
    UserResponse activateUser(Long userId);
    UserResponse deactivateUser(Long userId);

    // NEW METHODS
    void changePassword(String username, ChangePasswordRequest request);
    UserResponse getCurrentUser(String username);
}

