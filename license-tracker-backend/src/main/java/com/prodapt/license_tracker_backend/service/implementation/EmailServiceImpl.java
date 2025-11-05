package com.prodapt.license_tracker_backend.service.implementation;

import com.prodapt.license_tracker_backend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        // In production, integrate with actual email service (SendGrid, AWS SES, etc.)
        // For now, we'll just log the reset link

        String resetLink = String.format("%s/reset-password?token=%s", frontendUrl, resetToken);

        log.info("===========================================");
        log.info("PASSWORD RESET EMAIL");
        log.info("===========================================");
        log.info("To: {}", toEmail);
        log.info("Username: {}", username);
        log.info("Reset Link: {}", resetLink);
        log.info("Token: {}", resetToken);
        log.info("===========================================");

        // TODO: Implement actual email sending
        /*
        Example with Spring Mail:

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - License Tracker");
        message.setText(String.format(
            "Hello %s,\n\n" +
            "You requested to reset your password.\n\n" +
            "Click the link below to reset your password:\n%s\n\n" +
            "This link will expire in 1 hour.\n\n" +
            "If you didn't request this, please ignore this email.\n\n" +
            "Best regards,\n" +
            "License Tracker Team",
            username, resetLink
        ));
        mailSender.send(message);
        */
    }
}
