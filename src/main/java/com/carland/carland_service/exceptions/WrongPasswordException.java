package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Şifre yanlış olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 401 (Unauthorized) yanıtına çevrilir.
 * en: Exception thrown when the password is wrong; mapped to HTTP 401 (Unauthorized) by CustomExceptionHandler.
 */
@Getter
@Setter
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException(String message){
        super(message);
    }

}
