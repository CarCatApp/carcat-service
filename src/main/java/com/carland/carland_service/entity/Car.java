package com.carland.carland_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * tr: "cars" tablosunu modelleyen entity; müşteriye ait aracı (VIN, plaka, teknik özellikler, km) ve servis geçmişi ilişkilerini temsil eder.
 * en: Entity modeling the "cars" table; represents a customer's car (VIN, plate, technical specs, mileage) and its service history relations.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long carId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @ToString.Exclude
    Customer customer;

    @Column(nullable = false, unique = true)
    private String vin;
    private String plateNumber;
    private String brand;
    private String model;
    private Integer modelYear;
    private Long colorId;

    /**
     * tr: IDDA'nın Azerice renk metni. colorId doluysa boş kalır.
     *     Liste carlist:{owner}:* bu alanı taşır. Add ve edit commit sonrası o listeyi siler.
     * en: IDDA color text in Azerbaijani. Empty when colorId is set.
     *     carlist:{owner}:* carries this field. Add and edit delete that list after commit.
     */
    @Column(name = "idda_color", length = 80)
    private String iddaColor;
    private String engineType;
    private Long engineTypeId;
    private Integer engineVolume;
    private String transmissionType;
    private String bodyType;
    private Long mileage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal allTimeCost;
    /** Successful AI photo generates (informational). */
    Integer aiPhotoGenerateCount;
    /** Last time an AI generate job was started for this car. */
    LocalDateTime aiPhotoLastGenerateAt;
    /** OTP-style unlock instant. Generate and prompt-field edit are locked until this time. */
    LocalDateTime aiPhotoGenerateLockedUntil;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    @ToString.Exclude
    private MaintenanceTemplate maintenanceTemplate;

    @OneToMany(mappedBy = "car")
    @Builder.Default
    @JsonIgnore
    @ToString.Exclude
    List<ServiceHistory> serviceHistoryList = new ArrayList<>();

    @OneToMany(mappedBy = "car")
    @Builder.Default
    @JsonIgnore
    @ToString.Exclude
    List<CustomerServiceRecord> serviceRecordList = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vin_provided_fields")
    private List<String> vinProvidedFields;

    /** tr: Araç bir IDDA plaka kaydından yazıldı. / en: The car was written from an IDDA plate check. */
    Boolean fromIdda;

    /**
     * tr: Müşterinin değiştiremeyeceği IDDA alanları. Sonraki yazımı yalnız yeni bir IDDA add yapar.
     * en: IDDA fields the customer cannot change. Only a later IDDA add writes them again.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "idda_provided_fields")
    private List<String> iddaProvidedFields;

    /** Partner ids ({@code partners.id}) where this car received service. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "serviced_partner_ids")
    @Builder.Default
    private List<String> servicedPartnerIds = new ArrayList<>();

    /**
     * Branch ids for the car-list badges.
     * Mobile: {@code GET /api/v1/photo/for/branch/badge-logo/get/{branchId}}.
     * Null means not filled yet. Empty means filled and there is no branch badge.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "serviced_branch_ids")
    private List<String> servicedBranchIds;

    @PrePersist
    @PreUpdate
    void ensureVinProvidedFields() {
        if (vinProvidedFields == null) {
            vinProvidedFields = Collections.emptyList();
        }
        if (servicedPartnerIds == null) {
            servicedPartnerIds = new ArrayList<>();
        }
    }

}

