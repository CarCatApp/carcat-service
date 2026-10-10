package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.NotificationRepository;
import com.carland.carland_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * tr: Kullanıcı bildirimlerini yöneten servis; bildirimi okundu olarak işaretleme, soft delete (durumu deActive yapma) ve müşteriye ait aktif bildirimleri listeleme işlemlerini yapar.
 * en: Service managing user notifications; marks a notification as read, soft-deletes it (sets status to deActive), and lists a customer's active notifications.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl  implements NotificationService {

private final NotificationRepository notificationRepository;
    /**
     * tr: Verilen id'li bildirimi okundu (read=true) olarak işaretleyip kaydeder ve güncellenmiş bildirimi döner. Bildirim bulunamazsa ResourceNotFoundException fırlatır.
     * en: Marks the notification with the given id as read (read=true), saves it, and returns the updated notification. Throws ResourceNotFoundException if the notification is not found.
     */
    @Override
    public Notification editNotification(Long notificationId, boolean setRead, String acceptLanguage) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(()-> new ResourceNotFoundException(MessagesLangValues.NOTIFICATION_NOT_FOUND.getMessageByLang(acceptLanguage)));
       notification.setRead(true);
       notificationRepository.save(notification);
        return notification;
    }

    /**
     * tr: Verilen id'li bildirimi soft delete yapar: durumunu "deActive" olarak günceller ve kaydeder. Bildirim bulunamazsa ResourceNotFoundException fırlatır.
     * en: Soft-deletes the notification with the given id by setting its status to "deActive" and saving it. Throws ResourceNotFoundException if the notification is not found.
     */
    @Override
    public Notification deleteNotification(Long notificationId, String acceptLanguage) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(()-> new ResourceNotFoundException(MessagesLangValues.NOTIFICATION_NOT_FOUND.getMessageByLang(acceptLanguage)));
        notification.setStatus("deActive");
        notificationRepository.save(notification);
        return notification;
    }

    /**
     * tr: Header'dan gelen müşteri id'sine ait "ACTIVE" durumdaki bildirimleri listeler. Hiç bildirim yoksa ResourceNotFoundException fırlatır.
     * en: Lists notifications with "ACTIVE" status for the customer id taken from the request header. Throws ResourceNotFoundException if no notifications exist.
     */
    @Override
    public List<Notification> getNotificationListByCustomerId(String userIdHeader, String acceptLanguage) {
        List<Notification> notifications= notificationRepository.findAllByCustomerIdAndStatus(Long.valueOf(userIdHeader), "ACTIVE");
        // #region agent log
        int bookingCount = 0;
        StringBuilder ids = new StringBuilder("[");
        for (Notification row : notifications) {
            if (row.getType() != null && row.getType().startsWith("BOOKING")) {
                bookingCount++;
            }
            if (ids.length() < 300) {
                if (ids.length() > 1) {
                    ids.append(',');
                }
                ids.append(row.getId());
            }
        }
        ids.append(']');
        try {
            String line = "{\"sessionId\":\"f25d4f\",\"hypothesisId\":\"D\",\"location\":\"NotificationServiceImpl.getNotificationListByCustomerId\",\"message\":\"list\",\"data\":{\"userId\":"
                    + userIdHeader + ",\"count\":" + notifications.size() + ",\"bookingCount\":" + bookingCount
                    + ",\"ids\":" + ids + "},\"timestamp\":" + System.currentTimeMillis() + "}\n";
            java.nio.file.Files.write(
                    java.nio.file.Path.of("c:/Users/Aziz/IdeaProjects/debug-f25d4f.log"),
                    line.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // debug log must not affect the list
        }
        // #endregion
        if (notifications.isEmpty()){
            throw new ResourceNotFoundException(MessagesLangValues.NOTIFICATION_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return notifications;
    }
}
