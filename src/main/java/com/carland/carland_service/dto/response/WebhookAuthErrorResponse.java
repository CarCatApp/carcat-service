package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * tr: Webhook kimlik doğrulama hatalarında dönen 401 JSON gövdesinin modelidir (hata kodu, mesaj,
 *     partnerId, zaman damgası, HTTP status).
 * en: Model of the 401 JSON body returned on webhook authentication failures (error code, message,
 *     partnerId, timestamp, HTTP status).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookAuthErrorResponse {
    private String error;
    private String message;
    private Long partnerId;
    private LocalDateTime timeStamp;
    private Integer status;
}
