package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: OTP kodunun süresi dolduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when the OTP code has expired; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Setter
@Getter
public class ExpiredOtpException extends RuntimeException {
    public ExpiredOtpException(String message) {
        super(message);
    }
}
