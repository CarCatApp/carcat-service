package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCancelRequest;
import com.carland.carland_service.dto.booking.BookingCancelReasonView;
import com.carland.carland_service.dto.booking.BookingCancelReasonsResponse;
import com.carland.carland_service.dto.booking.BookingCanceledReasonView;
import com.carland.carland_service.dto.booking.BookingCarView;
import com.carland.carland_service.dto.booking.BookingDetailResponse;
import com.carland.carland_service.dto.booking.BookingLineView;
import com.carland.carland_service.dto.booking.BookingMineResponse;
import com.carland.carland_service.dto.booking.BookingPatchRequest;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingCancelReason;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingCancelReasonRepository;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CarRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tr: Owner rezervasyon listesi + detay + iptal (CRCT-285). unread=0 ta ki 286.
 * en: Owner booking list + detail + cancel (CRCT-285). unread=0 until 286.
 */
@Service
@RequiredArgsConstructor
public class BookingMineService {

    static final String DEFAULT_TZ = "Asia/Baku";
    static final String PAST_OR_COMPLETED = "Cannot cancel a completed or past booking";
    static final String OTHER_CODE = "other";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter STARTS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final CarRepository carRepository;
    private final BookingCreateService bookingCreateService;
    private final BookingCancelReasonRepository cancelReasonRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BookingMineResponse mine(Long customerUserId, String statusCsv, Long carId,
                                    Integer page, Integer pageSize, Integer limit, String timezoneHeader) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        requireOwnedCar(customerUserId, carId);
        String timezone = timezoneHeader == null || timezoneHeader.isBlank() ? DEFAULT_TZ : timezoneHeader.trim();
        int safePage = page == null || page < 1 ? 1 : page;
        int size = limit != null ? limit : (pageSize == null ? 20 : pageSize);
        if (size < 1) {
            size = 20;
        }
        size = Math.min(size, 50);
        PageRequest pageable = PageRequest.of(safePage - 1, size, Sort.by("range.start").ascending());
        List<String> statuses = parseStatuses(statusCsv);
        Page<Booking> result = loadPage(customerUserId, carId, statuses, pageable);
        Map<Long, List<String>> keys = keysByBooking(result.getContent());
        List<BookingView> items = new ArrayList<>();
        for (Booking booking : result.getContent()) {
            items.add(toView(booking, keys.getOrDefault(booking.getId(), List.of()), timezone));
        }
        return BookingMineResponse.builder()
                .counts(countsOf(customerUserId, carId))
                .page(safePage)
                .pageSize(size)
                .total(result.getTotalElements())
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse detail(Long customerUserId, String bookingKey, String timezoneHeader) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        if (bookingKey == null || bookingKey.isBlank()) {
            throw MissingFieldException.required("bookingId");
        }
        String timezone = timezoneHeader == null || timezoneHeader.isBlank() ? DEFAULT_TZ : timezoneHeader.trim();
        Booking booking = loadOwned(customerUserId, bookingKey.trim());
        List<BookingItem> rows = booking.getId() == null
                ? List.of()
                : bookingItemRepository.findByBooking_IdOrderByIdAsc(booking.getId());
        List<BookingLineView> lines = new ArrayList<>();
        for (BookingItem row : rows) {
            lines.add(BookingLineView.builder()
                    .serviceKey(row.getServiceKey())
                    .title(row.getTitleSnapshot())
                    .priceMin(row.getPriceMin())
                    .priceMax(row.getPriceMax())
                    .build());
        }
        Range range = booking.getRange();
        Calendar calendar = range == null ? null : range.getCalendar();
        Branch branch = booking.getBranch();
        Partner partner = branch == null ? null : branch.getPartner();
        String mode = range == null || range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
        return BookingDetailResponse.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .status(booking.getStatus())
                .bookingMode(mode)
                .branchId(branch == null ? null : branch.getId())
                .branchName(branch == null ? null : branch.getName())
                .branchAddress(branch == null ? null : branch.getAddress())
                .partnerName(partner == null ? null : partner.getName())
                .slotId(range == null ? null : range.getRangeId())
                .day(calendar == null || calendar.getDay() == null ? null : calendar.getDay().toString())
                .start(clock(range == null ? null : range.getStart(), timezone))
                .end(clock(range == null ? null : range.getEnd(), timezone))
                .startsAt(startsAt(range == null ? null : range.getStart(), timezone))
                .timezone(timezone)
                .car(carOf(booking))
                .items(lines)
                .priceMin(booking.getPriceMin())
                .priceMax(booking.getPriceMax())
                .currency(booking.getCurrency() == null ? "AZN" : booking.getCurrency())
                .unit(BookingCreateService.UNIT)
                .unreadCount(0)
                .canceledReason(canceledReasonOf(booking))
                .build();
    }

    @Transactional(readOnly = true)
    public BookingCancelReasonsResponse cancelReasons() {
        List<BookingCancelReasonView> items = new ArrayList<>();
        for (BookingCancelReason row : cancelReasonRepository.findByActiveTrueOrderBySortOrderAscIdAsc()) {
            items.add(BookingCancelReasonView.builder()
                    .code(row.getCode())
                    .title(titles(row.getTitleJson()))
                    .build());
        }
        return BookingCancelReasonsResponse.builder().items(items).build();
    }

    @Transactional
    public BookingDetailResponse patch(Long customerUserId, String bookingKey, BookingPatchRequest request,
                                       String timezoneHeader) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        if (bookingKey == null || bookingKey.isBlank()) {
            throw MissingFieldException.required("bookingId");
        }
        Booking booking = loadOwned(customerUserId, bookingKey.trim());
        String status = booking.getStatus() == null ? "" : booking.getStatus().toLowerCase();
        if (BookingStatus.COMPLETED.apiValue().equals(status)) {
            throw new ConflictException("Completed bookings cannot be edited");
        }
        if (!BookingStatus.PENDING.apiValue().equals(status)
                && !BookingStatus.CONFIRMED.apiValue().equals(status)
                && !BookingStatus.AUTO_ACCEPTED.apiValue().equals(status)) {
            throw new ConflictException("booking cannot be edited");
        }
        Long slotId = request == null ? null : request.getSlotId();
        List<String> keys = request == null ? null : request.getServiceKeys();
        boolean hasKeys = keys != null && !BookingCreateService.normalizeKeys(keys).isEmpty();
        if (slotId == null && !hasKeys) {
            throw MissingFieldException.required("slotId or serviceKeys");
        }
        bookingCreateService.applyEdit(booking, slotId, hasKeys ? keys : null);
        return detail(customerUserId, String.valueOf(booking.getId()), timezoneHeader);
    }

    @Transactional
    public BookingDetailResponse cancel(Long customerUserId, String bookingKey, BookingCancelRequest request,
                                        String timezoneHeader) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        if (bookingKey == null || bookingKey.isBlank()) {
            throw MissingFieldException.required("bookingId");
        }
        Booking booking = loadOwned(customerUserId, bookingKey.trim());
        String status = booking.getStatus() == null ? "" : booking.getStatus().toLowerCase();
        if (BookingStatus.COMPLETED.apiValue().equals(status)) {
            throw new ConflictException(PAST_OR_COMPLETED);
        }
        if (!BookingStatus.PENDING.apiValue().equals(status)
                && !BookingStatus.CONFIRMED.apiValue().equals(status)
                && !BookingStatus.AUTO_ACCEPTED.apiValue().equals(status)) {
            throw new ConflictException("booking cannot be cancelled");
        }
        Range range = booking.getRange();
        OffsetDateTime start = range == null ? null : range.getStart();
        if (start == null || !start.isAfter(OffsetDateTime.now())) {
            throw new ConflictException(PAST_OR_COMPLETED);
        }
        String code = request == null || request.getReason() == null ? "" : request.getReason().trim();
        if (code.isEmpty()) {
            throw MissingFieldException.required("reason");
        }
        BookingCancelReason reason = cancelReasonRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new ResourceNotFoundException("cancel reason not found"));
        String note = request.getNote() == null ? null : request.getNote().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        if (OTHER_CODE.equals(reason.getCode()) && note == null) {
            throw MissingFieldException.required("note");
        }
        booking.setStatus(BookingStatus.CANCELLED.apiValue());
        booking.setCancelReasonCode(reason.getCode());
        booking.setCancelNote(note);
        return detail(customerUserId, String.valueOf(booking.getId()), timezoneHeader);
    }

    private void requireOwnedCar(Long customerUserId, Long carId) {
        if (carId == null) {
            return;
        }
        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            throw new ResourceNotFoundException("car not found");
        }
        Customer owner = car.getCustomer();
        if (owner == null || owner.getUserId() == null || !owner.getUserId().equals(customerUserId)) {
            throw new ForbiddenException("car is not yours");
        }
    }

    private Booking loadOwned(Long customerUserId, String bookingKey) {
        Booking booking;
        if (bookingKey.regionMatches(true, 0, "CC-", 0, 3)) {
            booking = bookingRepository.findByRef(bookingKey.toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        } else {
            Long id;
            try {
                id = Long.valueOf(bookingKey);
            } catch (NumberFormatException ex) {
                throw new ResourceNotFoundException("booking not found");
            }
            booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        }
        if (booking.getCustomerUserId() == null || !booking.getCustomerUserId().equals(customerUserId)) {
            throw new ForbiddenException("booking is not yours");
        }
        return booking;
    }

    private BookingCarView carOf(Booking booking) {
        Car car = booking.getCarId() == null ? null : carRepository.findByCarId(booking.getCarId());
        if (car == null) {
            return BookingCarView.builder()
                    .carId(booking.getCarId())
                    .vin(booking.getVin())
                    .build();
        }
        return BookingCarView.builder()
                .carId(car.getCarId())
                .vin(car.getVin() == null ? booking.getVin() : car.getVin())
                .brand(car.getBrand())
                .model(car.getModel())
                .year(car.getModelYear())
                .build();
    }

    private Page<Booking> loadPage(Long userId, Long carId, List<String> statuses, PageRequest pageable) {
        boolean filterStatus = !statuses.isEmpty();
        if (carId != null && filterStatus) {
            return bookingRepository.findByCustomerUserIdAndCarIdAndStatusIn(userId, carId, statuses, pageable);
        }
        if (carId != null) {
            return bookingRepository.findByCustomerUserIdAndCarId(userId, carId, pageable);
        }
        if (filterStatus) {
            return bookingRepository.findByCustomerUserIdAndStatusIn(userId, statuses, pageable);
        }
        return bookingRepository.findByCustomerUserId(userId, pageable);
    }

    private Map<String, Long> countsOf(Long userId, Long carId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            counts.put(status.apiValue(), 0L);
        }
        List<Object[]> rows = carId == null
                ? bookingRepository.countGroupByStatus(userId)
                : bookingRepository.countGroupByStatusAndCarId(userId, carId);
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String status = String.valueOf(row[0]);
            long n = row[1] instanceof Number number ? number.longValue() : 0L;
            counts.put(status, n);
        }
        return counts;
    }

    static List<String> parseStatuses(String statusCsv) {
        if (statusCsv == null || statusCsv.isBlank()) {
            return List.of();
        }
        Set<String> wanted = new LinkedHashSet<>();
        for (String raw : statusCsv.split(",")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String value = raw.trim().toLowerCase();
            if ("canceled".equals(value)) {
                value = BookingStatus.CANCELLED.apiValue();
            }
            wanted.add(value);
        }
        if (wanted.contains(BookingStatus.CONFIRMED.apiValue())) {
            wanted.add(BookingStatus.AUTO_ACCEPTED.apiValue());
        }
        return List.copyOf(wanted);
    }

    private Map<Long, List<String>> keysByBooking(List<Booking> rows) {
        List<Long> ids = rows.stream().map(Booking::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return bookingItemRepository.findByBooking_IdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getBooking().getId(),
                        Collectors.mapping(BookingItem::getServiceKey, Collectors.toList())
                ));
    }

    private BookingView toView(Booking booking, List<String> keys, String timezone) {
        Range range = booking.getRange();
        Calendar calendar = range == null ? null : range.getCalendar();
        Branch branch = booking.getBranch();
        Partner partner = branch == null ? null : branch.getPartner();
        String mode = range == null || range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
        return BookingView.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .status(booking.getStatus())
                .bookingMode(mode)
                .branchId(branch == null ? null : branch.getId())
                .branchName(branch == null ? null : branch.getName())
                .partnerName(partner == null ? null : partner.getName())
                .slotId(range == null ? null : range.getRangeId())
                .day(calendar == null || calendar.getDay() == null ? null : calendar.getDay().toString())
                .start(clock(range == null ? null : range.getStart(), timezone))
                .end(clock(range == null ? null : range.getEnd(), timezone))
                .startsAt(startsAt(range == null ? null : range.getStart(), timezone))
                .timezone(timezone)
                .vin(booking.getVin())
                .carId(booking.getCarId())
                .serviceKeys(keys)
                .priceMin(booking.getPriceMin())
                .priceMax(booking.getPriceMax())
                .currency(booking.getCurrency() == null ? "AZN" : booking.getCurrency())
                .unit(BookingCreateService.UNIT)
                .unreadCount(0)
                .build();
    }

    private BookingCanceledReasonView canceledReasonOf(Booking booking) {
        String code = booking.getCancelReasonCode();
        if (code == null || code.isBlank()) {
            return null;
        }
        BookingCancelReason row = cancelReasonRepository.findByCodeAndActiveTrue(code).orElse(null);
        return BookingCanceledReasonView.builder()
                .code(code)
                .title(row == null ? Map.of() : titles(row.getTitleJson()))
                .note(booking.getCancelNote())
                .build();
    }

    private Map<String, String> titles(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception ex) {
            return Map.of("az", json);
        }
    }

    private static String clock(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).toLocalTime().format(CLOCK);
    }

    private static String startsAt(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).format(STARTS);
    }
}
