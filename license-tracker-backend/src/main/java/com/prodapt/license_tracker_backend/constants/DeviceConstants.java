package com.prodapt.license_tracker_backend.constants;

/**
 * Constants class for Device Service operations
 * Contains all magic strings, error messages, and configuration values
 */
public final class DeviceConstants {

    private DeviceConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ==================== CSV Related Constants ====================

    public static final class CsvHeaders {
        private CsvHeaders() {}

        public static final String DEVICE_ID = "deviceId";
        public static final String DEVICE_TYPE = "deviceType";
        public static final String MODEL = "model";
        public static final String IP_ADDRESS = "ipAddress";
        public static final String LOCATION = "location";
        public static final String REGION = "region";
        public static final String LIFECYCLE = "lifecycle";
        public static final String SOFTWARE_NAME = "softwareName";
        public static final String SOFTWARE_VERSION = "softwareVersion";
        public static final String PURCHASED_DATE = "purchasedDate";

        public static final String[] ALL_HEADERS = {
                DEVICE_ID, DEVICE_TYPE, MODEL, IP_ADDRESS, LOCATION,
                REGION, LIFECYCLE, SOFTWARE_NAME, SOFTWARE_VERSION, PURCHASED_DATE
        };
    }

    // ==================== Error Messages ====================

    public static final class ErrorMessages {
        private ErrorMessages() {}

        public static final String DEVICE_NOT_FOUND_ID = "Device not found with id: ";
        public static final String DEVICE_NOT_FOUND_DEVICE_ID = "Device not found with deviceId: ";
        public static final String DEVICE_ID_ALREADY_EXISTS = "Device ID already exists: ";
        public static final String FILE_EMPTY = "File is empty";
        public static final String ONLY_CSV_SUPPORTED = "Only CSV files are supported";
        public static final String FAILED_TO_PROCESS_FILE = "Failed to process file: ";
        public static final String INVALID_DATE_FORMAT = "Invalid date format for purchasedDate. Expected: yyyy-MM-dd";
        public static final String COLUMN_NOT_FOUND = " column not found in CSV";
        public static final String IS_REQUIRED = " is required";
        public static final String INVALID_VALUE_FORMAT = "Invalid %s value: %s. Valid values: %s";
    }

    // ==================== Log Messages ====================

    public static final class LogMessages {
        private LogMessages() {}

        // Info Messages
        public static final String CREATING_DEVICE = "Creating device: {}";
        public static final String DEVICE_CREATED = "Device created successfully: {}";
        public static final String UPDATING_DEVICE = "Updating device with ID: {}";
        public static final String DEVICE_UPDATED = "Device updated successfully: {}";
        public static final String DELETING_DEVICE = "Deleting device with ID: {}";
        public static final String DEVICE_DELETED = "Device deleted successfully: {}";
        public static final String STARTING_BULK_UPLOAD = "Starting bulk upload for file: {}";
        public static final String BULK_UPLOAD_COMPLETED = "Bulk upload completed. Success: {}, Failure: {}";
        public static final String GENERATING_TEMPLATE = "Generating device upload template";

        // Warn Messages
        public static final String NO_ACTIVE_ASSIGNMENTS = "No active license assignments found for device: {}";
        public static final String DEVICE_HAS_ACTIVE_ASSIGNMENTS = "Device has {} active license assignments. Auto-revoking before deletion.";
        public static final String AUTO_REVOKING_LICENSES = "Auto-revoking {} license assignment(s) for device: {} (Lifecycle: {})";
        public static final String LIFECYCLE_CHANGED = "Lifecycle changed from {} to {}. Auto-revoking licenses for device: {}";
        public static final String GET_USER_INFO_ERROR = "Error getting current user info";
        public static final String FETCH_USER_ID_ERROR = "Could not fetch user ID for username: {}";
        public static final String AUDIT_LOG_FAILED_DEVICE = "Failed to create audit log for device: {}";

        // Success Messages
        public static final String LICENSE_AUTO_REVOKED = "License {} auto-revoked from device {}";
        public static final String ALL_LICENSES_REVOKED = "Successfully auto-revoked all licenses for device: {}";

        // Error Messages
        public static final String ERROR_BULK_UPLOAD = "Error processing bulk upload";
        public static final String AUDIT_LOG_CREATE_FAILED = "Failed to create audit log for device creation";
        public static final String AUDIT_LOG_UPDATE_FAILED = "Failed to create audit log for device update";
        public static final String AUDIT_LOG_DELETE_FAILED = "Failed to create deletion audit log";
        public static final String AUDIT_LOG_BULK_UPLOAD_FAILED = "Failed to create bulk upload audit log";
        public static final String AUDIT_LOG_AUTO_REVOKE_FAILED = "Failed to create audit log for auto-revoked assignment";
    }

