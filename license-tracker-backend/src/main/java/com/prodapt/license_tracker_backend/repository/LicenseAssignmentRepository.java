package com.prodapt.license_tracker_backend.repository;

import com.prodapt.license_tracker_backend.entities.Device;
import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.LicenseAssignment;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseAssignmentRepository extends JpaRepository<LicenseAssignment, Long> {

    // Find active assignments by device
    List<LicenseAssignment> findByDeviceIdAndActiveTrue(Long deviceId);

    // Find active assignments by license
    List<LicenseAssignment> findByLicenseIdAndActiveTrue(Long licenseId);

    // Find all active assignments
    List<LicenseAssignment> findByActiveTrue();



    // Find specific active assignment
    Optional<LicenseAssignment> findByDeviceIdAndLicenseIdAndActiveTrue(Long deviceId, Long licenseId);

    // Check if assignment exists
    boolean existsByDeviceIdAndLicenseIdAndActiveTrue(Long deviceId, Long licenseId);

    // Count active assignments for a license (for usage tracking)
    @Query("SELECT COUNT(la) FROM LicenseAssignment la WHERE la.license.id = :licenseId AND la.active = true")
    long countActiveAssignmentsByLicenseId(@Param("licenseId") Long licenseId);


    @Query("SELECT COUNT(la) FROM LicenseAssignment la WHERE la.device.id = :deviceId AND la.active = true")
    long countActiveAssignmentsByDeviceId(@Param("deviceId") Long deviceId);

    @Query("SELECT COUNT(a) FROM LicenseAssignment a WHERE a.device = :device AND a.active = true")
    long countByDeviceAndActiveTrue(@Param("device") Device device);

    // Count active assignments for a license
    @Query("SELECT COUNT(a) FROM LicenseAssignment a WHERE a.license = :license AND a.active = true")
    long countByLicenseAndActiveTrue(@Param("license") License license);

    // Check if device has active assignments
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM LicenseAssignment a WHERE a.device = :device AND a.active = true")
    boolean existsByDeviceAndActiveTrue(@Param("device") Device device);

    // Find active assignments by device ID
    @Query("SELECT a FROM LicenseAssignment a WHERE a.device.id = :deviceId AND a.active = true")
    List<LicenseAssignment> findActiveAssignmentsByDeviceId(@Param("deviceId") Long deviceId);

    // Find active assignments by license ID
    @Query("SELECT a FROM LicenseAssignment a WHERE a.license.id = :licenseId AND a.active = true")
    List<LicenseAssignment> findActiveAssignmentsByLicenseId(@Param("licenseId") Long licenseId);

    // Find all assignments by device ID
    @Query("SELECT a FROM LicenseAssignment a WHERE a.device.id = :deviceId")
    List<LicenseAssignment> findByDeviceId(@Param("deviceId") Long deviceId);

    // Find all assignments by license ID
    @Query("SELECT a FROM LicenseAssignment a WHERE a.license.id = :licenseId")
    List<LicenseAssignment> findByLicenseId(@Param("licenseId") Long licenseId);

    long countByActiveTrue();

    @Query("SELECT COUNT(la) FROM LicenseAssignment la WHERE la.device.region = :region AND la.active = true")
    long countActiveAssignmentsByRegion(@Param("region") Region region);
    
    List<LicenseAssignment> findByDeviceAndActiveTrue(Device device);
}
