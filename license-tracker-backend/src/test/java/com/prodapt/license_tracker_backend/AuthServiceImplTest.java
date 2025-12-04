package com.prodapt.license_tracker_backend;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.prodapt.license_tracker_backend.dto.*;
import com.prodapt.license_tracker_backend.entities.User;
import com.prodapt.license_tracker_backend.entities.enums.AuditAction;
import com.prodapt.license_tracker_backend.entities.enums.EntityType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.entities.enums.UserRole;
import com.prodapt.license_tracker_backend.exception.ValidationException;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.security.jwt.JwtTokenUtil;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.EmailService;
import com.prodapt.license_tracker_backend.service.implementation.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    // Mocks for all dependencies
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenUtil jwtTokenUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ObjectMapper objectMapper;

    // Mocks for web context
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    // Class under test
    @InjectMocks
    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<String> auditDetailsCaptor;
    @Captor
    private ArgumentCaptor<AuditAction> auditActionCaptor;

    private User mockUser;

    // A real ObjectMapper for our mock to use
    private final ObjectMapper realObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setFullName("Test User");
        mockUser.setRole(UserRole.ADMIN);
        mockUser.setRegion(Region.CHENNAI);
        mockUser.setActive(true);
        mockUser.setPasswordChangeRequired(false);
        mockUser.setLastPasswordChange(LocalDateTime.now().minusMonths(2));
    }

    /**
     * Helper method to set up mocks for web context (IP address, User-Agent).
     * Called by any test that uses getRequestInfo().
     */
    private void setupMockWebContext() {
        // Mock the web context for getRequestInfo()
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(httpServletRequest.getHeader(anyString())).thenReturn(null); // Default for other headers
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    /**
     * Helper method to set up the ObjectMapper mock.
     * Called only by tests that create an audit log.
     */
    private void setupMockObjectMapper() throws JsonProcessingException {
        // Configure ObjectMapper mock to actually serialize the object
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation ->
                realObjectMapper.writeValueAsString(invocation.getArgument(0))
        );
    }

    // --- Login Tests ---

    @Test
    @DisplayName("login should succeed for active user")
    void login_Success() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        LoginRequest request = new LoginRequest("testuser", "password");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        when(jwtTokenUtil.generateToken(mockUser)).thenReturn("mock.token.string");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mock.token.string", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("ADMIN", response.getRole());
        assertFalse(response.getPasswordChangeRequired());

        verify(auditLogService, timeout(1000)).log(
                eq(1L),
                eq("testuser"),
                eq(EntityType.USER),
                eq("1"),
                eq(AuditAction.LOGIN),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"status\":\"SUCCESS\""));
    }

    @Test
    @DisplayName("login should fail if user not found (bad credentials)")
    void login_UserNotFound() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        LoginRequest request = new LoginRequest("notauser", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationException("Bad credentials"){});

        when(userRepository.findByUsername("notauser")).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.login(request);
        });

        assertEquals("Invalid username or password", exception.getMessage());

        verify(auditLogService, timeout(1000)).log(
                isNull(),
                eq("notauser"),
                eq(EntityType.USER),
                eq("UNKNOWN"),
                eq(AuditAction.LOGIN),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"failureReason\":\"INVALID_CREDENTIALS\""));
    }

    @Test
    @DisplayName("login should fail if user is deactivated")
    void login_UserDeactivated() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        mockUser.setActive(false);
        LoginRequest request = new LoginRequest("testuser", "password");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.login(request);
        });

        assertEquals("User account is deactivated", exception.getMessage());

        verify(auditLogService, timeout(1000)).log(
                eq(1L),
                eq("testuser"),
                eq(EntityType.USER),
                eq("1"),
                eq(AuditAction.LOGIN),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"failureReason\":\"ACCOUNT_DEACTIVATED\""));
    }

    // --- Forgot Password Tests ---

    @Test
    @DisplayName("forgotPassword should send email and save token for valid user")
    void forgotPassword_Success() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // Act
        PasswordResetResponse response = authService.forgotPassword(request);

        // Assert
        assertNotNull(response);
        assertEquals("Password reset link has been sent to your email", response.getMessage());
        assertEquals("t***@example.com", response.getEmail());

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getResetToken());
        assertTrue(savedUser.getResetTokenExpiry().isAfter(LocalDateTime.now()));

        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), eq("testuser"), anyString());

        verify(auditLogService, timeout(1000)).log(
                eq(1L),
                eq("testuser"),
                eq(EntityType.USER),
                eq("1"),
                eq(AuditAction.PASSWORD_CHANGE),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"action\":\"PASSWORD_RESET_REQUESTED\""));
    }

    @Test
    @DisplayName("forgotPassword should fail for unknown email")
    void forgotPassword_EmailNotFound() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        ForgotPasswordRequest request = new ForgotPasswordRequest("wrong@example.com");
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.forgotPassword(request);
        });

        assertEquals("No account found with this email address", exception.getMessage());

        verify(auditLogService, timeout(1000)).log(
                isNull(),
                eq("UNKNOWN (by email)"),
                eq(EntityType.USER),
                eq("UNKNOWN"),
                eq(AuditAction.PASSWORD_CHANGE),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"failureReason\":\"UNEXPECTED_ERROR\""));
    }

    @Test
    @DisplayName("forgotPassword should fail for deactivated user")
    void forgotPassword_UserDeactivated() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        mockUser.setActive(false);
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.forgotPassword(request);
        });

        assertEquals("User account is deactivated", exception.getMessage());

        verify(auditLogService, timeout(1000)).log(
                eq(1L),
                eq("testuser"),
                eq(EntityType.USER),
                eq("1"),
                eq(AuditAction.PASSWORD_CHANGE),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"failureReason\":\"ACCOUNT_DEACTIVATED\""));
    }

    // --- Reset Password Tests ---

    @Test
    @DisplayName("resetPassword should succeed with valid token and matching passwords")
    void resetPassword_Success() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        mockUser.setResetToken("valid-token");
        mockUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newPassword123", "newPassword123");

        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-password");

        // Act
        authService.resetPassword(request);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("encoded-password", savedUser.getPassword());
        assertNull(savedUser.getResetToken());
        assertFalse(savedUser.getPasswordChangeRequired());

        verify(auditLogService, timeout(1000)).log(
                eq(1L),
                eq("testuser"),
                eq(EntityType.USER),
                eq("1"),
                eq(AuditAction.PASSWORD_CHANGE),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"action\":\"PASSWORD_RESET_COMPLETED\""));
    }

    @Test
    @DisplayName("resetPassword should fail with invalid token")
    void resetPassword_InvalidToken() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newPassword1s23", "newPassword123");
        when(userRepository.findByResetToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("Invalid or expired reset token", exception.getMessage());

        verify(auditLogService, timeout(1000)).log(
                isNull(),
                eq("UNKNOWN (by token)"),
                any(), any(),
                eq(AuditAction.PASSWORD_CHANGE),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"failureReason\":\"UNEXPECTED_ERROR\""));
    }

    @Test
    @DisplayName("resetPassword should fail with expired token")
    void resetPassword_ExpiredToken() throws JsonProcessingException {
        // Arrange
        setupMockWebContext();
        setupMockObjectMapper();
        mockUser.setResetToken("expired-token");
        mockUser.setResetTokenExpiry(LocalDateTime.now().minusHours(1)); // Expired
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newPassword123", "newPassword123");

        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("Reset token has expired. Please request a new one.", exception.getMessage());

        verify(auditLogService, timeout(1000)).log(
                eq(1L),
                eq("testuser"),
                any(), any(),
                eq(AuditAction.PASSWORD_CHANGE),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("\"failureReason\":\"EXPIRED_RESET_TOKEN\""));
    }

    @Test
    @DisplayName("resetPassword should fail if passwords do not match")
    void resetPassword_PasswordMismatch() throws JsonProcessingException {
        // Arrange
        setupMockWebContext(); // Only this helper is needed, no audit log is created
        mockUser.setResetToken("valid-token");
        mockUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newPassword123", "MISMATCH");

        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("Passwords do not match", exception.getMessage());
        // Verify no password was encoded or saved
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Validate Token Tests ---
    // These tests don't need any helper setup

    @Test
    @DisplayName("validateResetToken should return true for valid token")
    void validateResetToken_Valid() {
        // Arrange
        mockUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(mockUser));

        // Act
        boolean isValid = authService.validateResetToken("valid-token");

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("validateResetToken should return false for expired token")
    void validateResetToken_Expired() {
        // Arrange
        mockUser.setResetTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(mockUser));

        // Act
        boolean isValid = authService.validateResetToken("expired-token");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("validateResetToken should return false for invalid token")
    void validateResetToken_Invalid() {
        // Arrange
        when(userRepository.findByResetToken("invalid-token")).thenReturn(Optional.empty());

        // Act
        boolean isValid = authService.validateResetToken("invalid-token");

        // Assert
        assertFalse(isValid);
    }
}