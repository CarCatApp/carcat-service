package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.CustomerInformationRequest;
import com.carland.carland_service.dto.response.CustomerInformationResponse;
import com.carland.carland_service.dto.response.NotificationResponse;
import com.carland.carland_service.dto.response.PinOccupiedResponse;
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

    /**
     * tr: Müşterinin "Məlumatlarım" profilini (ad, soyad, e-posta, FIN) döner; telefon salt-okunur.
     * en: Returns the customer's "My information" profile (name, surname, e-mail, FIN); phone is read-only.
     */
    CustomerInformationResponse getCustomerInformation(String role, String phoneNumber, String userIdHeader, String acceptLanguage);

    /**
     * tr: Profil kaydı. SIMA verified ise e-posta doluysa; değilse ad (min 3) + FIN zorunlu, soyad/e-posta opsiyonel.
     * en: Saves profile. When SIMA-verified, e-mail if provided; otherwise name (min 3) and FIN required, surname/e-mail optional.
     */
    CustomerInformationResponse saveCustomerInformation(CustomerInformationRequest request, String role,
                                                        String phoneNumber, String userIdHeader, String acceptLanguage);

    /**
     * tr: FIN başka SIMA-verified müşterideyse true; unverified duplicate veya kendi FIN ise false.
     * en: True when another SIMA-verified customer has this FIN; unverified duplicate or own FIN → false.
     */
    PinOccupiedResponse isPinOccupied(String pin, String role, String phoneNumber, String userIdHeader,
                                      String acceptLanguage);

}
