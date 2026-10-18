package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * tr: Bir servis ziyareti (Visit) içindeki tek bir servis satırını (visit_service_lines tablosu) temsil eden
 *     JPA entity'sidir; servis kodu/adı, maliyeti ve bir sonraki servis bilgilerini tutar.
 *     (Eski adı: ServiceHistoryV2)
 * en: JPA entity representing a single service line inside a service visit (visit_service_lines table);
 *     holds service code/name, cost and next-service info.
 *     (Former name: ServiceHistoryV2)
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "visit_service_lines")
@ToString(exclude = "visit")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VisitServiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    private Integer serviceCode;

    private String serviceName;

    /** Hyper universalServiceId, stored raw exactly as Hyper sends it. */
    private String universalServiceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_groups", columnDefinition = "jsonb")
    private List<String> serviceGroups;

    @Column(precision = 12, scale = 2)
    private BigDecimal costAmount;

    private String costCurrency;

    private LocalDate nextServiceDate;

    private Integer nextServiceMileage;

    /*
     * tr: Aşağıdakiler yalnız panel ("Xidməti tamamla") satırlarında dolar. costAmount = xidmət + məhsul (endirimsiz).
     * en: Fields below are filled only for panel ("Complete service") lines. costAmount = service + product (pre-discount).
     */

    /** package | individual | one_time | package_info — see {@code VisitLineKind}. */
    @Column(name = "kind", length = 16)
    private String kind;

    @Column(name = "offered_service_id")
    private Long offeredServiceId;

    @Column(name = "individual_service_id")
    private Long individualServiceId;

    /** Date this line was done. Falls back to the visit date when null. */
    @Column(name = "done_date")
    private LocalDate doneDate;

    /** Mileage when this line was done. Falls back to the visit mileage when null. */
    @Column(name = "done_km")
    private Integer doneKm;

    /** Service (labour) price, AZN, pre-discount. */
    @Column(name = "service_amount", precision = 12, scale = 2)
    private BigDecimal serviceAmount;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    /** After the line discount, AZN. */
    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "product_good_id")
    private Long productGoodId;

    /** "Heading · Brand", e.g. "Mühərrik yağı · Castrol EDGE". */
    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_qty", precision = 10, scale = 2)
    private BigDecimal productQty;

    /** Product total (unit price × qty), AZN. Null when included in the package. */
    @Column(name = "product_amount", precision = 12, scale = 2)
    private BigDecimal productAmount;

    @Column(name = "notes", length = 1000)
    private String notes;
}
