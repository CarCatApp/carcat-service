package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.NameSurname;
import com.carland.carland_service.dto.response.NotificationResponse;
import com.carland.carland_service.dto.response.UserResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.feign.NameSurnameFeign;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.NotificationRepository;
import com.carland.carland_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * tr: Kullanıcı işlemlerini yöneten servis; yeni kullanıcı detaylarını dış servisten (Feign) alınan ad-soyad ile oluşturur/kontrol eder ve kullanıcının bildirim listesini döner.
 * en: Service managing user operations; creates/verifies user details using the name and surname fetched from an external service (Feign), and returns the user's notification list.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final Helper helper;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final NameSurnameFeign nameSurnameFeign;

    /**
     * tr: Kullanıcı detaylarını ekler: ad-soyadı Feign ile dış servisten alır ve role göre kullanıcıyı kontrol eder/oluşturur (davet eden inviterId dahil). userId, role, phoneNumber veya timezone eksikse MissingFieldException fırlatır; başarıda mesajlı UserResponse döner.
     * en: Adds user details: fetches name and surname from an external service via Feign and checks/creates the user by role (including the inviterId). Throws MissingFieldException if userId, role, phoneNumber, or timezone is missing; returns a UserResponse with a success message.
     */
    @Override
    public UserResponse userAddDetails(Long userId, String role, String phoneNumber,
                                       String timezone, String acceptLanguage, Long inviterId) {

        if (userId == null || role == null || phoneNumber == null || timezone == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_FIELDS.getMessageByLang(acceptLanguage));
        }


        NameSurname nameSurname = nameSurnameFeign.getNameSurname(userId);
        log.info("name surname feign response : {}", nameSurname);
        helper.checkOrCreateUserByRole(userId, role, phoneNumber, nameSurname.getName(), nameSurname.getSurname(), acceptLanguage, inviterId);

        return UserResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    /**
     * tr: Aktif müşterinin tüm bildirimlerini NotificationResponse listesi olarak döner. Müşteri bulunamazsa UserNotFoundException, hiç bildirim yoksa ResourceNotFoundException fırlatır.
     * en: Returns all notifications of the active customer as a list of NotificationResponse. Throws UserNotFoundException if the customer is not found and ResourceNotFoundException if there are no notifications.
     */
    @Override
    public List<NotificationResponse> getNotificationList(String role, String phoneNumber, String userIdHeader,
                                                          String timezone, String acceptLanguage) {

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader),
                phoneNumber, UserStatus.ACTIVE.name());

        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        List<Notification> notifications = notificationRepository.findAllByCustomerId(customer.getUserId());

        if (notifications.isEmpty()) {
            throw new ResourceNotFoundException(MessagesLangValues.NOTIFICATION_NOT_FOUND.
                    getMessageByLang(acceptLanguage));
        }


        return notifications.stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .notificationText(n.getNotificationText())
                        .build())
                .toList();
    }
}
