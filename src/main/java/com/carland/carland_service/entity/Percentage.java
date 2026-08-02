package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * tr: "percentages" tablosunu modelleyen entity; bir aracın servis kalemi bazında bakım yüzdesi durumunu (son/sonraki servis, kalan km, yüzdeler) saklar.
 * en: Entity modeling the "percentages" table; stores a car's per-service maintenance percentage state (last/next service, remaining km, percentages).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "percentages")
public class Percentage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String serviceName;
    String actionType;

    String serviceNameRu;
    String serviceNameAz;
    String serviceNameEn;

    Long intervalKm;
    Integer intervalMonth;

    LocalDate lastServiceDate;
    Integer lastServiceKm;

    LocalDate nextServiceDate;
    Integer nextServiceKm;

    Integer remainingKm;
    LocalDate remainingMonths;

    Integer kmPercentage;
    Integer monthPercentage;

    Long serviceId;

    @Column(nullable = false)
    @ColumnDefault("false")
    boolean important;

    /**
     * Added status field to control percentage handling.
     * If status is "edited", percentage values are returned directly in the response.
     * If status is "created", percentage values are recalculated and then added to the response.
     */
    String status;

    Long carId;

    LocalDateTime lastNotificationSentAt;

    /** Last time partner (Hyper) data was applied to this percentage. */
    LocalDateTime lastPartnerSyncAt;

    /** Hyper visit recordId that was last applied; used for idempotent partner sync. */
    String partnerRecordId;


}
