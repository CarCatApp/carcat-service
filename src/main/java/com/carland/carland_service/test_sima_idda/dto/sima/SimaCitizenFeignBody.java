package com.carland.carland_service.test_sima_idda.dto.sima;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body sent to SIMA Verify Citizen (minified + HMAC-signed).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Omit null XOR field (documentNumber | birthDate) so body matches curl minify. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimaCitizenFeignBody {
    private String pin;
    private String documentNumber;
    private String birthDate;
    private String livePhoto;
    private String idempotencyKey;
}
