package com.carland.carland_service.test_sima_idda.controller;

import com.carland.carland_service.test_sima_idda.dto.request.SimaCitizenVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.request.SimaForeignVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.request.SimaPassportVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyResponse;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.service.SimaKycService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CarCat SIMA KYC demo APIs — HTML-aligned verify channels.
 * Flutter chooses template: citizen (vesika) / passport (local) / foreign (TRC|PRC|ERP).
 */
@RestController
@RequestMapping("/api/v1/sima")
@RequiredArgsConstructor
public class SimaController {

    private final SimaKycService simaKycService;

    /**
     * Curl/Postman parity test — HMAC + fresh UUID inside service, then POST SIMA staging
     * {@code https://pre-biosign-biometric-kyc.sima.az/api/v1/kyc/identity/verify}.
     * No X-User-Id / Customer required. Body: pin + (documentNumber XOR birthDate) + livePhoto.
     */
    @PostMapping("/test/identity/verify")
    public SimaApiEnvelope testIdentityVerify(@RequestBody SimaCitizenVerifyRequest request) {
        return simaKycService.testIdentityVerify(request);
    }

    /**
     * Verify Citizen Identity — AZ ID card.
     * SIMA: POST /api/v1/kyc/identity/verify
     */
    @PostMapping("/verify/citizen")
    public SimaVerifyResponse verifyCitizen(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody SimaCitizenVerifyRequest request
    ) {
        return simaKycService.verifyCitizen(userIdHeader, request);
    }

    /**
     * Verify Passport Identity — AZE passport.
     * SIMA: POST /api/v1/kyc/identity/verify/passport
     */
    @PostMapping("/verify/passport")
    public SimaVerifyResponse verifyPassport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody SimaPassportVerifyRequest request
    ) {
        return simaKycService.verifyPassport(userIdHeader, request);
    }

    /**
     * Verify Foreign Citizen Identity — migration docs TRC / PRC / ERP.
     * SIMA: POST /api/v1/kyc/identity/verify/foreign
     */
    @PostMapping("/verify/foreign")
    public SimaVerifyResponse verifyForeign(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody SimaForeignVerifyRequest request
    ) {
        return simaKycService.verifyForeign(userIdHeader, request);
    }
}
