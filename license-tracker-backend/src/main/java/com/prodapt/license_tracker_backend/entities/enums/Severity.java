package com.prodapt.license_tracker_backend.entities.enums;


public enum Severity {
    CRITICAL, //less than 15 days
    WARNING,
    INFO,
    LOW,      // Informational
    MEDIUM,   // Warning (30+ days)
    HIGH,     // Urgent (15-30 days)
}
