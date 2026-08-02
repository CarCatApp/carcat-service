package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * tr: Birden fazla müşteriye toplu push bildirimi göndermek için kullanılan istek DTO'su (NotificationController toplu gönderim akışı).
 * en: Request DTO for sending a bulk push notification to multiple customers (NotificationController bulk send flow).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkPushRequest {
    List<Long> customerIdList;
    String title;
    String body;
}
