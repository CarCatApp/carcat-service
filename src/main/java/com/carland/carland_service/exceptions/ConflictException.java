package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Veri çakışması durumlarında fırlatılan genel exception; CustomExceptionHandler tarafından HTTP 409 (Conflict) yanıtına çevrilir.
 * en: Generic exception thrown on data conflicts; mapped to HTTP 409 (Conflict) by CustomExceptionHandler.
 */
@Getter
@Setter
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
