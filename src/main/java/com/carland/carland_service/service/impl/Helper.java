package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.Admin;
import com.carland.carland_service.entity.AutoService;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.SuperAdmin;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.AdminRepository;
import com.carland.carland_service.repository.AutoServiceRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * tr: İki sorumluluğu olan yardımcı servistir: (1) role göre kullanıcı kaydı oluşturma/kontrol etme
 *     (ADMIN / SUPER_ADMIN / USER), (2) takvim ve randevu akışlarında kullanılan saat dilimi ve
 *     tarih formatlama yardımcıları.
 * en: Helper service with two responsibilities: (1) checking/creating user records by role
 *     (ADMIN / SUPER_ADMIN / USER), (2) timezone conversion and date formatting utilities used by
 *     calendar and appointment flows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Helper {
    private final AdminRepository adminRepository;
    private final CustomerRepository customerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final AutoServiceRepository autoServiceRepository;

    /**
     * tr: Verilen role göre kullanıcıyı kontrol eder, yoksa oluşturur: ADMIN için davet eden SuperAdmin ve
     *     AutoService zorunludur (yoksa UserNotFound/ResourceNotFound fırlatır), SUPER_ADMIN ve USER doğrudan
     *     oluşturulur; tanınmayan rolde ResourceNotFoundException (INVALID_ROLE) fırlatır.
     * en: Checks the user for the given role and creates it when absent: ADMIN requires the inviting
     *     SuperAdmin and AutoService (throws UserNotFound/ResourceNotFound otherwise), SUPER_ADMIN and USER
     *     are created directly; throws ResourceNotFoundException (INVALID_ROLE) for unknown roles.
     */
    public void checkOrCreateUserByRole(Long userId, String role, String phoneNumber, String name,
                                        String surname, String acceptLanguage, Long inviterId) {
        switch (role) {
            case "ADMIN" -> {
                Admin byId = adminRepository.findByUserId(userId);
                Admin byPhone = adminRepository.findByPhoneNumber(phoneNumber);

                if ((byId != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byId.getStatus())) ||
                        (byPhone != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byPhone.getStatus()))) {
                    log.info("Aktiv admin var {}", phoneNumber);
                    return;
                }

                if (byId != null || byPhone != null) {
                    log.info("Admin var aktiv deyil {}", phoneNumber);
                    return;
                }
                SuperAdmin superAdmin = superAdminRepository.findByUserId(inviterId);
                if (superAdmin == null) {
                    throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
                }
                AutoService autoService = autoServiceRepository.findBySuperAdmin(superAdmin);
                if (autoService == null) {
                    throw new ResourceNotFoundException(MessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
                }
                Admin newAdmin = Admin.builder()
                        .userId(userId)
                        .phoneNumber(phoneNumber)
                        .name(name)
                        .surname(surname)
                        .status(UserStatus.ACTIVE.name())
                        .notificationLanguage(acceptLanguage)
                        .autoService(autoService)
                        .build();
                adminRepository.save(newAdmin);
                log.info("Admin tapilmadi, yeni yaradildi: {}", phoneNumber);
            }

            case "SUPER_ADMIN" -> {
                System.err.println("super admine girdi");

                SuperAdmin byId = superAdminRepository.findByUserId(userId);
                SuperAdmin byPhone = superAdminRepository.findByPhoneNumber(phoneNumber);

                if ((byId != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byId.getStatus())) ||
                        (byPhone != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byPhone.getStatus()))) {
                    log.info("Aktiv Super admin var {}", phoneNumber);
                    return;
                }

                if (byId != null || byPhone != null) {
                    log.info("Super Admin var aktiv deyil {}", phoneNumber);
                    return;
                }

                SuperAdmin newAdmin = SuperAdmin.builder()
                        .userId(userId)
                        .phoneNumber(phoneNumber)
                        .name(name)
                        .surname(surname)
                        .status(UserStatus.ACTIVE.name())
                        .notificationLanguage(acceptLanguage)
                        .build();
                superAdminRepository.save(newAdmin);
                log.info("Super Admin tapilmadi, yeni yaradildi: {}", phoneNumber);
            }


            case "USER" -> {
                System.err.println("customere girdi");

                Customer byId = customerRepository.findByUserId(userId);
                Customer byPhone = customerRepository.findByPhoneNumber(phoneNumber);

                if ((byId != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byId.getStatus())) ||
                        (byPhone != null && UserStatus.ACTIVE.name().equalsIgnoreCase(byPhone.getStatus()))) {
                    log.info("Aktiv musteri tapildi, : {}", phoneNumber);
                    return;
                }

                if (byId != null || byPhone != null) {
                    log.info("musteri tapildi ama aktiv deyil,: {}", phoneNumber);
                    return;
                }

                Customer newCustomer = Customer.builder()
                        .userId(userId)
                        .phoneNumber(phoneNumber)
                        .name(name)
                        .surname(surname)
                        .createdAt(LocalDate.now())
                        .status(UserStatus.ACTIVE.name())
                        .notificationLanguage(acceptLanguage)
                        .build();
                customerRepository.save(newCustomer);
                log.info("musteri tapilmadi, yeni yaradildi: {}", phoneNumber);
            }

            default ->
                    throw new ResourceNotFoundException(MessagesLangValues.INVALID_ROLE.getMessageByLang(acceptLanguage));
        }
    }


    /**
     * tr: Verilen tarih, saat ve saat dilimini UTC'ye çevrilmiş OffsetDateTime olarak döner.
     * en: Converts the given date, time and timezone into an OffsetDateTime normalized to UTC.
     */
    public OffsetDateTime getUtcTimeFromDayAndTimeAndTimeZone(LocalDate date, LocalTime time, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDateTime localDateTime = LocalDateTime.of(date, time);
        return localDateTime.atZone(zoneId).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    /**
     * tr: UTC zamanından gün (LocalDate) kısmını döner.
     * en: Returns the day (LocalDate) part of a UTC time.
     */
    public LocalDate getUtcDayFromUtcTime(OffsetDateTime utcTime) {
        return utcTime.toLocalDate();
    }


    /**
     * tr: UTC zamanını verilen saat dilimindeki yerel saate (LocalTime) çevirir.
     * en: Converts a UTC time to the local time (LocalTime) in the given timezone.
     */
    public LocalTime getLocalTimeFromUtcUseTZ(OffsetDateTime utcDateTime, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        return utcDateTime.atZoneSameInstant(zoneId).toLocalTime();
    }

    /**
     * tr: Randevu saatini dile uygun "dd MMM yyyy HH:mm" formatında string'e çevirir; null girişte null döner.
     * en: Formats an appointment time as "dd MMM yyyy HH:mm" per language; returns null on null input.
     */
    public String formatAppointmentDate(LocalTime dateTime, String acceptLanguage) {
        if (dateTime == null) return null;

        Locale locale = acceptLanguage != null && !acceptLanguage.isEmpty()
                ? Locale.forLanguageTag(acceptLanguage)
                : Locale.getDefault();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale);

        return dateTime.format(formatter);
    }

    /**
     * tr: UTC zamanını verilen saat dilimindeki OffsetDateTime'a çevirir; timezone null ise sistem varsayılanını kullanır.
     * en: Converts a UTC time to an OffsetDateTime in the given timezone; uses the system default when timezone is null.
     */
    public OffsetDateTime getLocalDateTimeFromUtcUseTZ(OffsetDateTime utcDateTime, String timezone) {
        if (utcDateTime == null) return null;
        ZoneId zone = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        return utcDateTime.atZoneSameInstant(zone).toOffsetDateTime();
    }

    /**
     * tr: Randevu tarihini (OffsetDateTime) dile uygun "dd MMM yyyy HH:mm" formatında string'e çevirir.
     * en: Formats an appointment date (OffsetDateTime) as "dd MMM yyyy HH:mm" per language.
     */
    public String formatAppointmentDate(OffsetDateTime dateTime, String acceptLanguage) {
        if (dateTime == null) return null;

        Locale locale = acceptLanguage != null && !acceptLanguage.isEmpty()
                ? Locale.forLanguageTag(acceptLanguage)
                : Locale.getDefault();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale);
        return dateTime.format(formatter);
    }
}
