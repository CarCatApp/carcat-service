package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Davet kodu hatalarında fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına, UICustomExceptionHandler tarafından "invite-error" sayfasına çevrilir.
 * en: Exception thrown on invite code errors; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler and to the "invite-error" page by UICustomExceptionHandler.
 */
@Getter
@Setter
public class InviteException extends RuntimeException {
    public InviteException(String message) {
        super(message);
    }
}
