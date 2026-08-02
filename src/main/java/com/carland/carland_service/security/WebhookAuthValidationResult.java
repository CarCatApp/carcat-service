package com.carland.carland_service.security;

import lombok.Getter;

/**
 * tr: Webhook kimlik doğrulamasının sonucunu taşıyan değişmez (immutable) değer nesnesidir;
 *     geçerlilik durumu, başarısızlık sebebi ve varsa partnerId bilgisini içerir.
 * en: Immutable value object carrying the webhook authentication result; holds validity flag,
 *     failure reason and the partnerId when available.
 */
@Getter
public final class WebhookAuthValidationResult {

    private final boolean valid;
    private final WebhookAuthFailure failure;
    private final Long partnerId;

    private WebhookAuthValidationResult(boolean valid, WebhookAuthFailure failure, Long partnerId) {
        this.valid = valid;
        this.failure = failure;
        this.partnerId = partnerId;
    }

    /**
     * tr: partnerId'siz başarılı sonuç üretir (internal token aşaması için).
     * en: Produces a successful result without partnerId (for the internal token stage).
     */
    public static WebhookAuthValidationResult success() {
        return new WebhookAuthValidationResult(true, null, null);
    }

    /**
     * tr: Doğrulanan partnerId ile başarılı sonuç üretir.
     * en: Produces a successful result with the validated partnerId.
     */
    public static WebhookAuthValidationResult success(Long partnerId) {
        return new WebhookAuthValidationResult(true, null, partnerId);
    }

    /**
     * tr: partnerId'siz başarısız sonuç üretir.
     * en: Produces a failed result without partnerId.
     */
    public static WebhookAuthValidationResult failure(WebhookAuthFailure failure) {
        return new WebhookAuthValidationResult(false, failure, null);
    }

    /**
     * tr: İlgili partnerId ile başarısız sonuç üretir.
     * en: Produces a failed result with the related partnerId.
     */
    public static WebhookAuthValidationResult failure(WebhookAuthFailure failure, Long partnerId) {
        return new WebhookAuthValidationResult(false, failure, partnerId);
    }
}
