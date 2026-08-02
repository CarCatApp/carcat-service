package com.carland.carland_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Toplu push bildirimi gönderiminin sonucunu (toplam/başarılı/başarısız sayıları) döndüren yanıt DTO'su.
 * en: Response DTO returning the result of a bulk push notification send (total/success/failed counts).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkPushResponse {
    String message;
    Integer totalItemCount;
    Integer successItemCount;
    Integer failedItemCount;
}
