package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingInboxResponse;
import com.carland.carland_service.dto.booking.CarResponseForSlotPanel;
import com.carland.carland_service.dto.booking.BookingRejectRequest;
import com.carland.carland_service.dto.booking.BookingServiceLineView;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.StaffNotesResponse;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingInspection;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.BookingStaffNote;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BrandRepository;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingInspectionRepository;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.RangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * tr: Staff gelen istekler: inbox, kabul, ret. Flag'e bağlı değil.
 * en: Staff inbox / accept / reject. Not gated by the owner booking flag.
 */
@Service
@RequiredArgsConstructor
public class StaffBookingService {

    static final String DEFAULT_TZ = "Asia/Baku";
    static final String BOARD = "board";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> BOARD_STATUSES = List.of(
            BookingStatus.PENDING.apiValue(),
            BookingStatus.CONFIRMED.apiValue(),
            BookingStatus.AUTO_ACCEPTED.apiValue(),
            BookingStatus.REJECTED.apiValue(),
            BookingStatus.CANCELLED.apiValue(),
            BookingStatus.COMPLETED.apiValue(),
            BookingStatus.NO_SHOW.apiValue());

    private final BookingStaffAccess bookingStaffAccess;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingIndividualLineRepository individualLineRepository;
    private final BookingInspectionRepository inspectionRepository;
    private final RangeRepository rangeRepository;
    private final BookingCapacityService bookingCapacityService;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final BookingStaffNoteService bookingStaffNoteService;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public StaffNotesResponse notes(Long userId, boolean mustChangePassword, String kind, String acceptLanguage) {
        requireStaff(userId, mustChangePassword, acceptLanguage);
        return bookingStaffNoteService.list(kind, acceptLanguage);
    }

