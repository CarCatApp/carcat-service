package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: NotificationController bildirim sorgularında dönen yanıt DTO'su (id, tip, bildirim metni).
 * en: Response DTO returned by NotificationController notification queries (id, type, notification text).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    Long id;
    String type;
    String notificationText;
}
