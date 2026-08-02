package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Aranan kaynak (araç, takvim, servis vb.) bulunamadığında fırlatılan exception; CustomExceptionHandler tarafından HTTP 404 (Not Found) yanıtına çevrilir.
 * en: Exception thrown when a requested resource (car, calendar, service, etc.) is not found; mapped to HTTP 404 (Not Found) by CustomExceptionHandler.
 */
@Getter
@Setter
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

