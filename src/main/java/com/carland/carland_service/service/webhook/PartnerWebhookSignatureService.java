package com.carland.carland_service.service.webhook;

import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.util.HmacSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PartnerWebhookSignatureService {

    private static final String PARTNER_ID_PARAM = "partnerId";

    private final PartnerRepository partnerRepository;
    private final HmacSignatureValidator hmacSignatureValidator;
    private final ObjectMapper objectMapper;

    public boolean isValid(HttpServletRequest request, byte[] body) {
        Long partnerId = resolvePartnerId(request, body);
        if (partnerId == null) {
            return false;
        }

        Partner partner = partnerRepository.findById(partnerId).orElse(null);
        if (partner == null || !Boolean.TRUE.equals(partner.getActive())) {
            return false;
        }

        String secret = partner.getWebhookSecret();
        if (!StringUtils.hasText(secret)) {
            return false;
        }

        String provided = request.getHeader(HmacSignatureValidator.HEADER_NAME);
        if (!StringUtils.hasText(provided)) {
            return false;
        }

        byte[] payload = hmacSignatureValidator.resolvePayload(request, body);
        return hmacSignatureValidator.isValid(secret, payload, provided.trim());
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
            return null;
        }
    }
}
