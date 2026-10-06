package com.prince.codereview.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_results")
@Getter
@Setter
public class ReviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_job_id", nullable = false, unique = true)
    private ReviewJob reviewJob;

    @Column(name = "overall_score")
    private int overallScore;

    @Column(name = "quality_score")
    private int qualityScore;

    @Column(name = "security_score")
    private int securityScore;

    @Column(name = "maintainability_score")
    private int maintainabilityScore;

    @Column(name = "summary_text", length = 4000)
    private String summaryText;

    @Column(name = "risks_text", length = 4000)
    private String risksText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}