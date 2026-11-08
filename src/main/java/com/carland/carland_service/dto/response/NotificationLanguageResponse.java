package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Müştərinin yadda saxlanmış bildiriş dili. Booking push bu dəyəri oxuyur.
 * en: The customer's stored notification language. Booking push reads this value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLanguageResponse {
    String language;
}
