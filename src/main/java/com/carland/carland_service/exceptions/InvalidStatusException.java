package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Kullanıcı/kayıt durumu işlem için uygun olmadığında fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when a user/record status is not valid for the operation; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Getter
@Setter
public class InvalidStatusException extends RuntimeException {
    public InvalidStatusException(String message) {
        super(message);
    }
}
