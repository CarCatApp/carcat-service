package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.CalendarRequest;
import com.carland.carland_service.dto.response.CalendarResponse;
import com.carland.carland_service.dto.response.RangeResponse;
import com.carland.carland_service.entity.Admin;
import com.carland.carland_service.entity.AutoService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.*;
import com.carland.carland_service.exceptions.*;
import com.carland.carland_service.repository.AdminRepository;
import com.carland.carland_service.repository.AutoServiceRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.service.CalendarService;
import com.carland.carland_service.service.impl.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * tr: Takvim yönetiminin implementasyonudur: admin için gün/saat aralıklı takvim oluşturur
 *     (saatleri UTC'ye çevirerek saklar) ve oto servise ait takvimi sorgular.
 * en: Implementation of calendar management: creates a calendar with day/time ranges for an admin
 *     (storing times converted to UTC) and queries the calendar of an auto service.
 */
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {
    private final AdminRepository adminRepository;
    private final CalendarRepository calendarRepository;
    private final Helper helper;
    private final AutoServiceRepository autoServiceRepository;

    /**
     * tr: Admin'in oto servisi için verilen gün/saat aralığında, rangeMinutes uzunluğunda dilimlerden
     *     oluşan yeni bir takvim oluşturur ve aralık listesini döner. Eksik alan veya geçersiz süre için
     *     MissingFieldException; admin bulunamazsa, geçmiş tarih/saat seçilirse InvalidStatusException;
     *     oto servis yoksa ResourceNotFoundException; aynı gün+kategori için takvim zaten varsa
     *     AlreadyExistsException fırlatır.
     * en: Creates a new calendar for the admin's auto service on the given day/time window, sliced into
     *     rangeMinutes-long ranges, and returns the range list. Throws MissingFieldException on missing
     *     fields or invalid duration; InvalidStatusException when the admin is not found or a past
     *     date/time is chosen; ResourceNotFoundException when the auto service is missing; and
     *     AlreadyExistsException when a calendar already exists for the same day+category.
     */
    @Override
    @Transactional
    public CalendarResponse createCalendar(CalendarRequest calendarRequest, String phoneNumber,
                                           String userIdHeader, String timezone, String acceptLanguage) {

        if (phoneNumber == null || userIdHeader == null || calendarRequest.getDay() == null ||
                calendarRequest.getStart() == null || calendarRequest.getEnd() == null ||
                calendarRequest.getRangeMinutes() == null || calendarRequest.getServiceCategory() == null ||
                calendarRequest.getWorkerCount() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Admin admin = adminRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader), phoneNumber,
                UserStatus.ACTIVE.name());

        if (admin == null) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }

        if (calendarRequest.getRangeMinutes() <= 0) {
            throw new MissingFieldException(MessagesLangValues.INVALID_RANGE_MINUTES.getMessageByLang(acceptLanguage));
        }

        LocalDate todayLocal = LocalDate.now(ZoneId.of(timezone));
        LocalTime nowLocal = LocalTime.now(ZoneId.of(timezone));

        if (calendarRequest.getDay().isBefore(todayLocal)) {
            throw new InvalidStatusException(MessagesLangValues.PAST_DATE_NOT_ALLOWED.getMessageByLang(acceptLanguage));
        }
        if (calendarRequest.getDay().isEqual(todayLocal) && calendarRequest.getStart().isBefore(nowLocal)) {
            throw new InvalidStatusException(MessagesLangValues.START_TIME_ALREADY_PASSED.getMessageByLang(acceptLanguage));
        }

        if (!calendarRequest.getStart().isBefore(calendarRequest.getEnd())) {
            throw new MissingFieldException(MessagesLangValues.START_AFTER_END.getMessageByLang(acceptLanguage));
        }

        AutoService autoService = admin.getAutoService();
        if (autoService == null) {
            throw new ResourceNotFoundException(MessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        OffsetDateTime startUtc = helper.getUtcTimeFromDayAndTimeAndTimeZone(calendarRequest.getDay(),
                calendarRequest.getStart(), timezone);
        OffsetDateTime endUtc = helper.getUtcTimeFromDayAndTimeAndTimeZone(
                calendarRequest.getDay(), calendarRequest.getEnd(), timezone);

        LocalDate utcDay = helper.getUtcDayFromUtcTime(startUtc);

        Calendar existingCalendar = calendarRepository.findByDayAndServiceCategoryAndAutoService(utcDay,
                calendarRequest.getServiceCategory(), autoService);
        if (existingCalendar != null) {
            throw new AlreadyExistsException(MessagesLangValues.CALENDAR_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }

        List<Range> rangeList = createRangeList(
                calendarRequest.getDay(),
                calendarRequest.getStart(),
                calendarRequest.getEnd(),
                calendarRequest.getRangeMinutes(),
                timezone,
                calendarRequest.getWorkerCount()
        );

        Calendar calendar = Calendar.builder()
                .day(utcDay)
                .start(startUtc)
                .end(endUtc)
                .autoService(autoService)
                .timeRanges(rangeList)
                .rangeMinutes(calendarRequest.getRangeMinutes())
                .status(CalendarStatus.ACTIVE.name())
                .serviceCategory(calendarRequest.getServiceCategory())
                .build();

        rangeList.forEach(range -> range.setCalendar(calendar));

        calendarRepository.save(calendar);

        List<RangeResponse> rangeResponseList = mapToRangeResponseList(rangeList, timezone, acceptLanguage);

        return CalendarResponse.builder()
                .timeRanges(rangeResponseList)
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    /**
     * tr: Verilen oto servis id'si, gün ve servis kategorisine göre takvimi bulur ve zaman aralıklarını
     *     kullanıcının saat dilimine çevirerek döner. Eksik alanlarda MissingFieldException; oto servis
     *     veya takvim bulunamazsa ResourceNotFoundException fırlatır.
     * en: Finds the calendar by the given auto service id, day, and service category, and returns its
     *     time ranges converted to the caller's timezone. Throws MissingFieldException on missing fields
     *     and ResourceNotFoundException when the auto service or calendar cannot be found.
     */
    @Override
    public CalendarResponse getCalendarByAutoServiceId(CalendarRequest request, String role, String phoneNumber,
                                                       String userIdHeader, String timezone, String acceptLanguage) {

        if (request == null || request.getDay() == null || request.getServiceCategory() == null || role == null ||
                phoneNumber == null || userIdHeader == null || request.getAutoServiceId() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        AutoService autoService = autoServiceRepository.findById(request.getAutoServiceId()).orElseThrow(() -> new
                ResourceNotFoundException(MessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage)));

//        Admin admin = adminRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader), phoneNumber,
//                UserStatus.ACTIVE.name());
//
//        if (admin == null) {
//            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
//        }
//        if (role.equals(UserRoles.ADMIN.name()) && !autoService.getAdmins().contains(admin)) {
//            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
//        }

        Calendar calendar = calendarRepository.findByDayAndServiceCategoryAndAutoService(request.getDay(),
                request.getServiceCategory(), autoService);

        if (calendar == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CALENDAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        List<Range> ranges = calendar.getTimeRanges();

        return CalendarResponse.builder()
                .timeRanges(mapToRangeResponseList(ranges, timezone, acceptLanguage))
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }


    private List<RangeResponse> mapToRangeResponseList(List<Range> rangeList, String timezone, String acceptLanguage) {
        return mapToRangeResponseList(rangeList, timezone, acceptLanguage, null);
    }

    private List<RangeResponse> mapToRangeResponseList(List<Range> rangeList, String timezone, String acceptLanguage, @Nullable OffsetDateTime cutoffUtc) {
        return rangeList.stream()
                .sorted(Comparator.comparing(Range::getStart))
                .filter(range -> cutoffUtc == null || range.getStart().isAfter(cutoffUtc))
                .map(range -> RangeResponse.builder()
                        .rangeId(range.getRangeId())
                        .start(helper.getLocalTimeFromUtcUseTZ(range.getStart(), timezone))
                        .end(helper.getLocalTimeFromUtcUseTZ(range.getEnd(), timezone))
                        .status(range.getStatus())
                        .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                        .freeCount(range.getWorkerCount() - range.getAppointments().size())
                        .build())
                .toList();
    }


    private List<Range> createRangeList(LocalDate day, LocalTime start, LocalTime end, Integer rangeMinutes, String timezone, Integer workerCount) {
        List<Range> ranges = new ArrayList<>();

        OffsetDateTime currentStartUtc = helper.getUtcTimeFromDayAndTimeAndTimeZone(day, start, timezone);
        OffsetDateTime endUtc = helper.getUtcTimeFromDayAndTimeAndTimeZone(day, end, timezone);

        while (currentStartUtc.isBefore(endUtc)) {
            OffsetDateTime currentEndUtc = currentStartUtc.plusMinutes(rangeMinutes);
            if (currentEndUtc.isAfter(endUtc)) {
                currentEndUtc = endUtc;
            }

            ranges.add(Range.builder()
                    .start(currentStartUtc)
                    .end(currentEndUtc)
                    .workerCount(workerCount)
                    .status(RangeStatus.AVAILABLE.name())
                    .build()
            );

            if (currentEndUtc.equals(endUtc)) {
                break;
            }

            currentStartUtc = currentEndUtc;
        }

        return ranges;
    }

}
