package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: DeviceTokenController üzerinden kullanıcının push bildirim cihaz token'ını kaydetmek için kullanılan istek DTO'su.
 * en: Request DTO used via DeviceTokenController to register a user's push notification device token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenRequest {
    private Long userId;
    private String deviceToken;
    private String platform;
}
