package com.carland.carland_service.test_sima_idda.dto.sima;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified SIMA result object fields used across citizen / passport / foreign responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaIdentityResult {
    private String pin;
    private String documentNumber;
    private String name;
    private String surname;
    private String patronymic;
    private String birthDate;
    private String birthAddress;
    private String address;
    private String nationality;
    private String gender;
    private String expDate;
    private String documentType;
    private String issuingCountry;
    private Double livenessScore;
    private Boolean livenessStatus;
    private String livenessFailureReason;
    private Double similarityScore;
    private Boolean similarityStatus;
    private String transactionId;
    private String idempotencyKey;
    private String processTime;
}
