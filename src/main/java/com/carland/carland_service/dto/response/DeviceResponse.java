package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: DeviceTokenController cihaz token kayıt işleminden sonra dönen basit mesaj yanıtı DTO'su.
 * en: Simple message response DTO returned after DeviceTokenController device token registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    String message;
}
