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
    private String engineType;
    private Long engineTypeId;
    private Integer engineVolume;
    private String transmissionType;
    private String bodyType;
    private Long mileage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal allTimeCost;
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

    /** Partner ids ({@code partners.id}) where this car received service — used for badge logos on car list. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "serviced_partner_ids")
    @Builder.Default
    private List<String> servicedPartnerIds = new ArrayList<>();

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

