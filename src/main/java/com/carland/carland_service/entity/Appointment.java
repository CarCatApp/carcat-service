package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

/**
 * tr: Müşterinin şubeye aldığı randevu.
 * en: Customer appointment at a branch.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "appointment_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime appointmentDate;
    OffsetDateTime appointmentStart;
    OffsetDateTime appointmentEnd;

    String status;
    String serviceName;
    String actionType;
    String serviceCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Branch branch;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "range_id")
    private Range range;
}
