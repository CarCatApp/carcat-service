package com.carland.carland_service.test_sima_idda.controller;

import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyResponse;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.service.SimaKycService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * CarCat SIMA KYC demo APIs — HTML-aligned verify channels.
 * Flutter chooses template: citizen (vesika) / foreign (TRC|PRC|ERP).
 * Passport channel is paused (PO).
 */
@RestController
@RequestMapping("/api/v1/sima")
@RequiredArgsConstructor
public class SimaController {

    private final SimaKycService simaKycService;

    /**
     * Postman/Flutter test — multipart photo; service converts JPEG → Base64, HMAC + fresh UUID,
     * then POST SIMA staging {@code /api/v1/kyc/identity/verify}.
     * Form: pin, documentNumber XOR birthDate, photo (file).
     * Staging defaults: pin=62HJ5KQ, documentNumber=AB0668397.
     */
    @PostMapping(value = "/test/identity/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SimaApiEnvelope testIdentityVerify(
            @RequestParam("pin") String pin,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "birthDate", required = false) String birthDate,
            @RequestParam("photo") MultipartFile photo
    ) {
        return simaKycService.testIdentityVerify(pin, documentNumber, birthDate, photo);
    }

    /**
     * Verify Citizen Identity — AZ şəxsiyyət vəsiqəsi.
     * Same photo pipeline as {@code /test/identity/verify}; Feign → SIMA {@code /api/v1/kyc/identity/verify}.
     * Form: pin, documentNumber XOR birthDate, photo (JPEG file). JWT + X-User-Id required.
     */
    @Operation(summary = "SIMA verify citizen (vesika)",
            description = "Same form-data as /test/identity/verify: pin, documentNumber XOR birthDate, photo (JPEG File). Extra: JWT + X-User-Id.")
    @PostMapping(value = "/verify/citizen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SimaVerifyResponse verifyCitizen(
            @RequestParam("pin") String pin,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "birthDate", required = false) String birthDate,
            @RequestParam("photo") MultipartFile photo,
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        return simaKycService.verifyCitizen(userIdHeader, pin, documentNumber, birthDate, photo);
    }

    /**
     * Verify Passport Identity — AZE passport.
     * SIMA: POST /api/v1/kyc/identity/verify/passport
     * PO: not needed for now — mapping kept commented.
     */
//    @PostMapping("/verify/passport")
//    public SimaVerifyResponse verifyPassport(
//            @RequestHeader("X-User-Id") String userIdHeader,
//            @RequestBody SimaPassportVerifyRequest request
//    ) {
//        return simaKycService.verifyPassport(userIdHeader, request);
//    }

    /**
     * Verify Foreign Citizen Identity — migration docs TRC / PRC / ERP.
     * Feign → SIMA {@code /api/v1/kyc/identity/verify/foreign}.
     * Form: pin, documentType, photo (JPEG file). JWT + X-User-Id required.
     */
    @Operation(summary = "SIMA verify foreign (TRC|PRC|ERP)",
            description = "Same photo File as /test/identity/verify. Form: pin, documentType (TRC|PRC|ERP), photo. Extra: JWT + X-User-Id.")
    @PostMapping(value = "/verify/foreign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SimaVerifyResponse verifyForeign(
            @RequestParam("pin") String pin,
            @RequestParam("documentType") String documentType,
            @RequestParam("photo") MultipartFile photo,
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        return simaKycService.verifyForeign(userIdHeader, pin, documentType, photo);
    }
}
