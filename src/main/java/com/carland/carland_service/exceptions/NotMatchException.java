package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Veriler beklenen değerle eşleşmediğinde (örn. araç kullanıcıya ait değil) fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when data does not match the expected value (e.g. car does not belong to the user); mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Getter
@Setter
public class NotMatchException extends RuntimeException {
    public NotMatchException(String message) {
        super(message);
    }
}
