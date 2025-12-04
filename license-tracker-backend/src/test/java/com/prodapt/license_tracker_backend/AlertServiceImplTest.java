package com.prodapt.license_tracker_backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodapt.license_tracker_backend.dto.AlertResponse;
import com.prodapt.license_tracker_backend.dto.AlertStatsResponse;
import com.prodapt.license_tracker_backend.entities.*;
import com.prodapt.license_tracker_backend.entities.enums.*;
import com.prodapt.license_tracker_backend.exception.ResourceNotFoundException;
import com.prodapt.license_tracker_backend.repository.AlertRepository;
import com.prodapt.license_tracker_backend.repository.LicenseRepository;
import com.prodapt.license_tracker_backend.repository.SoftwareVersionRepository;
import com.prodapt.license_tracker_backend.repository.UserRepository;
import com.prodapt.license_tracker_backend.service.AuditLogService;
import com.prodapt.license_tracker_backend.service.implementation.AlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Use LENIENT strictness to avoid issues with unnecessary stubs in simpler tests
// This is an alternative to moving the objectMapper stub, but moving it is cleaner.
// For this solution, we will stick to the cleaner approach of moving the stub.
@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    // Mocks for all dependencies
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private SoftwareVersionRepository softwareVersionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ObjectMapper objectMapper;

    // The class we are testing
    @InjectMocks
    private AlertServiceImpl alertService;

    // Argument captors to inspect objects passed to mocks
    @Captor
    private ArgumentCaptor<Alert> alertCaptor;
    @Captor
    private ArgumentCaptor<String> auditDetailsCaptor;
    @Captor
    private ArgumentCaptor<Long> auditUserIdCaptor;
    @Captor
    private ArgumentCaptor<String> auditUsernameCaptor;

    // Mock objects for reuse in tests
    private Alert mockAlert;
    private License mockLicense;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // Initialize mock objects
        mockAlert = Alert.builder()
                .id(1L)
                .alertType(AlertType.LICENSE_EXPIRING)
                .severity(Severity.CRITICAL)
                .message("Test Alert")
                .region(Region.CHENNAI) // Using your specified region
                .generatedAt(LocalDateTime.now().minusDays(1))
                .acknowledged(false)
                .build();

        mockLicense = new License();
        mockLicense.setLicenseKey("TEST-KEY-123");
        mockLicense.setSoftwareName("TestSoftware");
        mockLicense.setValidTo(LocalDate.now().plusDays(10)); // Expires in 10 days
        mockLicense.setRegion(Region.CHENNAI); // Using your specified region
        mockLicense.setActive(true);
        mockLicense.setCurrentUsage(95);
        mockLicense.setMaxUsage(100);

        mockUser = new User();
        mockUser.setId(42L);
        mockUser.setUsername("test-user");

        // **FIX 3**: Removed objectMapper stub from here
    }

    // Helper method to mock Spring Security context
    private void setupMockSecurityContext(String username, Long userId) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    // Helper method to mock the alertRepository.save() call
    private void mockAlertSave() {
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alertToSave = invocation.getArgument(0);
            if (alertToSave.getId() == null) {
                alertToSave.setId(99L); // Assign a mock ID if it's a new alert
            }
            return alertToSave;
        });
    }

    // Helper method to mock the objectMapper call
    private void mockObjectMapper() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"mocked\":\"json\"}");
    }


    @Test
    @DisplayName("getAlertById should return AlertResponse when found")
    void getAlertById_Success() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));

        // Act
        AlertResponse response = alertService.getAlertById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Alert", response.getMessage());
        assertEquals("CRITICAL", response.getSeverity());
        assertEquals("⚠️", response.getIcon());
        assertEquals("danger", response.getColor());
    }

    @Test
    @DisplayName("getAlertById should throw ResourceNotFoundException when not found")
    void getAlertById_NotFound() {
        // Arrange
        when(alertRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            alertService.getAlertById(99L);
        });
    }

    @Test
    @DisplayName("getStatistics should return correct counts")
    void getStatistics_Success() {
        // Arrange
        when(alertRepository.count()).thenReturn(100L);
        when(alertRepository.countUnacknowledged()).thenReturn(20L);
        when(alertRepository.countUnacknowledgedBySeverity(Severity.CRITICAL)).thenReturn(5L);
        when(alertRepository.countUnacknowledgedBySeverity(Severity.HIGH)).thenReturn(10L);
        when(alertRepository.countUnacknowledgedBySeverity(Severity.MEDIUM)).thenReturn(3L);
        when(alertRepository.countUnacknowledgedBySeverity(Severity.LOW)).thenReturn(2L);

        // Act
        AlertStatsResponse stats = alertService.getStatistics();

        // Assert
        assertEquals(100L, stats.getTotal());
        assertEquals(20L, stats.getUnacknowledged());
        assertEquals(5L, stats.getCritical());
        assertEquals(10L, stats.getHigh());
        assertEquals(3L, stats.getMedium());
        assertEquals(2L, stats.getLow());
    }

    @Test
    @DisplayName("acknowledgeAlert should set fields, save, and create audit log")
    void acknowledgeAlert_Success() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub
        setupMockSecurityContext("test-user", 42L);

        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));
        // Use the helper for save mock
        mockAlertSave();

        // Act
        AlertResponse response = alertService.acknowledgeAlert(1L, "test-user");

        // Assert
        assertTrue(response.getAcknowledged());
        assertEquals("test-user", response.getAcknowledgedBy());
        assertNotNull(response.getAcknowledgedAt());

        verify(alertRepository).save(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue();

        assertTrue(savedAlert.getAcknowledged());
        assertEquals("test-user", savedAlert.getAcknowledgedBy());

        verify(auditLogService).log(
                eq(42L),
                eq("test-user"),
                eq(EntityType.ALERT),
                eq("1"),
                eq(AuditAction.ACKNOWLEDGE),
                eq("{\"mocked\":\"json\"}")
        );
    }

    @Test
    @DisplayName("acknowledgeAlert should use SYSTEM user when no authentication is present")
    void acknowledgeAlert_SystemUser_Success() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));
        mockAlertSave(); // **FIX 1**: Added save stub

        // Act
        alertService.acknowledgeAlert(1L, "ManualAcknowledge");

        // Assert
        verify(auditLogService).log(
                isNull(), // UserID should be null
                eq("SYSTEM"), // Username should be SYSTEM
                eq(EntityType.ALERT),
                eq("1"),
                eq(AuditAction.ACKNOWLEDGE),
                anyString()
        );
    }

    @Test
    @DisplayName("generateLicenseExpiryAlerts should create new alert for expiring license")
    void generateLicenseExpiryAlerts_CreatesNewAlert() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub
        mockAlertSave(); // **FIX 1**: Added save stub

        when(licenseRepository.findByActiveTrueAndValidToBetween(any(), any())).thenReturn(List.of(mockLicense));
        when(alertRepository.findByAlertTypeAndMessageContaining(any(), anyString())).thenReturn(Collections.emptyList());

        // Act
        alertService.generateLicenseExpiryAlerts();

        // Assert
        verify(alertRepository).save(alertCaptor.capture());
        Alert newAlert = alertCaptor.getValue();

        assertEquals(AlertType.LICENSE_EXPIRING, newAlert.getAlertType());
        assertEquals(Severity.CRITICAL, newAlert.getSeverity()); // 10 days <= 15 days
        assertTrue(newAlert.getMessage().contains("expires in 10 days"));
        assertEquals(99L, newAlert.getId()); // Check that our mock ID was set

        verify(auditLogService).log(
                isNull(),
                eq("SYSTEM_SCHEDULER"),
                eq(EntityType.ALERT),
                eq("SCHEDULED_CHECK"),
                eq(AuditAction.CREATE),
                auditDetailsCaptor.capture()
        );

        assertEquals("{\"mocked\":\"json\"}", auditDetailsCaptor.getValue());
    }

    @Test
    @DisplayName("generateLicenseExpiryAlerts should not create alert if recent one exists")
    void generateLicenseExpiryAlerts_SkipsRecentAlert() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub (for the audit log)

        mockAlert.setGeneratedAt(LocalDateTime.now().minusDays(1));
        mockAlert.setAcknowledged(false);

        when(licenseRepository.findByActiveTrueAndValidToBetween(any(), any())).thenReturn(List.of(mockLicense));
        when(alertRepository.findByAlertTypeAndMessageContaining(any(), eq("TEST-KEY-123")))
                .thenReturn(List.of(mockAlert));

        // Act
        alertService.generateLicenseExpiryAlerts();

        // Assert
        verify(alertRepository, never()).save(any());

        verify(auditLogService).log(
                isNull(),
                eq("SYSTEM_SCHEDULER"),
                any(),
                any(),
                any(),
                anyString()
        );
    }

    @Test
    @DisplayName("generateSoftwareVersionAlerts should create alert for critical version")
    void generateSoftwareVersionAlerts_CreatesNewAlert() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub
        mockAlertSave(); // **FIX 1**: Added save stub

        Device mockDevice = new Device();
        mockDevice.setDeviceId("DEVICE-001");
        mockDevice.setRegion(Region.MUMBAI); // Using your specified region

        SoftwareVersion criticalVersion = new SoftwareVersion();
        criticalVersion.setDevice(mockDevice);
        criticalVersion.setSoftwareName("Firmware");
        criticalVersion.setCurrentVersion("1.0.0");
        criticalVersion.setLatestVersion("2.0.0");
        criticalVersion.setStatus(SoftwareVersionStatus.CRITICAL);

        // **FIX 2**: Added stub for the OUTDATED call
        when(softwareVersionRepository.findByStatus(SoftwareVersionStatus.OUTDATED)).thenReturn(Collections.emptyList());
        when(softwareVersionRepository.findByStatus(SoftwareVersionStatus.CRITICAL)).thenReturn(List.of(criticalVersion));

        when(alertRepository.findByAlertTypeAndMessageContaining(any(), anyString())).thenReturn(Collections.emptyList());

        // Act
        alertService.generateSoftwareVersionAlerts();

        // Assert
        verify(alertRepository).save(alertCaptor.capture());
        Alert newAlert = alertCaptor.getValue();

        assertEquals(AlertType.SOFTWARE_VERSION_CRITICAL, newAlert.getAlertType());
        assertEquals(Severity.CRITICAL, newAlert.getSeverity());
        assertEquals(Region.MUMBAI, newAlert.getRegion());
        assertTrue(newAlert.getMessage().contains("DEVICE-001"));

        verify(auditLogService).log(
                isNull(),
                eq("SYSTEM_SCHEDULER"),
                eq(EntityType.ALERT),
                eq("SCHEDULED_CHECK"),
                eq(AuditAction.CREATE),
                anyString()
        );
    }

    @Test
    @DisplayName("generateLicenseCapacityAlerts should create CRITICAL alert for >= 90% usage")
    void generateLicenseCapacityAlerts_CriticalUsage() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub
        mockAlertSave(); // **FIX 1**: Added save stub

        // mockLicense is 95/100 (95%)
        when(licenseRepository.findAll()).thenReturn(List.of(mockLicense));
        when(alertRepository.findByAlertTypeAndMessageContaining(any(), anyString())).thenReturn(Collections.emptyList());

        // Act
        alertService.generateLicenseCapacityAlerts();

        // Assert
        verify(alertRepository).save(alertCaptor.capture());
        Alert newAlert = alertCaptor.getValue();

        assertEquals(AlertType.LICENSE_CAPACITY_CRITICAL, newAlert.getAlertType());
        assertEquals(Severity.CRITICAL, newAlert.getSeverity());
        assertTrue(newAlert.getMessage().contains("capacity at 95% (95/100)"));
    }

    @Test
    @DisplayName("generateLicenseCapacityAlerts should create HIGH alert for 80-89% usage")
    void generateLicenseCapacityAlerts_WarningUsage() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub
        mockAlertSave(); // **FIX 1**: Added save stub

        mockLicense.setCurrentUsage(85); // 85% usage
        when(licenseRepository.findAll()).thenReturn(List.of(mockLicense));
        when(alertRepository.findByAlertTypeAndMessageContaining(any(), anyString())).thenReturn(Collections.emptyList());

        // Act
        alertService.generateLicenseCapacityAlerts();

        // Assert
        verify(alertRepository).save(alertCaptor.capture());
        Alert newAlert = alertCaptor.getValue();

        assertEquals(AlertType.LICENSE_CAPACITY_WARNING, newAlert.getAlertType());
        assertEquals(Severity.HIGH, newAlert.getSeverity());
        assertTrue(newAlert.getMessage().contains("capacity at 85% (85/100)"));
    }

    @Test
    @DisplayName("generateLicenseCapacityAlerts should create no alert for < 80% usage")
    void generateLicenseCapacityAlerts_SafeUsage() throws JsonProcessingException {
        // Arrange
        mockObjectMapper(); // **FIX 3**: Added ObjectMapper stub (for the audit log)

        mockLicense.setCurrentUsage(50); // 50% usage
        when(licenseRepository.findAll()).thenReturn(List.of(mockLicense));

        // Act
        alertService.generateLicenseCapacityAlerts();

        // Assert
        verify(alertRepository, never()).save(any());

        // Audit log should still be created
        verify(auditLogService).log(any(), any(), any(), any(), any(), anyString());
    }
}