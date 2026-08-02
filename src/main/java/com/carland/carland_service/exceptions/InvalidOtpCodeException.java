package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Girilen OTP kodu geçersiz olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when the entered OTP code is invalid; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Getter
@Setter
public class InvalidOtpCodeException extends RuntimeException {
    public InvalidOtpCodeException(String message) {
        super(message);
    }
}
