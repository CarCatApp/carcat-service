package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * tr: Müşteri kaydı (USER) + takvim saat dilimi yardımcıları. Staff BookingStaff üzerinden gelir.
 * en: Customer record (USER) + calendar timezone helpers. Staff lives on BookingStaff.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Helper {

    private final CustomerRepository customerRepository;

    public void checkOrCreateUserByRole(Long userId, String role, String phoneNumber, String name,
                                        String surname, String acceptLanguage, Long inviterId) {
        if (role == null) {
            throw new ResourceNotFoundException(MessagesLangValues.INVALID_ROLE.getMessageByLang(acceptLanguage));
        }
        String normalized = role.trim().toUpperCase();
        if (UserRoles.USER.name().equals(normalized)) {
            ensureCustomer(userId, phoneNumber, name, surname, acceptLanguage);
            return;
        }
        if (UserRoles.PARTNER_ADMIN.name().equals(normalized)
                || UserRoles.BRANCH_ADMIN.name().equals(normalized)) {
            log.info("Service-side {} row skipped; booking_staff/auth users own this role userId={}",
                    normalized, userId);
            return;
        }
        throw new ResourceNotFoundException(MessagesLangValues.INVALID_ROLE.getMessageByLang(acceptLanguage));
    }

    private void ensureCustomer(Long userId, String phoneNumber, String name, String surname, String acceptLanguage) {
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
        customerRepository.save(Customer.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .name(name)
                .surname(surname)
                .createdAt(LocalDate.now())
                .status(UserStatus.ACTIVE.name())
                .notificationLanguage(acceptLanguage)
                .build());
        log.info("musteri tapilmadi, yeni yaradildi: {}", phoneNumber);
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
        if (dateTime == null) {
            return null;
        }
        Locale locale = acceptLanguage != null && !acceptLanguage.isEmpty()
                ? Locale.forLanguageTag(acceptLanguage)
                : Locale.getDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale);
        return dateTime.format(formatter);
    }

    public OffsetDateTime getLocalDateTimeFromUtcUseTZ(OffsetDateTime utcDateTime, String timezone) {
        if (utcDateTime == null) {
            return null;
        }
        ZoneId zone = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        return utcDateTime.atZoneSameInstant(zone).toOffsetDateTime();
    }

    public String formatAppointmentDate(OffsetDateTime dateTime, String acceptLanguage) {
        if (dateTime == null) {
            return null;
        }
        Locale locale = acceptLanguage != null && !acceptLanguage.isEmpty()
                ? Locale.forLanguageTag(acceptLanguage)
                : Locale.getDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale);
        return dateTime.format(formatter);
    }
}
