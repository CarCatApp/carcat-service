package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Bir aracın partner serviste geçirdiği tek bir servis ziyaretini (visits tablosu) temsil eden JPA entity'sidir;
 *     ziyaretin tarihi, kilometresi, maliyeti, servis merkezi ve satır/parça detaylarını tutar.
 * en: JPA entity representing a single service visit of a car at a partner service center (visits table);
 *     holds visit date, mileage, costs, service center info and line/part details.
 */
@Entity
@Table(name = "visits", uniqueConstraints = {
        @UniqueConstraint(name = "uk_visits_car_hyper_record", columnNames = {"car_id", "hyper_record_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"car", "services", "parts"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "hyper_record_id", nullable = false)
    private Long hyperRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    private String serviceType;

    private LocalDate lastServiceDate;

    private Integer lastServiceMileage;

    private String invoiceNumber;

    private String dealer;

    /** {@link com.carland.carland_service.enums.PartnerId} — references {@code partners.id}. */
    private Long serviceCenterId;

    /** Denormalized partner display name at write time. */
    private String serviceCenterName;

    @Column(precision = 12, scale = 2)
    private BigDecimal costAmount;

    private String costCurrency;

    @Column(precision = 12, scale = 2)
    private BigDecimal finalCostAmount;

    private String finalCostCurrency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_groups", columnDefinition = "jsonb")
    private List<String> serviceGroups;

    @Builder.Default
    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VisitServiceLine> services = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 32)
    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VisitPart> parts = new ArrayList<>();

    /**
     * tr: Ziyarete bir servis satırı ekler ve çift yönlü ilişkiyi (line → visit) kurar.
     * en: Adds a service line to this visit and wires the bidirectional relation (line → visit).
     */
    public void addService(VisitServiceLine service) {
        services.add(service);
        service.setVisit(this);
    }

    /**
     * tr: Ziyarete bir parça kaydı ekler ve çift yönlü ilişkiyi (part → visit) kurar.
     * en: Adds a part record to this visit and wires the bidirectional relation (part → visit).
     */
    public void addPart(VisitPart part) {
        parts.add(part);
        part.setVisit(this);
    }
}
