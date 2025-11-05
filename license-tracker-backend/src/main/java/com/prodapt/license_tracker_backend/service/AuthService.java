package com.prodapt.license_tracker_backend.service;



import com.prodapt.license_tracker_backend.dto.*;


public interface AuthService {
    LoginResponse login(LoginRequest request);
    PasswordResetResponse forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    boolean validateResetToken(String token);
}
