package com.prodapt.license_tracker_backend.entities;

import com.prodapt.license_tracker_backend.entities.enums.AlertType;
import com.prodapt.license_tracker_backend.entities.enums.Region;
import com.prodapt.license_tracker_backend.entities.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private Region region;

    private LocalDateTime generatedAt;

    private Boolean acknowledged = false;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
