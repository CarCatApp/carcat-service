package com.carland.carland_service.service.impl;

import com.carland.carland_service.service.PushNotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * tr: Firebase Cloud Messaging (FCM) üzerinden mobil cihazlara push bildirim gönderen servis.
 * en: Service sending push notifications to mobile devices via Firebase Cloud Messaging (FCM).
 */
@Service
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    /**
     * tr: Verilen başlık ve içerikle, belirtilen cihaz token'ına FCM push bildirimi gönderir. Gönderim FirebaseMessagingException ile başarısız olursa hata loglanır ve RuntimeException fırlatılır.
     * en: Sends an FCM push notification with the given title and body to the specified device token. If sending fails with a FirebaseMessagingException, the error is logged and a RuntimeException is thrown.
     */
    @Override
    public void send(String title, String body, String deviceToken) {

        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push göndərildi. token={}, response={}", deviceToken, response);
        } catch (FirebaseMessagingException e) {
            log.error("Firebase push göndərilmədi. token={}", deviceToken, e);
            throw new RuntimeException("Push notification göndərilmədi");
        }
    }
}
