package com.carland.carland_service.test_sima_idda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mobile → CarCat: Verify Foreign Citizen Identity (TRC / PRC / ERP).
 * Maps to SIMA POST /api/v1/kyc/identity/verify/foreign
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaForeignVerifyRequest {
    private String pin;
    private String livePhoto;
    /** Must be one of: TRC, PRC, ERP */
    private String documentType;
}
