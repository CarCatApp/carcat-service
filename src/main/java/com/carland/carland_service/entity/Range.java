package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: "ranges" tablosunu modelleyen entity; takvim (Calendar) içindeki tek bir randevu zaman aralığını ve ona bağlı randevuları temsil eder.
 * en: Entity modeling the "ranges" table; represents a single appointment time slot within a Calendar and its linked appointments.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "ranges")
public class Range {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long rangeId;

    @Column(name = "start_time")
    OffsetDateTime start;

    @Column(name = "end_time")
    OffsetDateTime end;

    String status;
    Integer workerCount;

    /** Selected service's day was closed. The range row stays. */
    @Column(name = "day_hidden")
    Boolean dayHidden;

    /** instant | approval. Staff sets this when creating the day's ranges. */
    @Column(name = "booking_mode", length = 16)
    String bookingMode;

    /** * or one catalog serviceKey. Staff-generated rows leave this empty and use slotTarget. */
    @Column(name = "service_key", length = 64)
    String serviceKey;

    /** package | individual | repair_inspection. Empty on older rows. */
    @Column(name = "slot_target", length = 32)
    String slotTarget;

    /** Partner-panel package (branch_care_packages.id). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_package_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BranchCarePackage carePackage;

    /** Catalog individual service id. Not a service inside a package. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "individual_service_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    IndividualService individualService;


    @OneToMany
    @JoinColumn(name = "range_id")
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "calendar_id")
    Calendar calendar;
}