    @Transactional(readOnly = true)
    public BookingInboxResponse inbox(Long userId, boolean mustChangePassword, String status, Long branchId,
                                      Integer page, Integer pageSize, String timezone, String acceptLanguage) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        String wanted = status == null || status.isBlank() ? BookingStatus.PENDING.apiValue() : status.trim().toLowerCase();
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        boolean board = BOARD.equals(wanted);
        Page<Booking> result;
        if (branchId != null) {
            bookingStaffAccess.requireWritableBranch(staff, branchId, acceptLanguage);
            result = board
                    ? bookingRepository.findByBranch_IdAndStatusInOrderByCreatedAtDesc(branchId, BOARD_STATUSES, pageable)
                    : bookingRepository.findByBranch_IdAndStatusOrderByCreatedAtDesc(branchId, wanted, pageable);
        } else if (BookingStaffRole.BRANCH_ADMIN.name().equals(staff.getRole())) {
            if (staff.getBranch() == null) {
                throw new ForbiddenException("branch required");
            }
            Long ownBranch = staff.getBranch().getId();
            result = board
                    ? bookingRepository.findByBranch_IdAndStatusInOrderByCreatedAtDesc(ownBranch, BOARD_STATUSES, pageable)
                    : bookingRepository.findByBranch_IdAndStatusOrderByCreatedAtDesc(ownBranch, wanted, pageable);
        } else {
            Long partnerId = staff.getPartner().getId();
            result = board
                    ? bookingRepository.findByBranch_Partner_IdAndStatusInOrderByCreatedAtDesc(partnerId, BOARD_STATUSES, pageable)
                    : bookingRepository.findByBranch_Partner_IdAndStatusOrderByCreatedAtDesc(partnerId, wanted, pageable);
        }
        List<Booking> rows = result.getContent();
        Map<Long, List<String>> keys = keysByBooking(rows);
        Map<Long, List<BookingIndividualLine>> lines = linesByBooking(rows);
        Map<Long, BookingInspection> inspections = inspectionsByBooking(rows);
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone.trim();
        String lang = BookingMineService.langOf(acceptLanguage);
        Map<String, Long> brandIds = new HashMap<>();
        List<BookingView> items = new ArrayList<>();
        for (Booking booking : rows) {
            items.add(toView(booking, keys.getOrDefault(booking.getId(), List.of()), tz, lang,
                    lines.getOrDefault(booking.getId(), List.of()), inspections.get(booking.getId()), brandIds));
        }
        return BookingInboxResponse.builder()
                .page(safePage)
                .pageSize(safeSize)
                .items(items)
                .build();
    }

    @Transactional
    public BookingView accept(Long userId, boolean mustChangePassword, Long bookingId, String timezone,
                              String acceptLanguage) {
        return decide(userId, mustChangePassword, bookingId, BookingStatus.CONFIRMED.apiValue(), null, timezone, acceptLanguage);
    }

    @Transactional
    public BookingView reject(Long userId, boolean mustChangePassword, Long bookingId, BookingRejectRequest request,
                              String timezone, String acceptLanguage) {
        BookingStaffNoteService.Applied applied = bookingStaffNoteService.apply(
                BookingStaffNote.CANCEL, request, acceptLanguage);
        return decide(userId, mustChangePassword, bookingId, BookingStatus.REJECTED.apiValue(),
                applied, timezone, acceptLanguage);
    }

    /**
     * tr: Təsdiqlənmiş booku completed edir. Xidmət sətirləri webhookdan gəlir.
     * en: Marks a confirmed booking completed. Service lines arrive from the webhook.
     */
    @Transactional
    public BookingView complete(Long userId, boolean mustChangePassword, Long bookingId, String timezone,
                                String acceptLanguage) {
        return decide(userId, mustChangePassword, bookingId, BookingStatus.COMPLETED.apiValue(),
                null, timezone, acceptLanguage);
    }

    /**
     * tr: Təsdiqlənmiş booku no_show edir və səbəbi yazır.
     * en: Marks a confirmed booking no_show and stores the reason.
     */
    @Transactional
    public BookingView noShow(Long userId, boolean mustChangePassword, Long bookingId, BookingRejectRequest request,
                              String timezone, String acceptLanguage) {
        BookingStaffNoteService.Applied applied = bookingStaffNoteService.apply(
                BookingStaffNote.NO_SHOW, request, acceptLanguage);
        return decide(userId, mustChangePassword, bookingId, BookingStatus.NO_SHOW.apiValue(),
                applied, timezone, acceptLanguage);
    }

    private BookingView decide(Long userId, boolean mustChangePassword, Long bookingId, String nextStatus,
                               BookingStaffNoteService.Applied applied, String timezone, String acceptLanguage) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        bookingStaffAccess.requireWritableBranch(staff, booking.getBranch().getId(), acceptLanguage);
        String current = booking.getStatus() == null ? "" : booking.getStatus().toLowerCase();
        if (BookingStatus.REJECTED.apiValue().equals(nextStatus)) {
            if (!BookingStatus.PENDING.apiValue().equals(current)) {
                throw new ConflictException("booking is not pending");
            }
        } else if (BookingStatus.COMPLETED.apiValue().equals(nextStatus)
                || BookingStatus.NO_SHOW.apiValue().equals(nextStatus)) {
            if (!BookingStatus.CONFIRMED.apiValue().equals(current)
                    && !BookingStatus.AUTO_ACCEPTED.apiValue().equals(current)) {
                throw new ConflictException("booking is not confirmed");
            }
        } else if (!BookingStatus.PENDING.apiValue().equals(current)) {
            throw new ConflictException("booking is not pending");
        }
        if (BookingStatus.CONFIRMED.apiValue().equals(nextStatus)) {
            Range range = booking.getRange();
            if (range == null || range.getRangeId() == null) {
                throw new ResourceNotFoundException("slot not found");
            }
            Range locked = rangeRepository.lockByRangeId(range.getRangeId())
                    .orElseThrow(() -> new ResourceNotFoundException("slot not found"));
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
            if (!BookingStatus.PENDING.apiValue().equals(booking.getStatus())) {
                throw new ConflictException("booking is not pending");
            }
            bookingCapacityService.requirePlace(locked, acceptLanguage);
            booking.setStatus(nextStatus);
            bookingRepository.saveAndFlush(booking);
            bookingCapacityService.closePendingWhenFull(locked);
        } else {
            booking.setStatus(nextStatus);
            if (applied != null) {
                String code = applied.code();
                if ((code == null || code.isBlank())
                        && BookingStatus.REJECTED.apiValue().equals(nextStatus)) {
                    code = BookingStatus.REJECTED.apiValue();
                }
                booking.setCancelReasonCode(code);
                booking.setCancelNote(applied.note());
            }
        }
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone.trim();
        String lang = BookingMineService.langOf(acceptLanguage);
        List<String> keys = bookingItemRepository.findByBooking_IdOrderByIdAsc(bookingId).stream()
                .map(BookingItem::getServiceKey)
                .toList();
        List<BookingIndividualLine> lines = individualLineRepository.findByBooking_IdOrderByIdAsc(bookingId);
        java.util.Optional<BookingInspection> found = inspectionRepository.findByBooking_Id(bookingId);
        BookingInspection inspection = found == null ? null : found.orElse(null);
        return toView(booking, keys, tz, lang, lines == null ? List.of() : lines, inspection, new HashMap<>());
    }

    private BookingStaff requireStaff(Long userId, boolean mustChangePassword, String acceptLanguage) {
        if (userId == null) {
            throw new ForbiddenException("Staff token required");
        }
        if (mustChangePassword) {
            throw new ForbiddenException("Şifrəni dəyişdirməlisiniz");
        }
        String lang = acceptLanguage == null ? "az" : acceptLanguage;
        return bookingStaffAccess.requireActive(userId, lang);
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

    private Map<Long, List<BookingIndividualLine>> linesByBooking(List<Booking> rows) {
        List<Long> ids = rows.stream().map(Booking::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<BookingIndividualLine> found = individualLineRepository.findByBooking_IdIn(ids);
        if (found == null || found.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<BookingIndividualLine>> grouped = new HashMap<>();
        for (BookingIndividualLine line : found) {
            if (line.getBooking() == null || line.getBooking().getId() == null) {
                continue;
            }
            grouped.computeIfAbsent(line.getBooking().getId(), id -> new ArrayList<>()).add(line);
        }
        return grouped;
    }

    private Map<Long, BookingInspection> inspectionsByBooking(List<Booking> rows) {
        List<Long> ids = rows.stream().map(Booking::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<BookingInspection> found = inspectionRepository.findByBooking_IdIn(ids);
        if (found == null || found.isEmpty()) {
            return Map.of();
        }
        Map<Long, BookingInspection> grouped = new HashMap<>();
        for (BookingInspection row : found) {
            if (row.getBooking() != null && row.getBooking().getId() != null) {
                grouped.put(row.getBooking().getId(), row);
            }
        }
        return grouped;
    }

    private BookingView toView(Booking booking, List<String> keys, String timezone, String lang,
                               List<BookingIndividualLine> lines, BookingInspection inspection,
                               Map<String, Long> brandIds) {
        Range range = booking.getRange();
        Calendar calendar = range == null ? null : range.getCalendar();
        String mode = range == null || range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
        List<BookingServiceLineView> individualServices = new ArrayList<>();
        if (lines != null) {
            for (BookingIndividualLine line : lines) {
                individualServices.add(BookingSelectionViews.line(line, lang));
            }
        }
        Customer customer = booking.getCustomerUserId() == null
                ? null : customerRepository.findByUserId(booking.getCustomerUserId());
        Car car = booking.getCarId() == null ? null : carRepository.findByCarId(booking.getCarId());
        String customerName = customer == null ? null : joinName(customer.getName(), customer.getSurname());
        return BookingView.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .status(booking.getStatus())
                .bookingMode(mode)
                .branchId(booking.getBranch() == null ? null : booking.getBranch().getId())
                .slotId(range == null ? null : range.getRangeId())
                .day(calendar == null || calendar.getDay() == null ? null : calendar.getDay().toString())
                .start(clock(range == null ? null : range.getStart(), timezone))
                .end(clock(range == null ? null : range.getEnd(), timezone))
                .timezone(timezone)
                .vin(booking.getVin())
                .carId(booking.getCarId())
                .customerName(customerName)
                .phone(customer == null ? null : customer.getPhoneNumber())
                .customerUserId(booking.getCustomerUserId())
                .plateNumber(car == null ? null : car.getPlateNumber())
                .carBrand(car == null ? null : car.getBrand())
                .carModel(car == null ? null : car.getModel())
                .car(carPanel(car, brandIds))
                .cancelReasonCode(booking.getCancelReasonCode())
                .cancelNote(booking.getCancelNote())
                .serviceKeys(keys)
                .packageName(booking.getPackageName())
                .packagePrice(booking.getPackagePrice())
                .individualServices(individualServices)
                .inspection(BookingSelectionViews.inspection(inspection))
                .serviceLabel(BookingSelectionViews.label(
                        booking.getPackageName(),
                        !individualServices.isEmpty(),
                        keys != null && !keys.isEmpty(),
                        inspection != null))
                .priceMin(booking.getPriceMin())
                .priceMax(booking.getPriceMax())
                .currency(booking.getCurrency() == null ? "AZN" : booking.getCurrency())
                .unit(BookingCreateService.UNIT)
                .unreadCount(0)
                .build();
    }

    private CarResponseForSlotPanel carPanel(Car car, Map<String, Long> brandIds) {
        if (car == null) {
            return null;
        }
        return CarResponseForSlotPanel.builder()
                .carId(car.getCarId())
                .brandId(brandId(car.getBrand(), brandIds))
                .brand(car.getBrand())
                .model(car.getModel())
                .plateNumber(car.getPlateNumber())
                .vin(car.getVin())
                .bodyType(car.getBodyType())
                .engineType(car.getEngineType())
                .modelYear(car.getModelYear())
                .engineVolume(car.getEngineVolume())
                .mileage(car.getMileage())
                .build();
    }

    private Long brandId(String brandName, Map<String, Long> brandIds) {
        if (brandName == null || brandName.isBlank()) {
            return null;
        }
        String key = brandName.trim().toLowerCase(Locale.ROOT);
        if (brandIds.containsKey(key)) {
            return brandIds.get(key);
        }
        Long id = pickBrandId(brandRepository.findAllByBrandNameIgnoreCase(brandName.trim()));
        brandIds.put(key, id);
        return id;
    }

    private static Long pickBrandId(List<Brand> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        for (Brand brand : rows) {
            if (".".equals(brand.getIsnew()) && brand.getStatus() != null
                    && "ACTIVE".equalsIgnoreCase(brand.getStatus())) {
                return brand.getBrandId();
            }
        }
        for (Brand brand : rows) {
            if (brand.getStatus() != null && "ACTIVE".equalsIgnoreCase(brand.getStatus())) {
                return brand.getBrandId();
            }
        }
        return rows.get(0).getBrandId();
    }

    private static String joinName(String name, String surname) {
        String first = name == null ? "" : name.trim();
        String last = surname == null ? "" : surname.trim();
        String joined = (first + " " + last).trim();
        return joined.isEmpty() ? null : joined;
    }

    private static String clock(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).toLocalTime().format(CLOCK);
    }
}
