package com.carland.carland_service.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * tr: İstek sıklığı limiti aşıldığında fırlatılır; HTTP 429.
 * en: Thrown when a rate limit is exceeded; mapped to HTTP 429.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {
    private final LocalDateTime lockedUntil;
    private final Long remainingSeconds;

    public TooManyRequestsException(String message) {
        this(message, null, null);
    }

    public TooManyRequestsException(String message, LocalDateTime lockedUntil, Long remainingSeconds) {
        super(message);
        this.lockedUntil = lockedUntil;
        this.remainingSeconds = remainingSeconds;
    }
}
