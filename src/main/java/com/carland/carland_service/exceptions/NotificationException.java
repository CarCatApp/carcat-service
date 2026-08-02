package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Push bildirimi gönderimi başarısız olduğunda fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown when push notification sending fails; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Getter
@Setter
public class NotificationException extends RuntimeException {
    public NotificationException(String message) {
        super(message);
    }
}
