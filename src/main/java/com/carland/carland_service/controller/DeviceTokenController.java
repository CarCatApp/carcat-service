package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.BulkPushRequest;
import com.carland.carland_service.dto.request.DeviceTokenRequest;
import com.carland.carland_service.dto.response.BulkPushResponse;
import com.carland.carland_service.dto.response.DeviceResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * tr: Cihaz push token REST controller'ı; FCM cihaz token'ı kaydetme/güncelleme ve toplu push bildirimi gönderme uçlarını sunar.
 * en: REST controller for device push tokens; exposes endpoints to save/update FCM device tokens and to send bulk push notifications.
 */
@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    /**
     * tr: Gövdedeki DeviceTokenRequest ile cihazın push token'ını kaydeder ya da mevcutsa günceller ve cihaz bilgisini döner.
     * en: Saves the device's push token from the DeviceTokenRequest body, or updates it if it already exists, and returns the device info.
     */
    @PostMapping("/post")
    public DeviceResponse saveOrUpdateToken(@RequestBody DeviceTokenRequest request) {
        return deviceTokenService.saveOrUpdateToken(request);

    }

    /**
     * tr: Gövdedeki BulkPushRequest ile birden fazla cihaza toplu push bildirimi gönderir ve gönderim sonucunu döner.
     * en: Sends a bulk push notification to multiple devices using the BulkPushRequest body and returns the delivery result.
     */
    @PostMapping("/send/bulk")
    public BulkPushResponse sendBulk(@RequestBody BulkPushRequest bulkRequest,
                                 @RequestHeader("Accept-Language") String acceptLanguage){
        return deviceTokenService.sendBulk(bulkRequest, acceptLanguage);
    }
}
