package com.carland.carland_service.service;


import com.carland.carland_service.dto.request.BulkPushRequest;
import com.carland.carland_service.dto.request.DeviceTokenRequest;
import com.carland.carland_service.dto.response.BulkPushResponse;
import com.carland.carland_service.dto.response.DeviceResponse;

/**
 * tr: Cihaz push token'larının kaydı/güncellenmesi ve toplu push bildirimi gönderimi için servis sözleşmesidir.
 * en: Service contract for saving/updating device push tokens and sending bulk push notifications.
 */
public interface DeviceTokenService {
    /**
     * tr: Cihaz token'ını kaydeder veya mevcutsa günceller ve cihaz bilgisini döner.
     * en: Saves the device token, or updates it if it already exists, and returns the device info.
     */
    DeviceResponse saveOrUpdateToken(DeviceTokenRequest request);

    /**
     * tr: Kayıtlı cihazlara toplu push bildirimi gönderir ve gönderim sonucunu döner.
     * en: Sends a bulk push notification to registered devices and returns the send result.
     */
    BulkPushResponse sendBulk(BulkPushRequest bulkRequest, String acceptLanguage);
}
