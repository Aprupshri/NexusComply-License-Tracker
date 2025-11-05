package com.prodapt.license_tracker_backend.entities;


import com.prodapt.license_tracker_backend.entities.enums.DeviceLifecycle;
import com.prodapt.license_tracker_backend.entities.enums.DeviceType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    private String model;
    private String ipAddress;
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    private DeviceLifecycle lifecycle;

    private String softwareName;
    private String softwareVersion;

    private LocalDate purchasedDate;
}
