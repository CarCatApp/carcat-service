package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingAvailabilityDayView;
import com.carland.carland_service.dto.booking.BookingAvailabilityResponse;
import com.carland.carland_service.dto.booking.BookingAvailabilitySlotView;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Müşteri müsaitlik (CRCT-283). Ay = gün özeti; tek gün = slot listesi.
 * en: Owner availability (CRCT-283). Month = day summary; single day = slots.
 */
@Service
@RequiredArgsConstructor
public class BookingAvailabilityService {

    static final String DEFAULT_TZ = "Asia/Baku";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> LIVE_BOOKING = BookingStatus.occupyingCapacity();

    private final BranchRepository branchRepository;
    private final CalendarRepository calendarRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public BookingAvailabilityResponse availability(Long branchId, String serviceKeysRaw, String fromRaw,
                                                    String toRaw, String timezoneHeader) {
        if (fromRaw == null || fromRaw.isBlank() || toRaw == null || toRaw.isBlank()) {
            throw new MissingFieldException("from and to are required");
        }
        LocalDate fromDay = parseDay(fromRaw);
        LocalDate toDay = parseDay(toRaw);
        if (toDay.isBefore(fromDay)) {
            throw new MissingFieldException("to must be on or after from");
        }
        String timezone = timezoneHeader == null || timezoneHeader.isBlank() ? DEFAULT_TZ : timezoneHeader.trim();
        ZoneId zone = zoneOf(timezone);
        List<String> wanted = parseKeys(serviceKeysRaw);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }

        List<Calendar> calendars = calendarRepository.findByBranchIdAndDayBetween(branchId, fromDay, toDay);
        Map<LocalDate, List<BookingAvailabilitySlotView>> byDay = new LinkedHashMap<>();
        OffsetDateTime nowUtc = OffsetDateTime.now();
        LocalDate today = LocalDate.now(zone);

        for (Calendar calendar : calendars) {
            if (calendar.getTimeRanges() == null) {
                continue;
            }
            List<Range> ranges = new ArrayList<>(calendar.getTimeRanges());
            ranges.sort(Comparator.comparing(Range::getStart));
            for (Range range : ranges) {
                if (!matchesKey(range.getServiceKey(), wanted)) {
                    continue;
                }
                BookingAvailabilitySlotView slot = toSlot(range, timezone, nowUtc);
                byDay.computeIfAbsent(calendar.getDay(), ignored -> new ArrayList<>()).add(slot);
            }
        }

        boolean singleDay = fromDay.equals(toDay);
        if (singleDay) {
            List<BookingAvailabilitySlotView> ranges = byDay.getOrDefault(fromDay, List.of());
            return BookingAvailabilityResponse.builder()
                    .branchId(branchId)
                    .from(fromDay.toString())
                    .to(toDay.toString())
                    .day(fromDay.toString())
                    .timezone(timezone)
                    .serviceKeys(wanted)
                    .remainingPercent(percent(ranges))
                    .ranges(ranges)
                    .build();
        }

        List<BookingAvailabilityDayView> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<BookingAvailabilitySlotView>> e : byDay.entrySet()) {
            List<BookingAvailabilitySlotView> slots = e.getValue();
            if (slots.isEmpty()) {
                continue;
            }
            days.add(BookingAvailabilityDayView.builder()
                    .day(e.getKey().format(ISO_DAY))
                    .state(state(e.getKey(), today, slots))
                    .remainingPercent(percent(slots))
                    .build());
        }
        days.sort(Comparator.comparing(BookingAvailabilityDayView::getDay));
        return BookingAvailabilityResponse.builder()
                .branchId(branchId)
                .from(fromDay.toString())
                .to(toDay.toString())
                .timezone(timezone)
                .serviceKeys(wanted)
                .days(days)
                .build();
    }

    private BookingAvailabilitySlotView toSlot(Range range, String timezone, OffsetDateTime nowUtc) {
        int capacity = range.getWorkerCount() == null ? 0 : range.getWorkerCount();
        int appointments = range.getAppointments() == null ? 0 : range.getAppointments().size();
        long bookingCount = range.getRangeId() == null ? 0
                : bookingRepository.countByRange_RangeIdAndStatusIn(range.getRangeId(), LIVE_BOOKING);
        int booked = appointments + (int) bookingCount;
        int remaining = Math.max(0, capacity - booked);
        boolean open = RangeStatus.AVAILABLE.name().equals(range.getStatus());
        boolean future = range.getStart() != null && range.getStart().isAfter(nowUtc);
        boolean bookable = open && remaining > 0 && future;
        String status = bookable ? "OPEN" : (!open ? range.getStatus() : (remaining <= 0 ? "FULL" : "OPEN"));
        String key = range.getServiceKey() == null || range.getServiceKey().isBlank() ? "*" : range.getServiceKey();
        String mode = range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
        return BookingAvailabilitySlotView.builder()
                .slotId(range.getRangeId())
                .start(clock(range.getStart(), timezone))
                .end(clock(range.getEnd(), timezone))
                .capacity(capacity)
                .bookedCount(booked)
                .remaining(remaining)
                .bookingMode(mode)
                .serviceKey(key)
                .status(status)
                .bookable(bookable)
                .build();
    }

    static boolean matchesKey(String rangeKey, List<String> wanted) {
        String slot = rangeKey == null || rangeKey.isBlank() ? "*" : rangeKey.trim();
        if (wanted.isEmpty() || wanted.contains("*") || "*".equals(slot)) {
            return true;
        }
        return wanted.contains(slot);
    }

    static List<String> parseKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("*");
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out.isEmpty() ? List.of("*") : List.copyOf(out);
    }

    private static int percent(List<BookingAvailabilitySlotView> slots) {
        int cap = 0;
        int rem = 0;
        for (BookingAvailabilitySlotView s : slots) {
            cap += s.getCapacity() == null ? 0 : s.getCapacity();
            rem += s.getRemaining() == null ? 0 : s.getRemaining();
        }
        if (cap <= 0) {
            return 0;
        }
        return (int) Math.round(rem * 100.0 / cap);
    }

    private static String state(LocalDate day, LocalDate today, List<BookingAvailabilitySlotView> slots) {
        if (day.isBefore(today)) {
            return "unavailable";
        }
        boolean any = slots.stream().anyMatch(s -> Boolean.TRUE.equals(s.getBookable())
                || (s.getRemaining() != null && s.getRemaining() > 0));
        if (!any) {
            return "full";
        }
        return "available";
    }

    private static String clock(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).toLocalTime().format(CLOCK);
    }

    private static LocalDate parseDay(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new MissingFieldException("from/to must be yyyy-MM-dd");
        }
    }

    private static ZoneId zoneOf(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            throw new MissingFieldException("invalid timezone");
        }
    }
}
