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
     * tr: Profil kaydı. SIMA verified ise yalnızca e-posta; değilse ad, soyad, e-posta ve FIN. Telefon değişmez.
     * en: Saves profile. When SIMA-verified only e-mail; otherwise name, surname, e-mail and FIN. Phone unchanged.
     */
    CustomerInformationResponse saveCustomerInformation(CustomerInformationRequest request, String role,
                                                        String phoneNumber, String userIdHeader, String acceptLanguage);

    /**
     * tr: FIN başka bir müşteride kayıtlıysa true, yoksa veya çağıranın kendisine aitse false döner.
     * en: Returns true when the FIN belongs to another customer; false when unused or owned by the caller.
     */
    PinOccupiedResponse isPinOccupied(String pin, String role, String phoneNumber, String userIdHeader,
                                      String acceptLanguage);

}
