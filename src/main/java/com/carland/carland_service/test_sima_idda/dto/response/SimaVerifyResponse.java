package com.carland.carland_service.test_sima_idda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CarCat API response for SIMA verify flows.
 * Only fields that are safe for demo response — no gender / address dump.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaVerifyResponse {
    private boolean verified;
    private String pin;
    private String name;
    private String surname;
    private Double livenessScore;
    private Double similarityScore;
    private String transactionId;
    private String code;
    private String message;
    private Integer simaErrorCode;
}
