package com.carland.carland_service.digitalhistory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * tr: Bir buraxılmış hesabat. Mətn jsonb-də donur. Keş yoxdur.
 * en: One issued report. The text is frozen in jsonb. There is no cache.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "digital_history_reports")
public class DigitalHistoryIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 24)
    private String code;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    /** tr: Bakı divar saatı. / en: Baku wall time. */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false, length = 8)
    private String lang;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> body;
}
