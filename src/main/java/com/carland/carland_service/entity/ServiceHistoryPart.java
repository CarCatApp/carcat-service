package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * tr: "service_history_parts" tablosunu modelleyen entity; bir servis geçmişi kaydında kullanılan parçayı (ad, miktar, maliyet, indirim) temsil eder.
 * en: Entity modeling the "service_history_parts" table; represents a part used in a service history record (name, quantity, cost, discount).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "service_history_parts")
public class ServiceHistoryPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_history_id", nullable = false)
    @ToString.Exclude
    ServiceHistory serviceHistory;

    String name;
    BigDecimal qty;
    String unit;
    BigDecimal cost;
    BigDecimal finalCost;
    BigDecimal discount;
}
