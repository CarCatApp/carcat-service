package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.CustomerInformationRequest;
import com.carland.carland_service.dto.response.CustomerInformationResponse;
import com.carland.carland_service.dto.response.NameSurname;
import com.carland.carland_service.dto.response.NotificationResponse;
import com.carland.carland_service.dto.response.PinOccupiedResponse;
import com.carland.carland_service.dto.response.UserResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.NotMatchException;
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
import java.util.regex.Pattern;

/**
 * tr: Kullanıcı işlemlerini yöneten servis; yeni kullanıcı detaylarını dış servisten (Feign) alınan ad-soyad ile oluşturur/kontrol eder ve kullanıcının bildirim listesini döner.
 * en: Service managing user operations; creates/verifies user details using the name and surname fetched from an external service (Feign), and returns the user's notification list.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final Pattern MAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** Azerbaijan FIN: 7 alphanumeric characters (SIMA pin). */
    private static final Pattern PIN_PATTERN = Pattern.compile("^[A-Za-z0-9]{7}$");

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

    /**
     * tr: Aktif müşterinin "Məlumatlarım" profilini döner. USER rolü şarttır; müşteri yoksa UserNotFoundException.
     * en: Returns the active customer's "My information" profile. Requires USER role; throws UserNotFoundException if missing.
     */
    @Override
    public CustomerInformationResponse getCustomerInformation(String role, String phoneNumber, String userIdHeader,
                                                              String acceptLanguage) {
        Customer customer = requireActiveCustomer(role, phoneNumber, userIdHeader, acceptLanguage);
        return toInformationResponse(customer);
    }

    /**
     * tr: Aktif müşterinin profilini kaydeder. SIMA verified ise yalnızca e-posta değişir (ad/soyad/FIN yok sayılır).
     *     Verified değilse ad, soyad, e-posta ve FIN güncellenir. Telefon değişmez.
     * en: Saves the active customer's profile. When SIMA-verified only e-mail is updated (name/surname/FIN ignored).
     *     Otherwise name, surname, e-mail and FIN are updated. Phone is never updated.
     */
    @Override
    public CustomerInformationResponse saveCustomerInformation(CustomerInformationRequest request, String role,
                                                               String phoneNumber, String userIdHeader,
                                                               String acceptLanguage) {
        Customer customer = requireActiveCustomer(role, phoneNumber, userIdHeader, acceptLanguage);

        if (request == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        boolean verified = Boolean.TRUE.equals(customer.getSimaVerified());
        String mail = trimToNull(request.getMail());

        if (verified) {
            if (mail == null) {
                throw new MissingFieldException(MessagesLangValues.CUSTOMER_INFORMATION_MISSING.getMessageByLang(acceptLanguage));
            }
            mail = mail.toLowerCase();
            if (!MAIL_PATTERN.matcher(mail).matches()) {
                throw new NotMatchException(MessagesLangValues.INVALID_MAIL.getMessageByLang(acceptLanguage));
            }
            ensureMailAvailable(mail, customer, acceptLanguage);
            customer.setMail(mail);
            customerRepository.save(customer);
            return toInformationResponse(customer);
        }

        String name = trimToNull(request.getName());
        String surname = trimToNull(request.getSurname());
        String pin = trimToNull(request.getPin());

        if (name == null || surname == null || mail == null || pin == null) {
            throw new MissingFieldException(MessagesLangValues.CUSTOMER_INFORMATION_MISSING.getMessageByLang(acceptLanguage));
        }

        mail = mail.toLowerCase();
        pin = pin.toUpperCase();

        if (!MAIL_PATTERN.matcher(mail).matches()) {
            throw new NotMatchException(MessagesLangValues.INVALID_MAIL.getMessageByLang(acceptLanguage));
        }
        if (!PIN_PATTERN.matcher(pin).matches()) {
            throw new NotMatchException(MessagesLangValues.INVALID_PIN.getMessageByLang(acceptLanguage));
        }

        Customer pinOwner = customerRepository.findByPinIgnoreCase(pin);
        if (pinOwner != null && !pinOwner.getUserId().equals(customer.getUserId())) {
            throw new AlreadyExistsException(MessagesLangValues.PIN_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }

        ensureMailAvailable(mail, customer, acceptLanguage);

        customer.setName(name);
        customer.setSurname(surname);
        customer.setPin(pin);
        customer.setMail(mail);
        customerRepository.save(customer);

        return toInformationResponse(customer);
    }

    private void ensureMailAvailable(String mail, Customer customer, String acceptLanguage) {
        Customer mailOwner = customerRepository.findByMailIgnoreCase(mail);
        if (mailOwner != null && !mailOwner.getUserId().equals(customer.getUserId())) {
            throw new AlreadyExistsException(MessagesLangValues.MAIL_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }
    }

    private Customer requireActiveCustomer(String role, String phoneNumber, String userIdHeader, String acceptLanguage) {
        if (userIdHeader == null || userIdHeader.isBlank() || phoneNumber == null || phoneNumber.isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_FIELDS.getMessageByLang(acceptLanguage));
        }
        if (role == null || !"USER".equalsIgnoreCase(role)) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return customer;
    }

    private static CustomerInformationResponse toInformationResponse(Customer customer) {
        return CustomerInformationResponse.builder()
                .name(customer.getName())
                .surname(customer.getSurname())
                .mail(customer.getMail())
                .pin(customer.getPin())
                .phoneNumber(customer.getPhoneNumber())
                .verified(Boolean.TRUE.equals(customer.getSimaVerified()))
                .build();
    }

    /**
     * tr: FIN başka müşterideyse occupied=true. Boşsa veya çağıranın kendi FIN'iyse false.
     * en: occupied=true when FIN belongs to another customer; false when unused or owned by the caller.
     */
    @Override
    public PinOccupiedResponse isPinOccupied(String pin, String role, String phoneNumber, String userIdHeader,
                                             String acceptLanguage) {
        if (pin == null || pin.isBlank() || userIdHeader == null || userIdHeader.isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer owner = customerRepository.findByPinIgnoreCase(pin.trim());
        boolean occupied = owner != null && owner.getUserId() != null
                && !String.valueOf(owner.getUserId()).equals(userIdHeader.trim());
        return PinOccupiedResponse.builder()
                .occupied(occupied)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
