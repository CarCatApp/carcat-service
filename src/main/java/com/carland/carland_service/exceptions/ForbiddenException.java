package com.carland.carland_service.exceptions;

/**
 * tr: Yetkisiz erişim; CustomExceptionHandler HTTP 403 döner.
 * en: Forbidden access; CustomExceptionHandler maps to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
