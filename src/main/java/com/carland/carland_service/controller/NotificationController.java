package com.carland.carland_service.controller;

import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * tr: Bildirim REST controller'ı; bildirimi okundu/okunmadı olarak işaretleme, bildirim silme ve müşteriye ait bildirim listesini getirme uçlarını sunar.
 * en: REST controller for notifications; exposes endpoints to mark a notification as read/unread, delete a notification, and fetch the notification list for a customer.
 */
@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * tr: Verilen notificationId'ye ait bildirimin okundu (setRead) durumunu günceller ve güncellenmiş bildirimi döner.
     * en: Updates the read status (setRead) of the notification identified by notificationId and returns the updated notification.
     */
    @PostMapping("/edit/byId")
    public Notification editNotification(@RequestParam Long notificationId,
                                         @RequestParam boolean setRead,
                                         @RequestHeader("Accept-Language") String acceptLanguage) {
        return notificationService.editNotification(notificationId, setRead, acceptLanguage);
    }

    /**
     * tr: Verilen notificationId'ye ait bildirimi siler ve silinen bildirimi döner.
     * en: Deletes the notification identified by notificationId and returns the deleted notification.
     */
    @PostMapping("/delete/byId")
    public Notification deleteNotification(@RequestParam Long notificationId,
                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        return notificationService.deleteNotification(notificationId, acceptLanguage);
    }


    /**
     * tr: X-User-Id header'ından belirlenen müşteriye ait bildirim listesini döner.
     * en: Returns the notification list for the customer resolved from the X-User-Id header.
     */
    @GetMapping("/get/list/by/customer")
    public List<Notification> getNotificationListByCustomerId(@RequestHeader("X-User-Id") String userIdHeader,
                                                              @RequestHeader("Accept-Language") String acceptLanguage) {
        return notificationService.getNotificationListByCustomerId(userIdHeader, acceptLanguage);
    }

}
