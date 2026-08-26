package com.carland.carland_service.test_sima_idda.controller;

import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyOutcome;
import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyResponse;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.service.SimaKycService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * CarCat SIMA KYC APIs. Flutter: citizen (vesika) / foreign (TRC|PRC|ERP).
 * Passport channel is paused (PO).
 */
@RestController
@RequestMapping("/api/v1/sima")
@RequiredArgsConstructor
public class SimaController {

    private final SimaKycService simaKycService;

    @PostMapping(value = "/test/identity/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SimaApiEnvelope testIdentityVerify(
            @RequestParam("pin") String pin,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "birthDate", required = false) String birthDate,
            @RequestParam("photo") MultipartFile photo
    ) {
        return simaKycService.testIdentityVerify(pin, documentNumber, birthDate, photo);
    }

    @Operation(summary = "SIMA verify citizen (vesika)",
            description = "Form: pin, documentNumber XOR birthDate, photo (JPEG File). JWT + X-User-Id.")
    @PostMapping(value = "/verify/citizen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SimaVerifyResponse> verifyCitizen(
            @RequestParam("pin") String pin,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "birthDate", required = false) String birthDate,
            @RequestParam("photo") MultipartFile photo,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        SimaVerifyOutcome outcome = simaKycService.verifyCitizen(
                userIdHeader, pin, documentNumber, birthDate, photo, acceptLanguage);
        return ResponseEntity.status(outcome.getHttpStatus()).body(outcome.getBody());
    }

    @Operation(summary = "SIMA verify foreign (TRC|PRC|ERP)",
            description = "Form: pin, documentType (TRC|PRC|ERP), photo. JWT + X-User-Id.")
    @PostMapping(value = "/verify/foreign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SimaVerifyResponse> verifyForeign(
            @RequestParam("pin") String pin,
            @RequestParam("documentType") String documentType,
            @RequestParam("photo") MultipartFile photo,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        SimaVerifyOutcome outcome = simaKycService.verifyForeign(
                userIdHeader, pin, documentType, photo, acceptLanguage);
        return ResponseEntity.status(outcome.getHttpStatus()).body(outcome.getBody());
    }
}
