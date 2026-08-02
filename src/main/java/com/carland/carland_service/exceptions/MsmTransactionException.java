package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: OTP SMS gönderim işlemi başarısız olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when the OTP SMS send operation fails; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Getter
@Setter
public class MsmTransactionException extends RuntimeException {
    public MsmTransactionException(String message) {
        super(message);
    }
}
