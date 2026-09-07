package com.carland.carland_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    /** Present on HTTP 429 (OTP / AI generate lock). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime lockedUntil;
    /** Seconds left until unlock — Flutter countdown. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long remainingSeconds;
    /** Alias used by OTP contract (same as remainingSeconds). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long retryAfter;
}
