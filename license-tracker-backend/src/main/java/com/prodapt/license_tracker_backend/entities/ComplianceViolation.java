package com.prodapt.license_tracker_backend.entities;

import com.prodapt.license_tracker_backend.entities.enums.Severity;
import com.prodapt.license_tracker_backend.entities.enums.ViolationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    @Enumerated(EnumType.STRING)
    private ViolationType violationType;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private LocalDateTime detectedAt;

    private Boolean resolved = false;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
