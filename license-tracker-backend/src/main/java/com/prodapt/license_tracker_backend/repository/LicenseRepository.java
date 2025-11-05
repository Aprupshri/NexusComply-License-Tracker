package com.prodapt.license_tracker_backend.repository;


import com.prodapt.license_tracker_backend.entities.License;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;



@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByLicenseKey(String licenseKey);

    boolean existsByLicenseKey(String licenseKey);

    List<License> findByActiveTrue();

    List<License> findByActiveFalse();

    List<License> findByActiveTrueAndValidToBefore(LocalDate date);


    List<License> findByActiveTrueAndValidToBetween(LocalDate startDate, LocalDate endDate);


    List<License> findByRegion(Region region);

    long countByActiveTrue();
    long countByRegion(Region region);
    long countByRegionAndActiveTrue(Region region);

    List<License> findByRegionAndActiveTrueAndValidToBetween(Region region, LocalDate start, LocalDate end);

    @Query("SELECT COUNT(l) FROM License l WHERE l.vendor.id = :vendorId")
    long countByVendorId(@Param("vendorId") Long vendorId);
}

