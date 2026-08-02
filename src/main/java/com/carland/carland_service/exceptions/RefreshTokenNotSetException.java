package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Refresh token bulunamadığında fırlatılan exception; CustomExceptionHandler tarafından HTTP 401 (Unauthorized) yanıtına çevrilir.
 * en: Exception thrown when the refresh token is not set; mapped to HTTP 401 (Unauthorized) by CustomExceptionHandler.
 */
@Setter
@Getter
public class RefreshTokenNotSetException extends RuntimeException{

    public RefreshTokenNotSetException(String message){

        super(message);
    }
}
