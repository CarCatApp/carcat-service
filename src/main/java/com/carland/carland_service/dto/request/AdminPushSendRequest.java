package com.carland.carland_service.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * tr: Admin push gönderim isteği: başlık, gövde ve opsiyonel AND kitle filtreleri.
 * en: Admin push send request: title, body, and optional AND audience filters.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminPushSendRequest extends AdminPushAudienceRequest {
    String title;
    String body;
}
