package com.prodapt.license_tracker_backend.service;




import com.prodapt.license_tracker_backend.dto.AssignmentRequest;
import com.prodapt.license_tracker_backend.dto.AssignmentResponse;
import com.prodapt.license_tracker_backend.dto.RevokeAssignmentRequest;

import java.util.List;

public interface LicenseAssignmentService {
    AssignmentResponse assignLicenseToDevice(AssignmentRequest request);
    AssignmentResponse revokeAssignment(Long assignmentId, RevokeAssignmentRequest request);
    List<AssignmentResponse> getActiveAssignmentsByDevice(Long deviceId);
    List<AssignmentResponse> getActiveAssignmentsByLicense(Long licenseId);
    List<AssignmentResponse> getAllActiveAssignments();
    List<AssignmentResponse> getAllAssignmentsByDevice(Long deviceId);
    List<AssignmentResponse> getAllAssignmentsByLicense(Long licenseId);
    AssignmentResponse getAssignmentById(Long id);
}
