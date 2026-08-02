package com.carland.carland_service.exceptions;


import lombok.Getter;
import lombok.Setter;

/**
 * tr: Kullanıcı adı/kullanıcı zaten kayıtlı olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 409 (Conflict) yanıtına çevrilir.
 * en: Exception thrown when the username/user is already registered; mapped to HTTP 409 (Conflict) by CustomExceptionHandler.
 */
@Getter
@Setter
public class UsernameAlreadyExistException extends RuntimeException{

    public UsernameAlreadyExistException(String message){
       super(message);
    }
}
