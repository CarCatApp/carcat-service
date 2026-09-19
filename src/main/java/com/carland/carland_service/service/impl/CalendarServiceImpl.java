package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.CalendarRequest;
import com.carland.carland_service.dto.response.CalendarResponse;
import com.carland.carland_service.dto.response.RangeResponse;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.CalendarStatus;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.service.BookingStaffAccess;
import com.carland.carland_service.service.CalendarService;
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

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CalendarRepository calendarRepository;
    private final BranchRepository branchRepository;
    private final BookingStaffAccess bookingStaffAccess;
    private final Helper helper;

    @Override
    @Transactional
    public CalendarResponse createCalendar(CalendarRequest calendarRequest, String phoneNumber,
                                           String userIdHeader, String timezone, String acceptLanguage) {

        if (phoneNumber == null || userIdHeader == null || calendarRequest.getDay() == null ||
                calendarRequest.getStart() == null || calendarRequest.getEnd() == null ||
                calendarRequest.getRangeMinutes() == null || calendarRequest.getServiceCategory() == null ||
                calendarRequest.getWorkerCount() == null || calendarRequest.getBranchId() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        BookingStaff staff = bookingStaffAccess.requireActive(Long.valueOf(userIdHeader), acceptLanguage);
        Branch branch = bookingStaffAccess.requireWritableBranch(
                staff, calendarRequest.getBranchId(), acceptLanguage);

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

        OffsetDateTime startUtc = helper.getUtcTimeFromDayAndTimeAndTimeZone(calendarRequest.getDay(),
                calendarRequest.getStart(), timezone);
        OffsetDateTime endUtc = helper.getUtcTimeFromDayAndTimeAndTimeZone(
                calendarRequest.getDay(), calendarRequest.getEnd(), timezone);
        LocalDate utcDay = helper.getUtcDayFromUtcTime(startUtc);

        Calendar existingCalendar = calendarRepository.findByDayAndServiceCategoryAndBranch(
                utcDay, calendarRequest.getServiceCategory(), branch);
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
                .branch(branch)
                .timeRanges(rangeList)
                .rangeMinutes(calendarRequest.getRangeMinutes())
                .status(CalendarStatus.ACTIVE.name())
                .serviceCategory(calendarRequest.getServiceCategory())
                .build();
        rangeList.forEach(range -> range.setCalendar(calendar));
        calendarRepository.save(calendar);

        return CalendarResponse.builder()
                .timeRanges(mapToRangeResponseList(rangeList, timezone, acceptLanguage))
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Override
    public CalendarResponse getCalendarByBranchId(CalendarRequest request, String role, String phoneNumber,
                                                  String userIdHeader, String timezone, String acceptLanguage) {
        if (request == null || request.getDay() == null || request.getServiceCategory() == null || role == null ||
                phoneNumber == null || userIdHeader == null || request.getBranchId() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage)));

        Calendar calendar = calendarRepository.findByDayAndServiceCategoryAndBranch(
                request.getDay(), request.getServiceCategory(), branch);
        if (calendar == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CALENDAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return CalendarResponse.builder()
                .timeRanges(mapToRangeResponseList(calendar.getTimeRanges(), timezone, acceptLanguage))
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    private List<RangeResponse> mapToRangeResponseList(List<Range> rangeList, String timezone, String acceptLanguage) {
        return mapToRangeResponseList(rangeList, timezone, acceptLanguage, null);
    }

    private List<RangeResponse> mapToRangeResponseList(List<Range> rangeList, String timezone, String acceptLanguage,
                                                       @Nullable OffsetDateTime cutoffUtc) {
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

    private List<Range> createRangeList(LocalDate day, LocalTime start, LocalTime end, Integer rangeMinutes,
                                        String timezone, Integer workerCount) {
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
                    .build());
            if (currentEndUtc.equals(endUtc)) {
                break;
            }
            currentStartUtc = currentEndUtc;
        }
        return ranges;
    }
}
