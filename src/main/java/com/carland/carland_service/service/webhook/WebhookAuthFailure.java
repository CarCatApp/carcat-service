package com.carland.carland_service.service.webhook;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebhookAuthFailure {

    INTERNAL_TOKEN_NOT_CONFIGURED(
            "Internal token not configured",
            "CARLAND_INTERNAL_TOKEN is not set on carland-service"
    ),
    MISSING_INTERNAL_TOKEN(
            "Missing internal token",
            "X-Internal-Token header is required for webhook-service requests"
    ),
    INVALID_INTERNAL_TOKEN(
            "Invalid internal token",
            "X-Internal-Token does not match CARLAND_INTERNAL_TOKEN"
    ),
    MISSING_PARTNER_ID(
            "Missing partnerId",
            "partnerId is required in JSON body or query string"
    ),
    PARTNER_NOT_FOUND(
            "Partner not found",
            "No active partner record for the given partnerId"
    ),
    PARTNER_INACTIVE(
            "Partner inactive",
            "Partner exists but active=false"
    ),
    PARTNER_WEBHOOK_SECRET_NOT_CONFIGURED(
            "Partner webhook secret not configured",
            "partners.webhook_secret is empty for this partnerId — set it in DB before accepting signed requests"
    ),
    MISSING_SIGNATURE_HEADER(
            "Missing signature",
            "X-Signature header is required"
    ),
    INVALID_SIGNATURE(
            "Invalid signature",
            "X-Signature does not match HMAC-SHA256 of the raw request bytes using partners.webhook_secret"
    );

    private final String error;
    private final String message;
}
