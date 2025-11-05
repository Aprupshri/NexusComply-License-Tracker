package com.prodapt.license_tracker_backend.service;

import com.prodapt.license_tracker_backend.dto.*;
import com.prodapt.license_tracker_backend.entities.enums.Region;

import java.util.List;

public interface ReportService {
    List<LicenseReportResponse> getLicenseReport(String vendor, String software, String region, String status);
    List<DeviceReportResponse> getDeviceReport(String deviceType, String region, String lifecycle);
    List<AssignmentReportResponse> getAssignmentReport(String region, Boolean active);
    List<ComplianceReportResponse> getComplianceReport();
    ComplianceReportResponse getComplianceReportByRegion(Region region);
}
