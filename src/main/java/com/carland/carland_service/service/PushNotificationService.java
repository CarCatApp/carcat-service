package com.carland.carland_service.service;

import java.util.List;
import java.util.Map;

/**
 * tr: Tekil cihazlara ve FCM multicast (en fazla 500) ile push gönderimi için servis sözleşmesidir.
 * en: Service contract for sending push notifications to individual devices and FCM multicast (max 500).
 */
public interface PushNotificationService {
    /**
     * tr: Verilen cihaz token'ına başlık ve içerik ile push bildirimi gönderir.
     * en: Sends a push notification with the given title and body to the specified device token.
     */
    void send(String title, String body, String deviceToken);

    /**
     * tr: Başlık ve gövdeyle birlikte string data gönderir. Növbə push-u bookingId və branchId taşır.
     * en: Sends the title and body together with string data. A booking push carries bookingId and branchId.
     */
    void send(String title, String body, String deviceToken, Map<String, String> data);

    /**
     * tr: FCM sendEachForMulticast; en fazla 500 token. Dönen liste token sırasıyla başarı/başarısız.
     * en: FCM sendEachForMulticast; max 500 tokens. Returned list is success/fail aligned to token order.
     */
    List<Boolean> sendEachForMulticast(String title, String body, List<String> deviceTokens);
}
