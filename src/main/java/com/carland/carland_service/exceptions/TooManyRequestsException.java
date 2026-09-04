package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: İstek sıklığı limiti aşıldığında fırlatılır; HTTP 429.
 * en: Thrown when a rate limit is exceeded; mapped to HTTP 429.
 */
@Getter
@Setter
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
