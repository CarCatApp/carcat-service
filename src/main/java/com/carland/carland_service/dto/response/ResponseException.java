package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * tr: Global exception handler'ların (CustomExceptionHandler) tüm hata cevaplarında kullandığı ortak
 *     JSON gövdesidir (hata başlığı, mesaj, zaman damgası, HTTP status).
 * en: Common JSON body used by the global exception handlers (CustomExceptionHandler) for all error
 *     responses (error title, message, timestamp, HTTP status).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseException {
    private String error;
    private String message;
    private LocalDateTime timeStamp;
    private Integer status;
}
