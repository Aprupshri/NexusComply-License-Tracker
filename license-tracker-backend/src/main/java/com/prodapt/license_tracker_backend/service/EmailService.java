package com.prodapt.license_tracker_backend.service;


public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String username, String resetToken);
}
