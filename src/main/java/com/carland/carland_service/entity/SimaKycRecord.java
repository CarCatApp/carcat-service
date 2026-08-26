package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * One SIMA KYC attempt (success or fail). Identity fields live here and are copied
 * onto {@link Customer} only when the attempt is verified.
 */
@Entity
@Table(name = "sima_kyc_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sima_kyc_idempotency", columnNames = "idempotency_key")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimaKycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    Customer customer;

    @Column(nullable = false, length = 16)
    String channel;

    @Column(nullable = false)
    boolean verified;

    @Column(name = "applied_to_profile", nullable = false)
    boolean appliedToProfile;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    String idempotencyKey;

    @Column(name = "transaction_id")
    String transactionId;

    @Column(name = "process_time")
    String processTime;

    String pin;
    @Column(name = "document_number")
    String documentNumber;
    String name;
    String surname;
    String patronymic;
    @Column(name = "birth_date")
    String birthDate;
    @Column(name = "birth_address")
    String birthAddress;
    String address;
    String nationality;
    String gender;
    @Column(name = "exp_date")
    String expDate;
    @Column(name = "document_type")
    String documentType;
    @Column(name = "issuing_country")
    String issuingCountry;

    @Column(name = "liveness_score")
    Double livenessScore;
    @Column(name = "liveness_status")
    Boolean livenessStatus;
    @Column(name = "liveness_failure_reason")
    String livenessFailureReason;
    @Column(name = "similarity_score")
    Double similarityScore;
    @Column(name = "similarity_status")
    Boolean similarityStatus;

    @Column(name = "sima_http_status")
    Integer simaHttpStatus;
    @Column(name = "sima_response_code")
    Integer simaResponseCode;
    @Column(name = "sima_message", length = 1000)
    String simaMessage;

    @Column(nullable = false, length = 64)
    String outcome;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
