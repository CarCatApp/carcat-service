package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Kullanıcı yaş sınırının altında olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when the user is under the age limit; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Setter
@Getter
public class UnderageException extends RuntimeException{

    public UnderageException(String message){

        super(message);
    }
}
