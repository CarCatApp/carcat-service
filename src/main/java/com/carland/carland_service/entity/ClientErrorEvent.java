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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * tr: Mobildən gələn tək hadisə. Vaxtlar UTC-dir (admin Bakı üçün +4 saat göstərir).
 *     userId boş ola bilər (qonaq) və hesab silinəndə boşaldılır.
 * en: One event from the mobile client. Timestamps are UTC (admin shows Baku as +4 hours).
 *     userId is empty for a guest and is cleared when the account is deleted.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_error_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_client_error_event_id", columnNames = "event_id"),
        indexes = {
                @Index(name = "idx_client_error_event_user_time", columnList = "user_id, occurred_at"),
                @Index(name = "idx_client_error_event_issue_time", columnList = "issue_id, occurred_at"),
                @Index(name = "idx_client_error_event_received", columnList = "received_at")
        })
public class ClientErrorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "stack_trace", length = 8192)
    private String stackTrace;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "install_id", nullable = false, length = 64)
    private String installId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "app_version", nullable = false, length = 40)
    private String appVersion;

    @Column(name = "build_number", nullable = false, length = 40)
    private String buildNumber;

    @Column(name = "package_name", nullable = false, length = 200)
    private String packageName;

    @Column(name = "os_name", nullable = false, length = 32)
    private String osName;

    @Column(name = "os_version", nullable = false, length = 80)
    private String osVersion;

    @Column(name = "memory_rss_bytes")
    private Long memoryRssBytes;

    @Column(length = 16)
    private String network;

    @Column(nullable = false, length = 16)
    private String environment;

    @Column(name = "context_library", length = 64)
    private String contextLibrary;

    @Column(name = "context_route", length = 160)
    private String contextRoute;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "is_expected", nullable = false)
    private boolean expected;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "fingerprint_version", nullable = false)
    private int fingerprintVersion;

    @Column(name = "issue_id", nullable = false)
    private long issueId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> breadcrumbs = new ArrayList<>();
}
