package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * tr: Dosya/fotoğraf kaydetme veya okuma hatalarında fırlatılan exception; CustomExceptionHandler tarafından HTTP 400 (Bad Request) yanıtına çevrilir.
 * en: Exception thrown on file/photo storage or read errors; mapped to HTTP 400 (Bad Request) by CustomExceptionHandler.
 */
@Setter
@Getter
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }
}
