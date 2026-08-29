package com.carland.carland_service.service.impl;

import com.carland.carland_service.service.PushNotificationService;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Boolean> sendEachForMulticast(String title, String body, List<String> deviceTokens) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return List.of();
        }
        if (deviceTokens.size() > 500) {
            throw new IllegalArgumentException("FCM multicast accepts at most 500 tokens per call");
        }
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                .build();
        try {
            BatchResponse batch = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            List<Boolean> results = new ArrayList<>(deviceTokens.size());
            List<SendResponse> responses = batch.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse item = responses.get(i);
                boolean ok = item.isSuccessful();
                results.add(ok);
                if (!ok) {
                    String reason = item.getException() != null ? item.getException().getMessage() : "unknown";
                    log.warn("Firebase multicast fail index={} tokenSuffix={} reason={}",
                            i, suffix(deviceTokens.get(i)), reason);
                }
            }
            return results;
        } catch (FirebaseMessagingException e) {
            log.error("Firebase multicast request failed size={}", deviceTokens.size(), e);
            throw new RuntimeException("Push notification göndərilmədi");
        }
    }

    private static String suffix(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(token.length() - 8);
    }
}
