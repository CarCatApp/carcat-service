package com.carland.carland_service.test_sima_idda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mobile → CarCat: Verify Citizen Identity (AZ ID card).
 * Maps to SIMA POST /api/v1/kyc/identity/verify
 * Exactly one of documentNumber | birthDate must be filled (other null).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaCitizenVerifyRequest {
    /** FIN — SIMA field name is pin */
    private String pin;
    private String documentNumber;
    /** yyyy-MM-dd */
    private String birthDate;
    private String livePhoto;
}
