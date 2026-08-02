package com.carland.carland_service.service;

import com.carland.carland_service.entity.Notification;

import java.util.List;

/**
 * tr: Kullanıcı bildirimlerinin okundu işaretleme, silme ve listeleme işlemlerinin sözleşmesidir.
 * en: Contract for marking user notifications as read, deleting them, and listing them.
 */
public interface NotificationService {
    /**
     * tr: Bildirimin okundu/okunmadı durumunu günceller ve bildirimi döner.
     * en: Updates the read/unread state of a notification and returns it.
     */
    Notification editNotification(Long notificationId, boolean setRead, String acceptLanguage);

    /**
     * tr: Bildirimi siler ve silinen bildirimi döner.
     * en: Deletes a notification and returns the deleted notification.
     */
    Notification deleteNotification(Long notificationId, String acceptLanguage);


    /**
     * tr: Müşteriye ait bildirim listesini döner.
     * en: Returns the notification list of the customer.
     */
    List<Notification> getNotificationListByCustomerId(String userIdHeader, String acceptLanguage);
}
