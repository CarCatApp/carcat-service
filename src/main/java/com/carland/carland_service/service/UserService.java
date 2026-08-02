package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.NotificationResponse;
import com.carland.carland_service.dto.response.UserResponse;

import java.util.List;

/**
 * tr: Kullanıcı (müşteri) kayıt/detay işlemleri ve kullanıcı bildirim listesi için servis sözleşmesidir.
 * en: Service contract for user (customer) registration/detail operations and the user's notification list.
 */
public interface UserService {
    /**
     * tr: Kullanıcı detaylarını kaydeder/günceller (davet eden id'si dahil) ve kullanıcı bilgisini döner.
     * en: Saves/updates the user's details (including the inviter id) and returns the user info.
     */
    UserResponse userAddDetails(Long userId, String role, String phoneNumber,  String timezone, String acceptLanguage, Long inviterId);

    /**
     * tr: Kullanıcıya ait bildirim listesini döner.
     * en: Returns the notification list belonging to the user.
     */
    List<NotificationResponse> getNotificationList(String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

}
