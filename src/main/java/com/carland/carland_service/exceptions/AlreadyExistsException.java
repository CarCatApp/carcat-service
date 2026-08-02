package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Kayıt zaten mevcut olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 409 (Conflict) yanıtına çevrilir.
 * en: Exception thrown when a record already exists; mapped to HTTP 409 (Conflict) by CustomExceptionHandler.
 */
@Getter
@Setter
public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String message) {
        super(message);
    }
}
