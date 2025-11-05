package com.prodapt.license_tracker_backend.repository;


import com.prodapt.license_tracker_backend.entities.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;


import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByVendorName(String vendorName);
    boolean existsByVendorName(String vendorName);

}
