package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * tr: Eyni barmaq izi ilə yığılmış xəta qrupu. Ham event 30 gündən sonra silinir, bu sətir qalır.
 * en: Error group keyed by fingerprint. Raw events expire after 30 days; this row stays.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_error_issues",
        uniqueConstraints = @UniqueConstraint(name = "uk_client_error_issue_fp",
                columnNames = {"fingerprint", "fingerprint_version"}),
        indexes = @Index(name = "idx_client_error_issue_last", columnList = "last_seen"))
public class ClientErrorIssue {

    public static final String STATUS_NEW = "new";
    public static final String STATUS_ONGOING = "ongoing";
    public static final String STATUS_RESOLVED = "resolved";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "fingerprint_version", nullable = false)
    private int fingerprintVersion;

    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "occurrence_count", nullable = false)
    private long occurrenceCount;

    @Column(name = "affected_user_count", nullable = false)
    private long affectedUserCount;

    @Column(name = "affected_install_count", nullable = false)
    private long affectedInstallCount;

    @Column(name = "high_frequency", nullable = false)
    private boolean highFrequency;

    @Column(name = "issue_status", nullable = false, length = 16)
    private String issueStatus;
}
