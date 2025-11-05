package com.prodapt.license_tracker_backend.repository;

import com.prodapt.license_tracker_backend.entities.SoftwareVersion;
import com.prodapt.license_tracker_backend.entities.enums.SoftwareVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SoftwareVersionRepository extends JpaRepository<SoftwareVersion, Long> {

    List<SoftwareVersion> findByDeviceId(Long deviceId);

    List<SoftwareVersion> findByStatus(SoftwareVersionStatus status);

    List<SoftwareVersion> findByLastCheckedBefore(LocalDate date);

    Optional<SoftwareVersion> findByDeviceIdAndSoftwareName(Long deviceId, String softwareName);

    @Query("SELECT sv FROM SoftwareVersion sv WHERE sv.status = :status ORDER BY sv.lastChecked ASC")
    List<SoftwareVersion> findByStatusOrderByLastCheckedAsc(SoftwareVersionStatus status);

    @Query("SELECT COUNT(sv) FROM SoftwareVersion sv WHERE sv.status = 'CRITICAL'")
    long countCriticalVersions();

    @Query("SELECT COUNT(sv) FROM SoftwareVersion sv WHERE sv.status = 'OUTDATED'")
    long countOutdatedVersions();

    @Query("SELECT COUNT(sv) FROM SoftwareVersion sv WHERE sv.status = 'UP_TO_DATE'")
    long countUpToDateVersions();
}

