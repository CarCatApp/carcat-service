package com.carland.carland_service.service;

/**
 * tr: Tekil cihazlara push bildirimi gönderimi için servis sözleşmesidir.
 * en: Service contract for sending push notifications to individual devices.
 */
public interface PushNotificationService {
    /**
     * tr: Verilen cihaz token'ına başlık ve içerik ile push bildirimi gönderir.
     * en: Sends a push notification with the given title and body to the specified device token.
     */
    void send(String title, String body, String deviceToken);

}
