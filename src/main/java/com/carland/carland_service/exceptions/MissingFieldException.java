package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Zorunlu alan eksik olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when a required field is missing; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Getter
@Setter
public class MissingFieldException extends RuntimeException {

    public MissingFieldException(String message) {
        super(message);
    }

    public static MissingFieldException required(String fieldName) {
        return new MissingFieldException(fieldName + " is required");
    }
}

