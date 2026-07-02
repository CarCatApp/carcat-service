package com.carland.carland_service.service.webhook;

import lombok.Getter;

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

    public static WebhookAuthValidationResult success() {
        return new WebhookAuthValidationResult(true, null, null);
    }

    public static WebhookAuthValidationResult success(Long partnerId) {
        return new WebhookAuthValidationResult(true, null, partnerId);
    }

    public static WebhookAuthValidationResult failure(WebhookAuthFailure failure) {
        return new WebhookAuthValidationResult(false, failure, null);
    }

    public static WebhookAuthValidationResult failure(WebhookAuthFailure failure, Long partnerId) {
        return new WebhookAuthValidationResult(false, failure, partnerId);
    }
}
