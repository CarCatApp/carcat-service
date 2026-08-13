package com.carland.carland_service.test_sima_idda.service;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.test_sima_idda.config.SimaIddaConstants;
import com.carland.carland_service.test_sima_idda.dto.request.SimaCitizenVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.request.SimaForeignVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.request.SimaPassportVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyResponse;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaCitizenFeignBody;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaForeignFeignBody;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaIdentityResult;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaPassportFeignBody;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaErrorBody;
import com.carland.carland_service.test_sima_idda.feign.SimaFeign;
import com.carland.carland_service.test_sima_idda.hmac.SimaHmacSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * SIMA KYC orchestration (demo). Applies verify result onto Customer via setters
 * that may not exist yet on the entity — intentional for patron walkthrough.
 * Does NOT call customerRepository.save() in this test package stage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimaKycService {

    private final SimaFeign simaFeign;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    /**
     * Raw SIMA proxy for Postman / curl parity — no Customer / X-User-Id.
     * Posts to pre-biosign {@code /api/v1/kyc/identity/verify} with fresh UUID + HMAC.
     */
    public SimaApiEnvelope testIdentityVerify(SimaCitizenVerifyRequest request) {
        validateCitizenXor(request);
        String idempotencyKey = UUID.randomUUID().toString();
        SimaCitizenFeignBody body = SimaCitizenFeignBody.builder()
                .pin(request.getPin())
                .documentNumber(blankToNull(request.getDocumentNumber()))
                .birthDate(blankToNull(request.getBirthDate()))
                .livePhoto(request.getLivePhoto())
                .idempotencyKey(idempotencyKey)
                .build();

        String minified = SimaHmacSigner.minify(body);
        String signature = SimaHmacSigner.signBase64(minified);
        log.info("SIMA test/identity/verify idempotencyKey={} bodyBytes={} signature={}",
                idempotencyKey, minified.length(), signature);

        try {
            return simaFeign.verifyCitizen(
                    SimaIddaConstants.EXAMPLE_SIMA_IDENTIFIER,
                    SimaIddaConstants.EXAMPLE_SIMA_AUTH_SCHEME,
                    signature,
                    SimaIddaConstants.EXAMPLE_SIMA_DEVICE_INFO,
                    minified
            );
        } catch (FeignException e) {
            return parseFeignErrorEnvelope(e, idempotencyKey);
        }
    }

    public SimaVerifyResponse verifyCitizen(String userIdHeader, SimaCitizenVerifyRequest request) {
        Customer customer = requireCustomer(userIdHeader);
        validateCitizenXor(request);

        String idempotencyKey = UUID.randomUUID().toString();
        SimaCitizenFeignBody body = SimaCitizenFeignBody.builder()
                .pin(request.getPin())
                .documentNumber(blankToNull(request.getDocumentNumber()))
                .birthDate(blankToNull(request.getBirthDate()))
                .livePhoto(request.getLivePhoto())
                .idempotencyKey(idempotencyKey)
                .build();

        String minified = SimaHmacSigner.minify(body);
        String signature = SimaHmacSigner.signBase64(minified);

        SimaApiEnvelope envelope;
        try {
            envelope = simaFeign.verifyCitizen(
                    SimaIddaConstants.EXAMPLE_SIMA_IDENTIFIER,
                    SimaIddaConstants.EXAMPLE_SIMA_AUTH_SCHEME,
                    signature,
                    SimaIddaConstants.EXAMPLE_SIMA_DEVICE_INFO,
                    minified
            );
        } catch (FeignException e) {
            envelope = parseFeignErrorEnvelope(e, idempotencyKey);
        }
        return handleEnvelope(customer, envelope, "citizen");
    }

    public SimaVerifyResponse verifyPassport(String userIdHeader, SimaPassportVerifyRequest request) {
        Customer customer = requireCustomer(userIdHeader);

        String idempotencyKey = UUID.randomUUID().toString();
        SimaPassportFeignBody body = SimaPassportFeignBody.builder()
                .pin(request.getPin())
                .documentNumber(request.getDocumentNumber())
                .livePhoto(request.getLivePhoto())
                .idempotencyKey(idempotencyKey)
                .build();

        String minified = SimaHmacSigner.minify(body);
        String signature = SimaHmacSigner.signBase64(minified);

        SimaApiEnvelope envelope;
        try {
            envelope = simaFeign.verifyPassport(
                    SimaIddaConstants.EXAMPLE_SIMA_IDENTIFIER,
                    SimaIddaConstants.EXAMPLE_SIMA_AUTH_SCHEME,
                    signature,
                    SimaIddaConstants.EXAMPLE_SIMA_DEVICE_INFO,
                    minified
            );
        } catch (FeignException e) {
            envelope = parseFeignErrorEnvelope(e, idempotencyKey);
        }
        return handleEnvelope(customer, envelope, "passport");
    }

    public SimaVerifyResponse verifyForeign(String userIdHeader, SimaForeignVerifyRequest request) {
        Customer customer = requireCustomer(userIdHeader);

        String idempotencyKey = UUID.randomUUID().toString();
        SimaForeignFeignBody body = SimaForeignFeignBody.builder()
                .pin(request.getPin())
                .livePhoto(request.getLivePhoto())
                .documentType(request.getDocumentType())
                .idempotencyKey(idempotencyKey)
                .build();

        String minified = SimaHmacSigner.minify(body);
        String signature = SimaHmacSigner.signBase64(minified);

        SimaApiEnvelope envelope;
        try {
            envelope = simaFeign.verifyForeign(
                    SimaIddaConstants.EXAMPLE_SIMA_IDENTIFIER,
                    SimaIddaConstants.EXAMPLE_SIMA_AUTH_SCHEME,
                    signature,
                    SimaIddaConstants.EXAMPLE_SIMA_DEVICE_INFO,
                    minified
            );
        } catch (FeignException e) {
            envelope = parseFeignErrorEnvelope(e, idempotencyKey);
        }
        return handleEnvelope(customer, envelope, "foreign");
    }

    private SimaApiEnvelope parseFeignErrorEnvelope(FeignException e, String idempotencyKey) {
        String content = e.contentUTF8();
        log.warn("SIMA Feign status={} bodySnippet={}", e.status(),
                content != null && content.length() > 400 ? content.substring(0, 400) : content);
        if (content != null && !content.isBlank()) {
            try {
                return objectMapper.readValue(content, SimaApiEnvelope.class);
            } catch (Exception parseEx) {
                log.warn("SIMA error body parse failed: {}", parseEx.getMessage());
            }
        }
        return SimaApiEnvelope.builder()
                .isSuccess(false)
                .error(SimaErrorBody.builder()
                        .httpStatus(e.status())
                        .errorMessage(e.getMessage())
                        .idempotencyKey(idempotencyKey)
                        .build())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private SimaVerifyResponse handleEnvelope(Customer customer, SimaApiEnvelope envelope, String channel) {
        if (envelope == null) {
            return SimaVerifyResponse.builder()
                    .verified(false)
                    .code("SIMA_EMPTY")
                    .message("Empty SIMA response")
                    .build();
        }

        if (!Boolean.TRUE.equals(envelope.getIsSuccess()) || envelope.getResult() == null) {
            Integer errorCode = envelope.getError() != null ? envelope.getError().getErrorCode() : null;
            String errorMessage = envelope.getError() != null ? envelope.getError().getErrorMessage() : "SIMA failed";
            log.warn("SIMA {} technical fail errorCode={} message={}", channel, errorCode, errorMessage);
            return SimaVerifyResponse.builder()
                    .verified(false)
                    .code(errorCode != null ? "SIMA_" + errorCode : "SIMA_FAIL")
                    .message(errorMessage)
                    .simaErrorCode(errorCode)
                    .transactionId(envelope.getError() != null ? envelope.getError().getTransactionId() : null)
                    .build();
        }

        SimaIdentityResult result = envelope.getResult();
        boolean biometricOk = Boolean.TRUE.equals(result.getLivenessStatus())
                && Boolean.TRUE.equals(result.getSimilarityStatus());

        if (!biometricOk) {
            log.info("SIMA {} isSuccess=true but biometric gate failed liveness={} similarity={}",
                    channel, result.getLivenessStatus(), result.getSimilarityStatus());
            return SimaVerifyResponse.builder()
                    .verified(false)
                    .pin(result.getPin())
                    .name(result.getName())
                    .surname(result.getSurname())
                    .livenessScore(result.getLivenessScore())
                    .similarityScore(result.getSimilarityScore())
                    .transactionId(result.getTransactionId())
                    .code("SIMA_BIOMETRIC_GATE")
                    .message("isSuccess alone is not verified; livenessStatus && similarityStatus required")
                    .build();
        }

        // --- apply onto Customer (fields may not exist on entity yet; intentional compile break for demo) ---
        applyVerifiedCustomerFields(customer, result);

        // Existing Customer fields — safe
        if (result.getName() != null) {
            customer.setName(result.getName());
        }
        if (result.getSurname() != null) {
            customer.setSurname(result.getSurname());
        }

        log.info("SIMA {} verified pin={} transactionId={} (no DB save in test_sima_idda stage)",
                channel, result.getPin(), result.getTransactionId());

        return SimaVerifyResponse.builder()
                .verified(true)
                .pin(result.getPin())
                .name(result.getName())
                .surname(result.getSurname())
                .livenessScore(result.getLivenessScore())
                .similarityScore(result.getSimilarityScore())
                .transactionId(result.getTransactionId())
                .code("OK")
                .message("Verified")
                .build();
    }

    /**
     * Calls setters that are expected on Customer after product adds FIN / KYC columns.
     * Intentionally references missing methods for patron source walkthrough.
     */
    private void applyVerifiedCustomerFields(Customer customer, SimaIdentityResult result) {
//        customer.setFin(result.getPin());
//        customer.setIsVerified(true);
//        customer.setFinVerified(true);
//        customer.setSimaTransactionId(result.getTransactionId());
//        customer.setLivenessScore(result.getLivenessScore());
//        customer.setSimilarityScore(result.getSimilarityScore());
//        customer.setGender(result.getGender());
//        customer.setDocumentNumber(result.getDocumentNumber());
//        customer.setPatronymic(result.getPatronymic());
        // DO NOT customerRepository.save(customer) — test stage, no DB mutation yet
    }

    private Customer requireCustomer(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("X-User-Id header required");
        }
        Customer customer = customerRepository.findByUserId(Long.valueOf(userIdHeader));
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found for userId=" + userIdHeader);
        }
        return customer;
    }

    private void validateCitizenXor(SimaCitizenVerifyRequest request) {
        boolean hasDoc = request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank();
        boolean hasBirth = request.getBirthDate() != null && !request.getBirthDate().isBlank();
        if (hasDoc == hasBirth) {
            throw new IllegalArgumentException(
                    "Exactly one of documentNumber or birthDate must be filled (HTML XOR rule; SIMA 713/714)");
        }
    }
}
