package com.vlz.laborexchange_applicationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "vacancy_id", nullable = false)
    private Long vacancyId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private ApplicationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void setStatusFromType(ApplicationStatusType type) {
        ApplicationStatus statusProxy = new ApplicationStatus();
        statusProxy.setId(type.getId());
        this.status = statusProxy;
    }
}