package com.carland.carland_service.service.webhook;

import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.util.HmacSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerWebhookSignatureService {

    private static final String PARTNER_ID_PARAM = "partnerId";

    private final PartnerRepository partnerRepository;
    private final HmacSignatureValidator hmacSignatureValidator;
    private final ObjectMapper objectMapper;

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

        String provided = request.getHeader(HmacSignatureValidator.HEADER_NAME);
        if (!StringUtils.hasText(provided)) {
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.MISSING_SIGNATURE_HEADER, partnerId);
        }

        byte[] payload = hmacSignatureValidator.resolvePayload(request, body);
        if (!hmacSignatureValidator.isValid(secret, payload, provided.trim())) {
            log.warn(
                    "Partner webhook signature mismatch: partnerId={}, path={}, payloadBytes={}",
                    partnerId,
                    request.getRequestURI(),
                    payload.length
            );
            return WebhookAuthValidationResult.failure(WebhookAuthFailure.INVALID_SIGNATURE, partnerId);
        }

        return WebhookAuthValidationResult.success(partnerId);
    }

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
