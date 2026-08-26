package com.carland.carland_service.test_sima_idda.service;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.SimaKycRecord;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.SimaKycRecordRepository;
import com.carland.carland_service.test_sima_idda.SimaVerificationGate;
import com.carland.carland_service.test_sima_idda.config.SimaIddaProperties;
import com.carland.carland_service.test_sima_idda.dto.request.SimaCitizenVerifyRequest;
import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyOutcome;
import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyResponse;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaCitizenFeignBody;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaErrorBody;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaForeignFeignBody;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaIdentityResult;
import com.carland.carland_service.test_sima_idda.feign.SimaFeign;
import com.carland.carland_service.test_sima_idda.hmac.SimaHmacSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * SIMA KYC: citizen/foreign persist every attempt; profile is updated only when verified.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimaKycService {

    private final SimaFeign simaFeign;
    private final CustomerRepository customerRepository;
    private final SimaKycRecordRepository simaKycRecordRepository;
    private final ObjectMapper objectMapper;
    private final SimaHmacSigner simaHmacSigner;
    private final SimaIddaProperties simaIddaProperties;

    public SimaApiEnvelope testIdentityVerify(
            String pin,
            String documentNumber,
            String birthDate,
            MultipartFile photo
    ) {
        return callCitizen(pin, documentNumber, birthDate, photo).envelope;
    }

    @Transactional
    public SimaVerifyOutcome verifyCitizen(
            String userIdHeader,
            String pin,
            String documentNumber,
            String birthDate,
            MultipartFile photo,
            String acceptLanguage
    ) {
        Customer customer = requireCustomer(userIdHeader);
        SimaVerifyOutcome blocked = preflight(customer, pin, acceptLanguage);
        if (blocked != null) {
            return blocked;
        }
        SimaCall call = callCitizen(pin, documentNumber, birthDate, photo);
        return finishAttempt(customer, call, "CITIZEN", acceptLanguage);
    }

    @Transactional
    public SimaVerifyOutcome verifyForeign(
            String userIdHeader,
            String pin,
            String documentType,
            MultipartFile photo,
            String acceptLanguage
    ) {
        Customer customer = requireCustomer(userIdHeader);
        validateForeignDocumentType(documentType);
        SimaVerifyOutcome blocked = preflight(customer, pin, acceptLanguage);
        if (blocked != null) {
            return blocked;
        }
        SimaCall call = callForeign(pin, documentType, photo);
        return finishAttempt(customer, call, "FOREIGN", acceptLanguage);
    }

    private SimaVerifyOutcome preflight(Customer customer, String pin, String acceptLanguage) {
        if (Boolean.TRUE.equals(customer.getSimaVerified())) {
            return SimaVerifyOutcome.builder()
                    .httpStatus(HttpStatus.OK.value())
                    .body(SimaVerifyResponse.builder()
                            .verified(true)
                            .pin(customer.getPin())
                            .name(customer.getName())
                            .surname(customer.getSurname())
                            .code("SIMA_ALREADY_VERIFIED")
                            .message(MessagesLangValues.SIMA_ALREADY_VERIFIED.getMessageByLang(acceptLanguage))
                            .build())
                    .build();
        }
        if (pinTakenByOther(pin, customer.getUserId())) {
            persistPinTaken(customer, pin, "PRECHECK");
            return pinTakenOutcome(pin, acceptLanguage);
        }
        return null;
    }

    private SimaVerifyOutcome finishAttempt(
            Customer customer,
            SimaCall call,
            String channel,
            String acceptLanguage
    ) {
        SimaApiEnvelope envelope = call.envelope;
        SimaIdentityResult result = envelope != null ? envelope.getResult() : null;
        SimaErrorBody error = envelope != null ? envelope.getError() : null;
        Integer simaCode = error != null ? error.getErrorCode() : null;
        String simaMessage = error != null ? error.getErrorMessage() : null;
        String transactionId = firstNonBlank(
                result != null ? result.getTransactionId() : null,
                error != null ? error.getTransactionId() : null
        );
        boolean verified = SimaVerificationGate.attemptVerified(envelope);

        String outcome;
        int httpStatus;
        boolean applyProfile = false;
        String appCode;
        String appMessage;

        if (envelope == null) {
            outcome = "SIMA_EMPTY";
            httpStatus = call.httpStatus > 0 ? call.httpStatus : 502;
            appCode = "SIMA_EMPTY";
            appMessage = "Empty SIMA response";
        } else if (!Boolean.TRUE.equals(envelope.getIsSuccess()) || result == null) {
            outcome = "SIMA_ERROR";
            httpStatus = resolveSimaHttp(error, call.httpStatus);
            appCode = simaCode != null ? "SIMA_" + simaCode : "SIMA_FAIL";
            appMessage = simaMessage != null ? simaMessage : "SIMA failed";
        } else if (!verified) {
            outcome = "SCORE_GATE";
            httpStatus = HttpStatus.OK.value();
            appCode = "SIMA_SCORE_GATE";
            appMessage = "livenessScore and similarityScore must both be >= 0.90";
        } else if (pinTakenByOther(result.getPin(), customer.getUserId())) {
            outcome = "PIN_TAKEN";
            httpStatus = HttpStatus.CONFLICT.value();
            appCode = "PIN_ALREADY_EXISTS";
            appMessage = MessagesLangValues.PIN_ALREADY_EXISTS.getMessageByLang(acceptLanguage);
        } else {
            outcome = "VERIFIED";
            httpStatus = HttpStatus.OK.value();
            applyProfile = true;
            appCode = "OK";
            appMessage = "Verified";
        }

        persistRecord(customer, channel, call.idempotencyKey, envelope, result, error,
                call.httpStatus, verified && applyProfile, applyProfile, outcome);

        if (applyProfile) {
            applyVerifiedProfile(customer, result);
        }

        return SimaVerifyOutcome.builder()
                .httpStatus(httpStatus)
                .body(SimaVerifyResponse.builder()
                        .verified(applyProfile)
                        .pin(result != null ? result.getPin() : null)
                        .name(result != null ? result.getName() : null)
                        .surname(result != null ? result.getSurname() : null)
                        .livenessScore(result != null ? result.getLivenessScore() : null)
                        .similarityScore(result != null ? result.getSimilarityScore() : null)
                        .transactionId(transactionId)
                        .code(appCode)
                        .message(appMessage)
                        .simaResponseCode(simaCode)
                        .simaMessage(simaMessage)
                        .simaErrorCode(simaCode)
                        .build())
                .build();
    }

    private void applyVerifiedProfile(Customer customer, SimaIdentityResult result) {
        if (result.getName() != null) {
            customer.setName(result.getName());
        }
        if (result.getSurname() != null) {
            customer.setSurname(result.getSurname());
        }
        if (result.getPin() != null && !result.getPin().isBlank()) {
            customer.setPin(result.getPin().trim().toUpperCase());
        }
        customer.setSimaVerified(true);
        customerRepository.save(customer);
        log.info("SIMA verified userId={} pin={} transactionId={}",
                customer.getUserId(), result.getPin(), result.getTransactionId());
    }

    private void persistRecord(
            Customer customer,
            String channel,
            String idempotencyKey,
            SimaApiEnvelope envelope,
            SimaIdentityResult result,
            SimaErrorBody error,
            int simaHttpStatus,
            boolean verified,
            boolean applied,
            String outcome
    ) {
        if (simaKycRecordRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.info("SIMA idempotency hit key={} — skip insert", idempotencyKey);
            return;
        }
        SimaKycRecord.SimaKycRecordBuilder row = SimaKycRecord.builder()
                .customer(customer)
                .channel(channel)
                .verified(verified)
                .appliedToProfile(applied)
                .idempotencyKey(idempotencyKey)
                .simaHttpStatus(simaHttpStatus > 0 ? simaHttpStatus : null)
                .simaResponseCode(error != null ? error.getErrorCode() : null)
                .simaMessage(error != null ? error.getErrorMessage() : null)
                .outcome(outcome)
                .createdAt(LocalDateTime.now());
        if (result != null) {
            row.transactionId(result.getTransactionId())
                    .processTime(result.getProcessTime())
                    .pin(result.getPin())
                    .documentNumber(result.getDocumentNumber())
                    .name(result.getName())
                    .surname(result.getSurname())
                    .patronymic(result.getPatronymic())
                    .birthDate(result.getBirthDate())
                    .birthAddress(result.getBirthAddress())
                    .address(result.getAddress())
                    .nationality(result.getNationality())
                    .gender(result.getGender())
                    .expDate(result.getExpDate())
                    .documentType(result.getDocumentType())
                    .issuingCountry(result.getIssuingCountry())
                    .livenessScore(result.getLivenessScore())
                    .livenessStatus(result.getLivenessStatus())
                    .livenessFailureReason(result.getLivenessFailureReason())
                    .similarityScore(result.getSimilarityScore())
                    .similarityStatus(result.getSimilarityStatus());
        } else if (error != null) {
            row.transactionId(error.getTransactionId())
                    .processTime(error.getProcessTime());
        }
        simaKycRecordRepository.save(row.build());
    }

    private void persistPinTaken(Customer customer, String pin, String channel) {
        simaKycRecordRepository.save(SimaKycRecord.builder()
                .customer(customer)
                .channel(channel)
                .verified(false)
                .appliedToProfile(false)
                .idempotencyKey(UUID.randomUUID().toString())
                .pin(pin)
                .outcome("PIN_TAKEN")
                .createdAt(LocalDateTime.now())
                .build());
    }

    private SimaVerifyOutcome pinTakenOutcome(String pin, String acceptLanguage) {
        return SimaVerifyOutcome.builder()
                .httpStatus(HttpStatus.CONFLICT.value())
                .body(SimaVerifyResponse.builder()
                        .verified(false)
                        .pin(pin)
                        .code("PIN_ALREADY_EXISTS")
                        .message(MessagesLangValues.PIN_ALREADY_EXISTS.getMessageByLang(acceptLanguage))
                        .build())
                .build();
    }

    private boolean pinTakenByOther(String pin, Long userId) {
        if (pin == null || pin.isBlank()) {
            return false;
        }
        Customer owner = customerRepository.findByPinIgnoreCase(pin.trim());
        return owner != null && !owner.getUserId().equals(userId);
    }

    private SimaCall callCitizen(String pin, String documentNumber, String birthDate, MultipartFile photo) {
        SimaCitizenVerifyRequest request = SimaCitizenVerifyRequest.builder()
                .pin(pin)
                .documentNumber(blankToNull(documentNumber))
                .birthDate(blankToNull(birthDate))
                .livePhoto(toJpegBase64(photo))
                .build();
        validateCitizenXor(request);

        String idempotencyKey = UUID.randomUUID().toString();
        SimaCitizenFeignBody body = SimaCitizenFeignBody.builder()
                .pin(request.getPin())
                .documentNumber(request.getDocumentNumber())
                .birthDate(request.getBirthDate())
                .livePhoto(request.getLivePhoto())
                .idempotencyKey(idempotencyKey)
                .build();

        String minified = SimaHmacSigner.minify(body);
        String signature = simaHmacSigner.signBase64(minified);
        log.info("SIMA citizen verify idempotencyKey={} photoBytes={} bodyBytes={}",
                idempotencyKey, photo.getSize(), minified.length());
        try {
            SimaApiEnvelope envelope = simaFeign.verifyCitizen(
                    simaIddaProperties.getSimaIdentifier(),
                    simaIddaProperties.getSimaAuthScheme(),
                    signature,
                    simaIddaProperties.getSimaDeviceInfo(),
                    minified
            );
            return new SimaCall(envelope, idempotencyKey, 200);
        } catch (FeignException e) {
            return parseFeignError(e, idempotencyKey);
        }
    }

    private SimaCall callForeign(String pin, String documentType, MultipartFile photo) {
        String idempotencyKey = UUID.randomUUID().toString();
        SimaForeignFeignBody body = SimaForeignFeignBody.builder()
                .pin(pin)
                .livePhoto(toJpegBase64(photo))
                .documentType(documentType.trim().toUpperCase())
                .idempotencyKey(idempotencyKey)
                .build();
        String minified = SimaHmacSigner.minify(body);
        String signature = simaHmacSigner.signBase64(minified);
        log.info("SIMA foreign verify user pin={} documentType={} idempotencyKey={}",
                pin, body.getDocumentType(), idempotencyKey);
        try {
            SimaApiEnvelope envelope = simaFeign.verifyForeign(
                    simaIddaProperties.getSimaIdentifier(),
                    simaIddaProperties.getSimaAuthScheme(),
                    signature,
                    simaIddaProperties.getSimaDeviceInfo(),
                    minified
            );
            return new SimaCall(envelope, idempotencyKey, 200);
        } catch (FeignException e) {
            return parseFeignError(e, idempotencyKey);
        }
    }

    private SimaCall parseFeignError(FeignException e, String idempotencyKey) {
        int status = e.status() > 0 ? e.status() : 502;
        String content = e.contentUTF8();
        log.warn("SIMA Feign status={} bodySnippet={}", status,
                content != null && content.length() > 400 ? content.substring(0, 400) : content);
        SimaApiEnvelope envelope = null;
        if (content != null && !content.isBlank()) {
            try {
                envelope = objectMapper.readValue(content, SimaApiEnvelope.class);
            } catch (Exception parseEx) {
                log.warn("SIMA error body parse failed: {}", parseEx.getMessage());
            }
        }
        if (envelope == null) {
            envelope = SimaApiEnvelope.builder()
                    .isSuccess(false)
                    .error(SimaErrorBody.builder()
                            .httpStatus(status)
                            .errorMessage(e.getMessage())
                            .idempotencyKey(idempotencyKey)
                            .build())
                    .build();
        }
        return new SimaCall(envelope, idempotencyKey, status);
    }

    private static int resolveSimaHttp(SimaErrorBody error, int feignStatus) {
        if (error != null && error.getHttpStatus() != null && error.getHttpStatus() > 0) {
            return error.getHttpStatus();
        }
        if (feignStatus > 0) {
            return feignStatus;
        }
        return 400;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private String toJpegBase64(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            String name = photo == null ? "null" : photo.getOriginalFilename();
            long size = photo == null ? -1 : photo.getSize();
            String ct = photo == null ? "null" : photo.getContentType();
            throw new MissingFieldException(
                    "photo file required/empty (name=" + name + ", size=" + size + ", contentType=" + ct
                            + "). Postman: form-data key must be 'photo', type File.");
        }
        try {
            byte[] bytes = photo.getBytes();
            if (bytes.length > 1024 * 1024) {
                throw new MissingFieldException("photo must be <= 1MB (SIMA limit), got=" + bytes.length);
            }
            if (bytes.length < 2 || bytes[0] != (byte) 0xFF || bytes[1] != (byte) 0xD8) {
                throw new MissingFieldException("photo must be JPEG (SOI FF D8)");
            }
            return Base64.getEncoder().encodeToString(bytes);
        } catch (MissingFieldException e) {
            throw e;
        } catch (Exception e) {
            throw new MissingFieldException("Failed to read photo: " + e.getMessage());
        }
    }

    private Customer requireCustomer(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new MissingFieldException("X-User-Id header required");
        }
        Customer customer = customerRepository.findByUserId(Long.valueOf(userIdHeader));
        if (customer == null) {
            throw new ResourceNotFoundException("Customer not found for userId=" + userIdHeader);
        }
        return customer;
    }

    private void validateCitizenXor(SimaCitizenVerifyRequest request) {
        boolean hasDoc = request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank();
        boolean hasBirth = request.getBirthDate() != null && !request.getBirthDate().isBlank();
        if (hasDoc == hasBirth) {
            throw new MissingFieldException(
                    "Exactly one of documentNumber or birthDate must be filled (HTML XOR rule; SIMA 713/714)");
        }
    }

    private void validateForeignDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            throw new MissingFieldException("documentType required: TRC, PRC or ERP");
        }
        String normalized = documentType.trim().toUpperCase();
        if (!normalized.equals("TRC") && !normalized.equals("PRC") && !normalized.equals("ERP")) {
            throw new MissingFieldException("documentType must be one of: TRC, PRC, ERP");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record SimaCall(SimaApiEnvelope envelope, String idempotencyKey, int httpStatus) {
    }
}
