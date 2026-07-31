package com.carland.carland_service.util;

import com.carland.carland_service.entity.Admin;
import com.carland.carland_service.entity.AutoService;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.SuperAdmin;
import com.carland.carland_service.enums.EnumMessagesLangValues;
import com.carland.carland_service.enums.EnumUserStatus;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class Helper {
    private final AdminRepository adminRepository;
    private final CustomerRepository customerRepository;
    private final SuperAdminRepository superAdminRepository;
    private final AutoServiceRepository autoServiceRepository;

    public void checkOrCreateUserByRole(Long userId, String role, String phoneNumber, String name,
                                        String surname, String acceptLanguage, Long inviterId) {
        switch (role) {
            case "ADMIN" -> {
                Admin byId = adminRepository.findByUserId(userId);
                Admin byPhone = adminRepository.findByPhoneNumber(phoneNumber);

                if ((byId != null && EnumUserStatus.ACTIVE.name().equalsIgnoreCase(byId.getStatus())) ||
                        (byPhone != null && EnumUserStatus.ACTIVE.name().equalsIgnoreCase(byPhone.getStatus()))) {
                    log.info("Aktiv admin var {}", phoneNumber);
                    return;
                }

                if (byId != null || byPhone != null) {
                    log.info("Admin var aktiv deyil {}", phoneNumber);
                    return;
                }
                SuperAdmin superAdmin = superAdminRepository.findByUserId(inviterId);
                if (superAdmin == null) {
                    throw new UserNotFoundException(EnumMessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
                }
                AutoService autoService = autoServiceRepository.findBySuperAdmin(superAdmin);
                if (autoService == null) {
                    throw new ResourceNotFoundException(EnumMessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
                }
                Admin newAdmin = Admin.builder()
                        .userId(userId)
                        .phoneNumber(phoneNumber)
                        .name(name)
                        .surname(surname)
                        .status(EnumUserStatus.ACTIVE.name())
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

                if ((byId != null && EnumUserStatus.ACTIVE.name().equalsIgnoreCase(byId.getStatus())) ||
                        (byPhone != null && EnumUserStatus.ACTIVE.name().equalsIgnoreCase(byPhone.getStatus()))) {
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
                        .status(EnumUserStatus.ACTIVE.name())
                        .notificationLanguage(acceptLanguage)
                        .build();
                superAdminRepository.save(newAdmin);
                log.info("Super Admin tapilmadi, yeni yaradildi: {}", phoneNumber);
            }


            case "USER" -> {
                System.err.println("customere girdi");

                Customer byId = customerRepository.findByUserId(userId);
                Customer byPhone = customerRepository.findByPhoneNumber(phoneNumber);

                if ((byId != null && EnumUserStatus.ACTIVE.name().equalsIgnoreCase(byId.getStatus())) ||
                        (byPhone != null && EnumUserStatus.ACTIVE.name().equalsIgnoreCase(byPhone.getStatus()))) {
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
                        .status(EnumUserStatus.ACTIVE.name())
                        .notificationLanguage(acceptLanguage)
                        .build();
                customerRepository.save(newCustomer);
                log.info("musteri tapilmadi, yeni yaradildi: {}", phoneNumber);
            }

            default ->
                    throw new ResourceNotFoundException(EnumMessagesLangValues.INVALID_ROLE.getMessageByLang(acceptLanguage));
        }
    }


    public OffsetDateTime getUtcTimeFromDayAndTimeAndTimeZone(LocalDate date, LocalTime time, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDateTime localDateTime = LocalDateTime.of(date, time);
        return localDateTime.atZone(zoneId).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    public LocalDate getUtcDayFromUtcTime(OffsetDateTime utcTime) {
        return utcTime.toLocalDate();
    }


    public LocalTime getLocalTimeFromUtcUseTZ(OffsetDateTime utcDateTime, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        return utcDateTime.atZoneSameInstant(zoneId).toLocalTime();
    }

    public String formatAppointmentDate(LocalTime dateTime, String acceptLanguage) {
        if (dateTime == null) return null;

        Locale locale = acceptLanguage != null && !acceptLanguage.isEmpty()
                ? Locale.forLanguageTag(acceptLanguage)
                : Locale.getDefault();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale);

        return dateTime.format(formatter);
    }

    public OffsetDateTime getLocalDateTimeFromUtcUseTZ(OffsetDateTime utcDateTime, String timezone) {
        if (utcDateTime == null) return null;
        ZoneId zone = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        return utcDateTime.atZoneSameInstant(zone).toOffsetDateTime();
    }

    public String formatAppointmentDate(OffsetDateTime dateTime, String acceptLanguage) {
        if (dateTime == null) return null;

        Locale locale = acceptLanguage != null && !acceptLanguage.isEmpty()
                ? Locale.forLanguageTag(acceptLanguage)
                : Locale.getDefault();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale);
        return dateTime.format(formatter);
    }
}
