package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.CustomerInformationRequest;
import com.carland.carland_service.dto.request.PromoCodeRequest;
import com.carland.carland_service.dto.response.CustomerInformationResponse;
import com.carland.carland_service.dto.response.NotificationLanguageResponse;
import com.carland.carland_service.dto.response.NotificationResponse;
import com.carland.carland_service.dto.response.PinOccupiedResponse;
import com.carland.carland_service.dto.response.UserResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.entity.Promo;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.NotMatchException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.messaging.CustomerEnsurePublisher;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.NotificationRepository;
import com.carland.carland_service.repository.PromoRepository;
import com.carland.carland_service.service.BookingMineService;
import com.carland.carland_service.service.CustomerEnsureService;
import com.carland.carland_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * tr: Kullanıcı işlemlerini yöneten servis; müşteri satırını header'daki id ve telefonla oluşturur/kontrol eder ve kullanıcının bildirim listesini döner.
 * en: Service managing user operations; creates/verifies the customer row from the header id and phone, and returns the user's notification list.
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
    private final CustomerEnsureService customerEnsureService;
    private final CustomerEnsurePublisher customerEnsurePublisher;
    private final PromoRepository promoRepository;

    /**
     * tr: Kullanıcı detaylarını ekler: role göre müşteriyi kontrol eder/oluşturur. Ad ve soyad burada yazılmaz. userId, role, phoneNumber veya timezone eksikse MissingFieldException fırlatır; başarıda message ve customerId döner.
     * en: Adds user details: checks/creates the customer by role. Name and surname are not written here. Throws MissingFieldException if userId, role, phoneNumber, or timezone is missing; returns message and customerId.
     */
    @Override
    public UserResponse userAddDetails(Long userId, String role, String phoneNumber,
                                       String timezone, String acceptLanguage, Long inviterId) {

        if (userId == null || role == null || phoneNumber == null || timezone == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_FIELDS.getMessageByLang(acceptLanguage));
        }

        boolean appUser = UserRoles.USER.name().equalsIgnoreCase(role.trim());
        try {
            helper.checkOrCreateUserByRole(userId, role, phoneNumber, null, null, acceptLanguage, inviterId);
        } catch (RuntimeException ex) {
            // Deploy/DB blip: keep trying ensure every 15s via Rabbit so the user is not stuck.
            if (appUser) {
                customerEnsurePublisher.publish(userId, phoneNumber, acceptLanguage);
            }
            throw ex;
        }

        if (appUser && !customerEnsureService.hasActiveCustomer(userId, phoneNumber)) {
            customerEnsurePublisher.publish(userId, phoneNumber, acceptLanguage);
        }

        return UserResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .customerId(userId)
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
     * tr: Aktif müşterinin profilini kaydeder. SIMA verified ise e-posta doluysa güncellenir (ad/soyad/FIN kilitli).
     *     Verified değilse ad (min 3) ve FIN zorunlu; soyad ve e-posta opsiyonel. FIN yalnızca başka SIMA-verified
     *     müşterideyse reddedilir. Telefon değişmez.
     * en: Saves the active customer's profile. When SIMA-verified, e-mail is updated only if provided.
     *     Otherwise name (min 3) and FIN are required; surname and e-mail are optional. FIN is rejected only
     *     when another SIMA-verified customer already has it. Phone is never updated.
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
            if (mail != null) {
                mail = mail.toLowerCase();
                if (!MAIL_PATTERN.matcher(mail).matches()) {
                    throw new NotMatchException(MessagesLangValues.INVALID_MAIL.getMessageByLang(acceptLanguage));
                }
                ensureMailAvailable(mail, customer, acceptLanguage);
                customer.setMail(mail);
                customerRepository.save(customer);
            }
            return toInformationResponse(customer);
        }

        String name = trimToNull(request.getName());
        String surname = trimToNull(request.getSurname());
        String pin = trimToNull(request.getPin());

        if (name == null || pin == null) {
            throw new MissingFieldException(MessagesLangValues.CUSTOMER_INFORMATION_MISSING.getMessageByLang(acceptLanguage));
        }
        if (name.length() < 3) {
            throw new NotMatchException(MessagesLangValues.NAME_TOO_SHORT.getMessageByLang(acceptLanguage));
        }

        pin = pin.toUpperCase();
        if (!PIN_PATTERN.matcher(pin).matches()) {
            throw new NotMatchException(MessagesLangValues.INVALID_PIN.getMessageByLang(acceptLanguage));
        }
        if (pinTakenByVerifiedOther(pin, customer.getUserId())) {
            throw new AlreadyExistsException(MessagesLangValues.PIN_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }

        if (mail != null) {
            mail = mail.toLowerCase();
            if (!MAIL_PATTERN.matcher(mail).matches()) {
                throw new NotMatchException(MessagesLangValues.INVALID_MAIL.getMessageByLang(acceptLanguage));
            }
            ensureMailAvailable(mail, customer, acceptLanguage);
        }

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
                .hasPromo(Boolean.TRUE.equals(customer.getHasPromo()))
                .promoCode(customer.getPromoCode())
                .build();
    }

    /**
     * tr: Kod promos.codeKey ile aranır. usableOn değilse alan değişmez.
     *     Aynı promo tekrar gelirse kayıt aynı kalır. Başka bir promo yazılmaz.
     *     Redis yok. customerId, X-User-Id ile aynı olmalıdır.
     * en: The code is looked up by promos.codeKey. A code that is not usableOn leaves the fields unchanged.
     *     The same promo again keeps the row. A different promo is not written.
     *     No Redis. customerId must equal X-User-Id.
     */
    @Override
    public CustomerInformationResponse applyPromo(PromoCodeRequest request, String role, String phoneNumber,
                                                  String userIdHeader, String acceptLanguage) {
        if (request == null || request.getCustomerId() == null
                || request.getPromoCode() == null || request.getPromoCode().isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = requireActiveCustomer(role, phoneNumber, userIdHeader, acceptLanguage);
        if (!request.getCustomerId().equals(customer.getUserId())) {
            throw new NotMatchException("customerId does not match");
        }
        String key = request.getPromoCode().trim().toLowerCase(Locale.ROOT);
        Promo promo = promoRepository.findByCodeKey(key).orElse(null);
        if (promo == null || !promo.usableOn(LocalDate.now(ZoneId.of("Asia/Baku")))) {
            throw new NotMatchException("promo code is not valid");
        }
        if (customer.getPromoId() != null && !customer.getPromoId().equals(promo.getId())) {
            throw new NotMatchException("promo code is already set");
        }
        if (customer.getPromoId() == null) {
            customer.setHasPromo(true);
            customer.setPromoId(promo.getId());
            customer.setPromoCode(promo.getCode());
            customerRepository.save(customer);
        }
        return toInformationResponse(customer);
    }

    /**
     * tr: FIN başka SIMA-verified müşterideyse occupied=true. Unverified duplicate veya kendi FIN → false.
     * en: occupied=true when another SIMA-verified customer has this FIN; unverified duplicate or own FIN → false.
     */
    @Override
    public PinOccupiedResponse isPinOccupied(String pin, String role, String phoneNumber, String userIdHeader,
                                             String acceptLanguage) {
        if (pin == null || pin.isBlank() || userIdHeader == null || userIdHeader.isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        boolean occupied = pinTakenByVerifiedOther(pin.trim(), userId);
        return PinOccupiedResponse.builder()
                .occupied(occupied)
                .build();
    }

    private boolean pinTakenByVerifiedOther(String pin, Long userId) {
        if (pin == null || pin.isBlank() || userId == null) {
            return false;
        }
        return customerRepository.findAllByPinIgnoreCase(pin.trim()).stream()
                .anyMatch(owner -> owner.getUserId() != null
                        && !owner.getUserId().equals(userId)
                        && Boolean.TRUE.equals(owner.getSimaVerified()));
    }

    /**
     * tr: Aktiv müştərinin notificationLanguage sahəsini az, en və ya ru edir. Boş və ya başqa kod rədd edilir.
     *     Booking push göndəriləndə bu sahəni oxuyur; başqa profil sahəsi dəyişmir.
     * en: Sets the active customer's notificationLanguage to az, en, or ru. A blank or other code is rejected.
     *     Booking push reads this field when it sends; no other profile field changes.
     */
    @Override
    public NotificationLanguageResponse setNotificationLanguage(String lang, String role, String phoneNumber,
                                                                String userIdHeader, String acceptLanguage) {
        String stored = normalizeLanguage(lang, acceptLanguage);
        Customer customer = requireActiveCustomer(role, phoneNumber, userIdHeader, acceptLanguage);
        customer.setNotificationLanguage(stored);
        customerRepository.save(customer);
        return NotificationLanguageResponse.builder().language(stored).build();
    }

    private static String normalizeLanguage(String lang, String acceptLanguage) {
        if (lang == null || lang.isBlank()) {
            throw MissingFieldException.required("lang");
        }
        String code = BookingMineService.langOf(lang);
        if (!"az".equals(code) && !"en".equals(code) && !"ru".equals(code)) {
            throw new NotMatchException(MessagesLangValues.INVALID_LANGUAGE.getMessageByLang(acceptLanguage));
        }
        return code;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
