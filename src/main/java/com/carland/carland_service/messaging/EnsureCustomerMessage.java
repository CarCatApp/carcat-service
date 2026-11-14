package com.carland.carland_service.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Cross-service JSON (auth publisher → carland consumer). Field names must stay stable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnsureCustomerMessage(
        Long userId,
        String phoneNumber,
        String acceptLanguage,
        int attempt
) {
    public EnsureCustomerMessage nextAttempt() {
        return new EnsureCustomerMessage(userId, phoneNumber, acceptLanguage, attempt + 1);
    }
}
