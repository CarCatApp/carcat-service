package com.carland.carland_service.test_sima_idda.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mobile → CarCat: Verify Citizen Identity (AZ ID card).
 * Maps to SIMA POST /api/v1/kyc/identity/verify
 * Exactly one of documentNumber | birthDate must be filled (other null).
 * JSON channels use livePhoto as Base64. Test endpoint accepts multipart {@code photo} and converts in service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaCitizenVerifyRequest {
    /** FIN — SIMA field name is pin */
    @Schema(example = "62HJ5KQ")
    private String pin;

    @Schema(example = "AB0668397")
    private String documentNumber;

    /** yyyy-MM-dd — XOR with documentNumber (omit / null when documentNumber set) */
    @Schema(nullable = true)
    private String birthDate;

    /** JPEG Base64 for SIMA (filled by service when multipart photo is used). */
    @Schema(hidden = true)
    private String livePhoto;
}
