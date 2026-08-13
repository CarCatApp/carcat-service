package com.carland.carland_service.test_sima_idda.controller;

import com.carland.carland_service.test_sima_idda.dto.request.SimaCitizenVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.request.SimaForeignVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.request.SimaPassportVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyResponse;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.service.SimaKycService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestPart("photo") MultipartFile photo
    ) {
        return simaKycService.testIdentityVerify(pin, documentNumber, birthDate, photo);
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
