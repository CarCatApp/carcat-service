package com.carland.carland_service.security;

import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.PartnerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * tr: Partner webhook isteklerinin HMAC imza doğrulamasını yapan servistir; partnerId'yi query veya
 *     body'den çözer, partner'ın aktifliğini ve webhook secret'ını kontrol eder, X-Signature imzasını doğrular.
 * en: Service performing HMAC signature validation of partner webhook requests; resolves partnerId from
 *     query or body, checks partner activeness and webhook secret, validates the X-Signature header.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerWebhookSignatureService {

    private static final String PARTNER_ID_PARAM = "partnerId";

    private final PartnerRepository partnerRepository;
    private final HmacSignatureValidator hmacSignatureValidator;
    private final ObjectMapper objectMapper;

    /**
     * tr: Sırasıyla kontrol eder: partnerId çözülemezse MISSING_PARTNER_ID, partner yoksa PARTNER_NOT_FOUND,
     *     aktif değilse PARTNER_INACTIVE, secret tanımsızsa SECRET_NOT_CONFIGURED, imza header'ı yoksa
     *     MISSING_SIGNATURE_HEADER, imza tutmazsa INVALID_SIGNATURE; hepsi geçerse başarı döner.
     * en: Checks in order: MISSING_PARTNER_ID when partnerId cannot be resolved, PARTNER_NOT_FOUND when the
     *     partner does not exist, PARTNER_INACTIVE when inactive, SECRET_NOT_CONFIGURED when the secret is
     *     unset, MISSING_SIGNATURE_HEADER when the signature header is absent, INVALID_SIGNATURE on mismatch;
     *     success when all pass.
     */
    public WebhookAuthValidationResult validate(HttpServletRequest request, byte[] body) {
        Long partnerId = resolvePartnerId(request, body);
        if (partnerId == null) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.MISSING_PARTNER_ID);
        }

        Partner partner = partnerRepository.findById(partnerId).orElse(null);
        if (partner == null) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.PARTNER_NOT_FOUND, partnerId);
        }
        if (!Boolean.TRUE.equals(partner.getActive())) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.PARTNER_INACTIVE, partnerId);
        }

        String secret = partner.getWebhookSecret();
        if (!StringUtils.hasText(secret)) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.PARTNER_WEBHOOK_SECRET_NOT_CONFIGURED, partnerId);
        }
        secret = secret.trim();

        String provided = request.getHeader(HmacSignatureValidator.HEADER_NAME);
        if (!StringUtils.hasText(provided)) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.MISSING_SIGNATURE_HEADER, partnerId);
        }

        byte[] payload = hmacSignatureValidator.resolvePayload(request, body);
        if (!hmacSignatureValidator.isValid(secret, payload, provided.trim())) {
            log.warn(
                    "Partner webhook signature mismatch: partnerId={}, secretSource=db, secretLength={}, path={}, payloadBytes={}, payloadSha256Prefix={}",
                    partnerId,
                    secret.length(),
                    request.getRequestURI(),
                    payload.length,
                    hmacSignatureValidator.sha256Prefix(payload)
            );
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.INVALID_SIGNATURE, partnerId);
        }

        return WebhookAuthValidationResult.success(partnerId);
    }

    /**
     * tr: partnerId'yi önce query parametresinden, yoksa JSON body'den çözer; sayı değilse null döner.
     * en: Resolves partnerId from the query parameter first, then from the JSON body; returns null when not numeric.
     */
    private Long resolvePartnerId(HttpServletRequest request, byte[] body) {
        String partnerIdParam = request.getParameter(PARTNER_ID_PARAM);
        if (StringUtils.hasText(partnerIdParam)) {
            try {
                return Long.parseLong(partnerIdParam.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return extractPartnerIdFromBody(body);
    }

    /**
     * tr: JSON body'deki partnerId alanını okur; body boşsa, alan yoksa veya long'a çevrilemezse null döner.
     * en: Reads the partnerId field from the JSON body; returns null when the body is empty, the field is
     *     missing or not convertible to long.
     */
    private Long extractPartnerIdFromBody(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode partnerIdNode = root.get("partnerId");
            if (partnerIdNode == null || partnerIdNode.isNull()) {
                return null;
            }
            if (!partnerIdNode.canConvertToLong()) {
                return null;
            }
            return partnerIdNode.asLong();
        } catch (IOException e) {
            log.warn("Failed to parse partnerId from webhook body: {}", e.getMessage());
            return null;
        }
    }
}