    // ==================== Audit Details Keys ====================

    public static final class AuditDetailsKeys {
        private AuditDetailsKeys() {}

        public static final String DEVICE_ID = "deviceId";
        public static final String DEVICE_TYPE = "deviceType";
        public static final String MODEL = "model";
        public static final String REGION = "region";
        public static final String CHANGES = "changes";
        public static final String OLD = "old";
        public static final String NEW = "new";
        public static final String HAD_ACTIVE_LICENSES = "hadActiveLicenses";
        public static final String REVOKED_LICENSES_COUNT = "revokedLicensesCount";
        public static final String LICENSE_KEY = "licenseKey";
        public static final String REASON = "reason";
        public static final String DEVICE_LIFECYCLE = "deviceLifecycle";
        public static final String TRIGGERED_BY = "triggeredBy";
        public static final String FILE_NAME = "fileName";
        public static final String TOTAL_RECORDS = "totalRecords";
        public static final String SUCCESS_COUNT = "successCount";
        public static final String FAILURE_COUNT = "failureCount";
        public static final String SOURCE = "source";
    }

    // ==================== Default Values ====================

    public static final class DefaultValues {
        private DefaultValues() {}

        public static final String SYSTEM_USERNAME = "SYSTEM";
        public static final String BULK_UPLOAD_ENTITY_ID = "BULK_UPLOAD";
        public static final String AUTO_REVOKE_REASON = "AUTO_REVOKE";
        public static final String BULK_UPLOAD_SOURCE = "BULK_UPLOAD";
    }

    // ==================== Format Constants ====================

    public static final class Formats {
        private Formats() {}

        public static final String DATE_FORMAT = "yyyy-MM-dd";
        public static final String AUTO_REVOKE_REASON_FORMAT = "Auto-revoked: Device lifecycle changed to %s";
        public static final String DEVICE_CREATED_SUCCESS_FORMAT = "Device %s created successfully";
        public static final String CSV_ROW_ERROR_FORMAT = "Row %d: %s";
        public static final String CSV_ROW_DEVICE_ERROR_FORMAT = "Row %d (Device: %s): %s";
    }

    // ==================== CSV Template Data ====================

    public static final class CsvTemplateData {
        private CsvTemplateData() {}

        // Sample Row 1
        public static final String SAMPLE_DEVICE_ID_1 = "RTR-BLR-101";
        public static final String SAMPLE_DEVICE_TYPE_1 = "ROUTER";
        public static final String SAMPLE_MODEL_1 = "Cisco ASR 9000";
        public static final String SAMPLE_IP_1 = "192.168.1.101";
        public static final String SAMPLE_LOCATION_1 = "Bangalore DC - Rack A5";
        public static final String SAMPLE_REGION_1 = "BANGALORE";
        public static final String SAMPLE_LIFECYCLE_1 = "ACTIVE";
        public static final String SAMPLE_SOFTWARE_NAME_1 = "IOS XR";
        public static final String SAMPLE_SOFTWARE_VERSION_1 = "7.5.2";
        public static final String SAMPLE_PURCHASED_DATE_1 = "2024-01-15";

        // Sample Row 2
        public static final String SAMPLE_DEVICE_ID_2 = "SW-CHN-201";
        public static final String SAMPLE_DEVICE_TYPE_2 = "SWITCH";
        public static final String SAMPLE_MODEL_2 = "Cisco Catalyst 9500";
        public static final String SAMPLE_IP_2 = "192.168.2.201";
        public static final String SAMPLE_LOCATION_2 = "Chennai DC - Rack B3";
        public static final String SAMPLE_REGION_2 = "CHENNAI";
        public static final String SAMPLE_LIFECYCLE_2 = "ACTIVE";
        public static final String SAMPLE_SOFTWARE_NAME_2 = "IOS-XE";
        public static final String SAMPLE_SOFTWARE_VERSION_2 = "17.6.3";
        public static final String SAMPLE_PURCHASED_DATE_2 = "2024-02-20";
    }

    // ==================== File Constants ====================

    public static final class FileConstants {
        private FileConstants() {}

        public static final String CSV_EXTENSION = ".csv";
    }

    // ==================== Map Keys ====================

    public static final class MapKeys {
        private MapKeys() {}

        public static final String USERNAME = "username";
        public static final String USER_ID = "userId";
        public static final String IP_ADDRESS = "ipAddress";
        public static final String LOCATION = "location";
        public static final String LIFECYCLE = "lifecycle";
        public static final String SOFTWARE_NAME = "softwareName";
        public static final String SOFTWARE_VERSION = "softwareVersion";
    }
}
