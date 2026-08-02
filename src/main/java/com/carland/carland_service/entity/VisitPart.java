package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * tr: Bir servis ziyaretinde (Visit) kullanılan tek bir parçayı (visit_parts tablosu) temsil eden JPA entity'sidir;
 *     parça adı, miktarı ve birimini tutar. (Eski adı: ServiceHistoryPartV2)
 * en: JPA entity representing a single part used in a service visit (visit_parts table);
 *     holds part name, quantity and unit. (Former name: ServiceHistoryPartV2)
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "visit_parts")
@ToString(exclude = "visit")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VisitPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    private String name;

    private BigDecimal qty;

    private String unit;
}
