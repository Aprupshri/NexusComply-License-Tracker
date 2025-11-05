package com.prodapt.license_tracker_backend.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.dto.*;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.exception.ValidationException;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.security.jwt.JwtTokenUtil;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.AuthService;
import com.prodapt.license_tracker_backend.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        Map<String, String> requestInfo = getRequestInfo();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ValidationException("User not found"));

            if (!user.getActive()) {
                logAuthFailure(user.getId(), request.getUsername(), "ACCOUNT_DEACTIVATED", "User account is deactivated", AuditAction.LOGIN, requestInfo);
                throw new ValidationException("User account is deactivated");
            }

            String token = jwtTokenUtil.generateToken(user);
            logSuccessfulLogin(user, requestInfo);

            log.info("Login successful for user: {}", request.getUsername());

            return LoginResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .region(user.getRegion().name())
                    .fullName(user.getFullName())
                    .passwordChangeRequired(user.getPasswordChangeRequired())
                    .build();

        } catch (AuthenticationException e) {
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            Long userId = user != null ? user.getId() : null;
            logAuthFailure(userId, request.getUsername(), "INVALID_CREDENTIALS", "Invalid username or password", AuditAction.LOGIN, requestInfo);
            log.warn("Login failed for user: {} from IP: {}", request.getUsername(), requestInfo.get("ipAddress"));
            throw new ValidationException("Invalid username or password");
        } catch (Exception e) {
            logAuthFailure(null, request.getUsername(), "UNEXPECTED_ERROR", e.getMessage(), AuditAction.LOGIN, requestInfo);
            throw e; // Re-throw the original exception
        }
    }

    @Override
    @Transactional
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset requested for email: {}", maskEmail(request.getEmail()));
        Map<String, String> requestInfo = getRequestInfo();

        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ValidationException("No account found with this email address"));

            if (!user.getActive()) {
                logAuthFailure(user.getId(), user.getUsername(), "ACCOUNT_DEACTIVATED", "Cannot reset password for deactivated account", AuditAction.PASSWORD_CHANGE, requestInfo);
                throw new ValidationException("User account is deactivated");
            }

            String resetToken = UUID.randomUUID().toString();
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS);

            user.setResetToken(resetToken);
            user.setResetTokenExpiry(expiryTime);
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);
            logPasswordResetRequest(user, expiryTime, requestInfo);

            log.info("Password reset email sent to: {}", maskEmail(request.getEmail()));

            return PasswordResetResponse.builder()
                    .message("Password reset link has been sent to your email")
                    .email(maskEmail(user.getEmail()))
                    // The resetToken is NOT returned for security reasons.
                    .build();
        } catch (Exception e) {
            logAuthFailure(null, "UNKNOWN (by email)", "UNEXPECTED_ERROR", e.getMessage(), AuditAction.PASSWORD_CHANGE, requestInfo);
            throw e;
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset attempt with token");
        Map<String, String> requestInfo = getRequestInfo();

        try {
            User user = userRepository.findByResetToken(request.getToken())
                    .orElseThrow(() -> new ValidationException("Invalid or expired reset token"));

            if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                logAuthFailure(user.getId(), user.getUsername(), "EXPIRED_RESET_TOKEN", "Reset token has expired", AuditAction.PASSWORD_CHANGE, requestInfo);
                throw new ValidationException("Reset token has expired. Please request a new one.");
            }

            // Consider moving this validation to the DTO with annotations
            if (request.getNewPassword().length() < 6) {
                throw new ValidationException("Password must be at least 6 characters long");
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new ValidationException("Passwords do not match");
            }

            LocalDateTime previousPasswordChange = user.getLastPasswordChange();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            user.setPasswordChangeRequired(false);
            user.setLastPasswordChange(LocalDateTime.now());
            userRepository.save(user);

            logSuccessfulPasswordReset(user, previousPasswordChange, requestInfo);
            log.info("Password reset successful for user: {}", user.getUsername());

        } catch (Exception e) {
            logAuthFailure(null, "UNKNOWN (by token)", "UNEXPECTED_ERROR", e.getMessage(), AuditAction.PASSWORD_CHANGE, requestInfo);
            throw e;
        }
    }

    @Override
    public boolean validateResetToken(String token) {
        return userRepository.findByResetToken(token)
                .map(user -> user.getResetTokenExpiry().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    // --- Private Helper Methods ---

    private void logSuccessfulLogin(User user, Map<String, String> requestInfo) {
        try {
            Map<String, Object> details = new HashMap<>(Map.of(
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "region", user.getRegion().name(),
                    "status", "SUCCESS",
                    "passwordChangeRequired", user.getPasswordChangeRequired(),
                    "lastPasswordChange", user.getLastPasswordChange() != null ? user.getLastPasswordChange().toString() : "Never",
                    "loginAt", LocalDateTime.now().toString(),
                    "tokenGeneratedAt", LocalDateTime.now().toString()
            ));
            details.putAll(requestInfo);

            auditLogService.log(user.getId(), user.getUsername(), EntityType.USER, user.getId().toString(),
                    AuditAction.LOGIN, objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.error("Failed to create audit log for successful login for user {}", user.getUsername(), e);
        }
    }

    private void logPasswordResetRequest(User user, LocalDateTime expiryTime, Map<String, String> requestInfo) {
        try {
            Map<String, Object> details = new HashMap<>(Map.of(
                    "username", user.getUsername(),
                    "email", maskEmail(user.getEmail()),
                    "action", "PASSWORD_RESET_REQUESTED",
                    "status", "SUCCESS",
                    "resetTokenGenerated", true,
                    "tokenExpiryHours", RESET_TOKEN_EXPIRY_HOURS,
                    "expiryTime", expiryTime.toString(),
                    "emailSent", true,
                    "requestedAt", LocalDateTime.now().toString()
            ));
            details.putAll(requestInfo);

            auditLogService.log(user.getId(), user.getUsername(), EntityType.USER, user.getId().toString(),
                    AuditAction.PASSWORD_CHANGE, objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.error("Failed to create audit log for password reset request for user {}", user.getUsername(), e);
        }
    }

    private void logSuccessfulPasswordReset(User user, LocalDateTime previousPasswordChange, Map<String, String> requestInfo) {
        try {
            Map<String, Object> details = new HashMap<>(Map.of(
                    "username", user.getUsername(),
                    "action", "PASSWORD_RESET_COMPLETED",
                    "status", "SUCCESS",
                    "passwordChangeRequired", false,
                    "previousPasswordChange", previousPasswordChange != null ? previousPasswordChange.toString() : "Never",
                    "newPasswordSetAt", user.getLastPasswordChange().toString(),
                    "tokenCleared", true,
                    "completedAt", LocalDateTime.now().toString()
            ));
            details.putAll(requestInfo);

            auditLogService.log(user.getId(), user.getUsername(), EntityType.USER, user.getId().toString(),
                    AuditAction.PASSWORD_CHANGE, objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.error("Failed to create audit log for successful password reset for user {}", user.getUsername(), e);
        }
    }

    /**
     * Centralized method to log any authentication/authorization failure.
     */
    private void logAuthFailure(Long userId, String username, String failureReason, String message, AuditAction action, Map<String, String> requestInfo) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("username", username);
            auditDetails.put("status", "FAILED");
            auditDetails.put("failureReason", failureReason);
            auditDetails.put("message", message);
            auditDetails.put("attemptedAt", LocalDateTime.now().toString());
            auditDetails.putAll(requestInfo);

            String entityId = userId != null ? userId.toString() : "UNKNOWN";

            auditLogService.log(userId, username, EntityType.USER, entityId, action, objectMapper.writeValueAsString(auditDetails));
        } catch (Exception auditError) {
            log.error("CRITICAL: Failed to create audit log for a security event [Action: {}, Reason: {}]", action, failureReason, auditError);
        }
    }

    private Map<String, String> getRequestInfo() {
        Map<String, String> requestInfo = new HashMap<>();
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                requestInfo.put("ipAddress", getClientIpAddress(request));
                requestInfo.put("userAgent", request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("Could not extract request information, likely running in a non-web context.");
        }
        requestInfo.putIfAbsent("ipAddress", "UNKNOWN");
        requestInfo.putIfAbsent("userAgent", "UNKNOWN");
        return requestInfo;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED",
                "HTTP_VIA", "REMOTE_ADDR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "invalid-email";
        }
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        if (localPart.isEmpty()) {
            return "***@" + parts[1];
        }
        return localPart.charAt(0) + "***@" + parts[1];
    }
}