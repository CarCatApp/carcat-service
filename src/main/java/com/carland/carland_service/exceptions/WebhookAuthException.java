package com.carland.carland_service.exceptions;

import com.carland.carland_service.security.WebhookAuthFailure;
import lombok.Getter;

/**
 * tr: Partner webhook isteğinin kimlik doğrulaması (imza/partner kontrolü) başarısız olduğunda fırlatılan exception; WebhookAuthExceptionHandler tarafından HTTP 401 (Unauthorized) yanıtına çevrilir.
 * en: Exception thrown when partner webhook request authentication (signature/partner check) fails; mapped to HTTP 401 (Unauthorized) by WebhookAuthExceptionHandler.
 */
@Getter
public class WebhookAuthException extends RuntimeException {

    private final WebhookAuthFailure failure;
    private final Long partnerId;

    public WebhookAuthException(WebhookAuthFailure failure) {
        this(failure, null);
    }

    public WebhookAuthException(WebhookAuthFailure failure, Long partnerId) {
        super(resolveMessage(failure, partnerId));
        this.failure = failure;
        this.partnerId = partnerId;
    }

    private static String resolveMessage(WebhookAuthFailure failure, Long partnerId) {
        if (partnerId == null) {
            return failure.getMessage();
        }
        return switch (failure) {
            case PARTNER_NOT_FOUND -> "No partner record for partnerId=" + partnerId;
            case PARTNER_INACTIVE -> "Partner is inactive for partnerId=" + partnerId;
            case PARTNER_WEBHOOK_SECRET_NOT_CONFIGURED ->
                    "partners.webhook_secret is not configured for partnerId=" + partnerId;
            case MISSING_SIGNATURE_HEADER -> "X-Signature header is required for partnerId=" + partnerId;
            case INVALID_SIGNATURE ->
                    "X-Signature does not match partners.webhook_secret for partnerId=" + partnerId
                            + " — sign the exact raw request bytes with that partner's secret";
            default -> failure.getMessage();
        };
    }
}
