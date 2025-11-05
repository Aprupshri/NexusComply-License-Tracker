package com.prodapt.license_tracker_backend.entities;


import com.prodapt.license_tracker_backend.entities.enums.SoftwareVersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "software_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftwareVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "software_name", nullable = false, length = 100)
    private String softwareName;

    @Column(name = "current_version", nullable = false, length = 20)
    private String currentVersion;

    @Column(name = "latest_version", length = 20)
    private String latestVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SoftwareVersionStatus status = SoftwareVersionStatus.UNKNOWN;

    @Column(name = "last_checked")
    private LocalDate lastChecked;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "update_url")
    private String updateUrl;

    @Column(name = "release_date")
    private LocalDate releaseDate;
}

