package com.prodapt.license_tracker_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "license_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "license_id", nullable = false)
    private License license;

    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    private LocalDateTime assignedOn;
    private String assignedBy;

    private LocalDateTime revokedOn;
    private String revokedBy;
    private String revocationReason;

    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        assignedOn = LocalDateTime.now();
    }
}
