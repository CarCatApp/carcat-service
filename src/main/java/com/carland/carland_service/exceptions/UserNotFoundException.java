package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Kullanıcı bulunamadığında fırlatılan exception; CustomExceptionHandler tarafından HTTP 404 (Not Found) yanıtına çevrilir.
 * en: Exception thrown when the user is not found; mapped to HTTP 404 (Not Found) by CustomExceptionHandler.
 */
@Getter
@Setter
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
