package com.carland.carland_service.test_sima_idda.dto.sima;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified SIMA KYC envelope: result + error + isSuccess.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaApiEnvelope {
    private SimaIdentityResult result;
    private SimaErrorBody error;
    private Boolean isSuccess;
}
