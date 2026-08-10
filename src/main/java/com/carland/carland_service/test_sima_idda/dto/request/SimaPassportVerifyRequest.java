package com.carland.carland_service.test_sima_idda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mobile → CarCat: Verify Passport Identity (local AZE passport path).
 * Maps to SIMA POST /api/v1/kyc/identity/verify/passport
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaPassportVerifyRequest {
    private String pin;
    private String documentNumber;
    private String livePhoto;
}
