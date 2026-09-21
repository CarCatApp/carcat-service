package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingQuoteResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.BookingWriteRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.BranchService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.RangeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * tr: Quote (yazmaz) + create. instant→confirmed, approval→pending. Kapasite kilitli.
 * en: Quote (no write) + create. instant→confirmed, approval→pending. Locked capacity.
 */
@Service
@RequiredArgsConstructor
public class BookingCreateService {

    static final String UNIT = "qepik";
    static final String DEFAULT_TZ = "Asia/Baku";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> LIVE = List.of(
            BookingStatus.PENDING.apiValue(), BookingStatus.CONFIRMED.apiValue());

    private final RangeRepository rangeRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BranchPackageRepository packageRepository;
    private final BranchServiceRepository serviceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BookingQuoteResponse quote(BookingWriteRequest request) {
        Prepared prepared = prepare(request, false);
        return BookingQuoteResponse.builder()
                .branchId(prepared.branch.getId())
                .slotId(prepared.range.getRangeId())
                .serviceKeys(prepared.keys)
                .priceMin(prepared.priceMin)
                .priceMax(prepared.priceMax)
                .currency("AZN")
                .unit(UNIT)
                .build();
    }

    @Transactional
    public BookingView create(BookingWriteRequest request, Long customerUserId, String timezoneHeader) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        Prepared prepared = prepare(request, true);
        String timezone = timezoneHeader == null || timezoneHeader.isBlank() ? DEFAULT_TZ : timezoneHeader.trim();
        String mode = modeOf(prepared.range);
        String status = BookingMode.APPROVAL.apiValue().equals(mode)
                ? BookingStatus.PENDING.apiValue()
                : BookingStatus.CONFIRMED.apiValue();
        String vin = blankToNull(request.getVin());
        Booking booking = bookingRepository.save(Booking.builder()
                .ref(nextRef())
                .customerUserId(customerUserId)
                .branch(prepared.branch)
                .range(prepared.range)
                .status(status)
                .priceMin(prepared.priceMin)
                .priceMax(prepared.priceMax)
                .currency("AZN")
                .vin(vin)
                .carId(request.getCarId())
                .build());
        for (Line line : prepared.lines) {
            bookingItemRepository.save(BookingItem.builder()
                    .booking(booking)
                    .serviceKey(line.key)
                    .titleSnapshot(line.title)
                    .priceMin(line.priceMin)
                    .priceMax(line.priceMax)
                    .build());
        }
        Calendar calendar = prepared.range.getCalendar();
        return BookingView.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .status(status)
                .bookingMode(mode)
                .branchId(prepared.branch.getId())
                .slotId(prepared.range.getRangeId())
                .day(calendar.getDay() == null ? null : calendar.getDay().toString())
                .start(clock(prepared.range.getStart(), timezone))
                .end(clock(prepared.range.getEnd(), timezone))
                .timezone(timezone)
                .vin(vin)
                .carId(request.getCarId())
                .serviceKeys(prepared.keys)
                .priceMin(prepared.priceMin)
                .priceMax(prepared.priceMax)
                .currency("AZN")
                .unit(UNIT)
                .unreadCount(0)
                .build();
    }

    private Prepared prepare(BookingWriteRequest request, boolean lock) {
        if (request == null || request.getBranchId() == null || request.getSlotId() == null) {
            throw MissingFieldException.required("branchId, slotId");
        }
        List<String> keys = normalizeKeys(request.getServiceKeys());
        if (keys.isEmpty()) {
            throw MissingFieldException.required("serviceKeys");
        }
        Range range = lock
                ? rangeRepository.lockByRangeId(request.getSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("slot not found"))
                : loadRange(request.getSlotId());
        Calendar calendar = range.getCalendar();
        if (calendar == null || calendar.getBranch() == null) {
            throw new ResourceNotFoundException("slot not found");
        }
        Branch branch = calendar.getBranch();
        if (!request.getBranchId().equals(branch.getId())
                || !Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("slot not found");
        }
        if (!RangeStatus.AVAILABLE.name().equals(range.getStatus())
                || range.getStart() == null
                || !range.getStart().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("slot_unavailable");
        }
        if (!BookingAvailabilityService.matchesKey(range.getServiceKey(), keys)) {
            throw new ConflictException("slot_unavailable");
        }
        int remaining = remaining(range);
        if (remaining <= 0) {
            throw new ConflictException("capacity_full");
        }
        List<Line> lines = priceLines(branch.getId(), keys);
        rejectPackageOverlap(lines);
        int min = 0;
        int max = 0;
        for (Line line : lines) {
            min += line.priceMin == null ? 0 : line.priceMin;
            max += line.priceMax == null ? 0 : line.priceMax;
        }
        return new Prepared(range, branch, keys, min, max, lines);
    }

    private Range loadRange(Long slotId) {
        Range range = rangeRepository.findByRangeId(slotId);
        if (range == null) {
            throw new ResourceNotFoundException("slot not found");
        }
        return range;
    }

    private int remaining(Range range) {
        int capacity = range.getWorkerCount() == null ? 0 : range.getWorkerCount();
        int appointments = range.getAppointments() == null ? 0 : range.getAppointments().size();
        long live = range.getRangeId() == null ? 0
                : bookingRepository.countByRange_RangeIdAndStatusIn(range.getRangeId(), LIVE);
        return Math.max(0, capacity - appointments - (int) live);
    }

    private List<Line> priceLines(Long branchId, List<String> keys) {
        List<BranchPackage> packages = packageRepository.findByBranchIdAndActiveTrueOrderByIdAsc(branchId);
        List<BranchService> services = serviceRepository.findByBranchIdAndActiveTrueOrderByIdAsc(branchId);
        List<Line> lines = new ArrayList<>();
        for (String key : keys) {
            BranchPackage pkg = packages.stream().filter(p -> key.equals(p.getServiceKey())).findFirst().orElse(null);
            if (pkg != null) {
                lines.add(new Line(key, firstTitle(pkg.getTitleJson()), pkg.getPriceMin(), pkg.getPriceMax(),
                        stringList(pkg.getIncludedServiceKeys())));
                continue;
            }
            BranchService svc = services.stream().filter(s -> key.equals(s.getServiceKey())).findFirst().orElse(null);
            if (svc == null) {
                throw new ResourceNotFoundException("unknown serviceKey: " + key);
            }
            lines.add(new Line(key, firstTitle(svc.getTitleJson()), svc.getPriceMin(), svc.getPriceMax(), List.of()));
        }
        return lines;
    }

    private static void rejectPackageOverlap(List<Line> lines) {
        Set<String> selected = new HashSet<>();
        for (Line line : lines) {
            selected.add(line.key);
        }
        for (Line line : lines) {
            for (String included : line.included) {
                if (selected.contains(included)) {
                    throw new ConflictException("serviceKey is already included in the package");
                }
            }
        }
    }

    private List<String> stringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String firstTitle(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            var map = objectMapper.readValue(json, new TypeReference<java.util.Map<String, String>>() {});
            if (map.containsKey("en")) {
                return map.get("en");
            }
            return map.values().stream().findFirst().orElse(json);
        } catch (Exception ex) {
            return json;
        }
    }

    private String nextRef() {
        for (int i = 0; i < 25; i++) {
            String ref = "CC-" + String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            if (!bookingRepository.existsByRef(ref)) {
                return ref;
            }
        }
        throw new ConflictException("ref_retry");
    }

    static List<String> normalizeKeys(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String key : raw) {
            if (key != null && !key.isBlank()) {
                out.add(key.trim());
            }
        }
        return List.copyOf(out);
    }

    private static String modeOf(Range range) {
        return range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
    }

    private static String clock(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).toLocalTime().format(CLOCK);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Line(String key, String title, Integer priceMin, Integer priceMax, List<String> included) {}

    private record Prepared(Range range, Branch branch, List<String> keys, int priceMin, int priceMax, List<Line> lines) {}
}
