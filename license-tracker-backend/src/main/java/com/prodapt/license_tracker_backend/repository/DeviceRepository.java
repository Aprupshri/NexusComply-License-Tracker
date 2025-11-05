package com.prodapt.license_tracker_backend.repository;


import com.prodapt.license_tracker_backend.entities.Device;
import com.prodapt.license_tracker_backend.entities.enums.DeviceLifecycle;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceId(String deviceId);
    List<Device> findByRegion(Region region);
    Page<Device> findByLifecycle(DeviceLifecycle lifecycle, Pageable pageable);
    List<Device> findByRegionAndLifecycle(Region region, DeviceLifecycle lifecycle);
    boolean existsByDeviceId(String deviceId);
    long countByLifecycle(DeviceLifecycle lifecycle);
    long countByRegion(Region region);
    long countByRegionAndLifecycle(Region region, DeviceLifecycle lifecycle);

    @Query("SELECT COUNT(DISTINCT d) FROM Device d JOIN LicenseAssignment la ON d.id = la.device.id WHERE la.active = true")
    long countDevicesWithActiveLicenses();

}
