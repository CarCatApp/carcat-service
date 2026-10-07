package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingQuoteResponse;
import com.carland.carland_service.dto.booking.BookingServiceLineView;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.BookingWriteRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingInspection;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.BookingSelectedService;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.BranchService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * tr: Quote (yazmaz) + create. instant→auto_accepted, approval→pending. Kapasite kilitli.
 * en: Quote (no write) + create. instant→auto_accepted, approval→pending. Locked capacity.
 */
@Service
@RequiredArgsConstructor
public class BookingCreateService {

    static final String UNIT = "qepik";
    static final String DEFAULT_TZ = "Asia/Baku";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> LIVE = BookingStatus.occupyingCapacity();

    private final RangeRepository rangeRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BranchPackageRepository packageRepository;
    private final BranchServiceRepository serviceRepository;
    private final BranchCarePackageRepository carePackageRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final BookingSelectedServiceRepository bookingSelectedServiceRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final ObjectMapper objectMapper;
    private final BookingSelectionWriter selectionWriter;
    private final BookingCapacityService bookingCapacityService;

    @Transactional(readOnly = true)
    public BookingQuoteResponse quote(BookingWriteRequest request) {
        Prepared prepared = prepare(request, false, null);
        BookingSelectionWriter.Priced priced = extras(prepared.branch, request);
        int pack = carePackageQepik(prepared.branch, request == null ? null : request.getCarePackageId());
        int extraMin = pack + (priced == null ? 0 : priced.priceMin());
        int extraMax = pack + (priced == null ? 0 : priced.priceMax());
        return BookingQuoteResponse.builder()
                .branchId(prepared.branch.getId())
                .slotId(prepared.range.getRangeId())
                .serviceKeys(prepared.keys)
                .priceMin(prepared.priceMin + extraMin)
                .priceMax(prepared.priceMax + extraMax)
                .currency("AZN")
                .unit(UNIT)
                .build();
    }

    @Transactional
    public BookingView create(BookingWriteRequest request, Long customerUserId, String timezoneHeader) {
        return create(request, customerUserId, timezoneHeader, "az");
    }

    @Transactional
    public BookingView create(BookingWriteRequest request, Long customerUserId, String timezoneHeader,
                              String acceptLanguage) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        Prepared prepared = prepare(request, true, null);
        Car car = requireOwnedCar(customerUserId, request);
        BookingSelectionWriter.Priced priced = extras(prepared.branch, request);
        String timezone = timezoneHeader == null || timezoneHeader.isBlank() ? DEFAULT_TZ : timezoneHeader.trim();
        String mode = modeOf(prepared.range);
        String status = BookingMode.APPROVAL.apiValue().equals(mode)
                ? BookingStatus.PENDING.apiValue()
                : BookingStatus.AUTO_ACCEPTED.apiValue();
        String vin = car.getVin();
        Booking booking = Booking.builder()
                .ref(nextRef())
                .customerUserId(customerUserId)
                .branch(prepared.branch)
                .range(prepared.range)
                .status(status)
                .priceMin(prepared.priceMin)
                .priceMax(prepared.priceMax)
                .currency("AZN")
                .vin(vin)
                .carId(car.getCarId())
                .build();
        applyCarePackage(booking, prepared.branch, request.getCarePackageId());
        if (priced != null) {
            int min = booking.getPriceMin() == null ? 0 : booking.getPriceMin();
            int max = booking.getPriceMax() == null ? 0 : booking.getPriceMax();
            booking.setPriceMin(min + priced.priceMin());
            booking.setPriceMax(max + priced.priceMax());
        }
        booking = bookingRepository.save(booking);
        if (BookingStatus.AUTO_ACCEPTED.apiValue().equals(status)) {
            bookingCapacityService.closePendingWhenFull(prepared.range);
        }
        BookingInspection inspection = null;
        if (priced != null) {
            Customer customer = customerRepository.findByUserId(customerUserId);
            inspection = selectionWriter.save(booking, customer, car, priced);
        }
        for (Line line : prepared.lines) {
            bookingItemRepository.save(BookingItem.builder()
                    .booking(booking)
                    .serviceKey(line.key)
                    .titleSnapshot(line.title)
                    .priceMin(line.priceMin)
                    .priceMax(line.priceMax)
                    .build());
        }
        attachOfferedServices(booking, request.getOfferedServiceIds());
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
                .carId(car.getCarId())
                .serviceKeys(prepared.keys)
                .packageName(booking.getPackageName())
                .packagePrice(booking.getPackagePrice())
                .individualServices(individualViews(priced, acceptLanguage))
                .inspection(BookingSelectionViews.inspection(inspection))
                .serviceLabel(BookingSelectionViews.label(
                        booking.getPackageName(),
                        priced != null && !priced.rows().isEmpty(),
                        !prepared.keys.isEmpty(),
                        inspection != null))
                .priceMin(booking.getPriceMin())
                .priceMax(booking.getPriceMax())
                .currency("AZN")
                .unit(UNIT)
                .unreadCount(0)
                .build();
    }

    @Transactional
    public void applyEdit(Booking booking, Long slotId, List<String> serviceKeys) {
        if (booking == null || booking.getBranch() == null || booking.getRange() == null) {
            throw new ResourceNotFoundException("booking not found");
        }
        Long targetSlot = slotId != null ? slotId : booking.getRange().getRangeId();
        List<String> keys = normalizeKeys(serviceKeys);
        if (keys.isEmpty()) {
            keys = bookingItemRepository.findByBooking_IdOrderByIdAsc(booking.getId()).stream()
                    .map(BookingItem::getServiceKey)
                    .filter(key -> key != null && !key.isBlank())
                    .map(String::trim)
                    .toList();
        }
        Prepared prepared = prepare(BookingWriteRequest.builder()
                .branchId(booking.getBranch().getId())
                .slotId(targetSlot)
                .serviceKeys(keys)
                .build(), true, booking.getId());
        booking.setRange(prepared.range);
        booking.setPriceMin(prepared.priceMin);
        booking.setPriceMax(prepared.priceMax);
        bookingItemRepository.deleteByBooking_Id(booking.getId());
        for (Line line : prepared.lines) {
            bookingItemRepository.save(BookingItem.builder()
                    .booking(booking)
                    .serviceKey(line.key)
                    .titleSnapshot(line.title)
                    .priceMin(line.priceMin)
                    .priceMax(line.priceMax)
                    .build());
        }
    }

    private BookingSelectionWriter.Priced extras(Branch branch, BookingWriteRequest request) {
        if (request == null || (!hasIds(request.getIndividualServiceIds()) && !hasText(request.getIssue()))) {
            return null;
        }
        return selectionWriter.price(branch, request.getIndividualServiceIds(), request.getIssue());
    }

    private int carePackageQepik(Branch branch, Long carePackageId) {
        if (carePackageId == null) {
            return 0;
        }
        BranchCarePackage pkg = carePackageRepository.findById(carePackageId)
                .orElseThrow(() -> new ResourceNotFoundException("care package not found"));
        if (!Boolean.TRUE.equals(pkg.getActive())
                || pkg.getBranch() == null
                || branch == null
                || !branch.getId().equals(pkg.getBranch().getId())) {
            throw new ResourceNotFoundException("care package not found");
        }
        return pkg.getPrice() == null ? 0 : Math.multiplyExact(pkg.getPrice(), 100);
    }

    private List<BookingServiceLineView> individualViews(BookingSelectionWriter.Priced priced, String acceptLanguage) {
        if (priced == null || priced.rows().isEmpty()) {
            return List.of();
        }
        String lang = BookingMineService.langOf(acceptLanguage);
        List<BookingServiceLineView> views = new ArrayList<>();
        for (BranchIndividualService row : priced.rows()) {
            IndividualService catalog = row.getIndividualService();
            views.add(BookingSelectionViews.line(BookingIndividualLine.builder()
                    .individualServiceId(catalog.getId())
                    .code(catalog.getCode())
                    .titleJson(catalog.getTitleJson())
                    .priceSimple(row.getPriceSimple())
                    .priceMedium(row.getPriceMedium())
                    .priceComplex(row.getPriceComplex())
                    .build(), lang));
        }
        return views;
    }

    private static boolean hasSelection(BookingWriteRequest request) {
        if (request == null) {
            return false;
        }
        return request.getCarePackageId() != null
                || hasIds(request.getIndividualServiceIds())
                || hasText(request.getIssue());
    }

    private static boolean hasIds(List<Long> ids) {
        if (ids == null) {
            return false;
        }
        for (Long id : ids) {
            if (id != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String issue) {
        return issue != null && !issue.trim().isEmpty();
    }

    private void applyCarePackage(Booking booking, Branch branch, Long carePackageId) {
        if (carePackageId == null) {
            return;
        }
        BranchCarePackage pkg = carePackageRepository.findById(carePackageId)
                .orElseThrow(() -> new ResourceNotFoundException("care package not found"));
        if (!Boolean.TRUE.equals(pkg.getActive())
                || pkg.getBranch() == null
                || branch == null
                || !branch.getId().equals(pkg.getBranch().getId())) {
            throw new ResourceNotFoundException("care package not found");
        }
        int qepik = pkg.getPrice() == null ? 0 : Math.multiplyExact(pkg.getPrice(), 100);
        booking.setCarePackageId(pkg.getId());
        booking.setPackageName(pkg.getName());
        booking.setPackagePrice(qepik);
        int min = booking.getPriceMin() == null ? 0 : booking.getPriceMin();
        int max = booking.getPriceMax() == null ? 0 : booking.getPriceMax();
        booking.setPriceMin(min + qepik);
        booking.setPriceMax(max + qepik);
    }

    private void attachOfferedServices(Booking booking, List<Long> offeredServiceIds) {
        if (offeredServiceIds == null || offeredServiceIds.isEmpty()) {
            return;
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : offeredServiceIds) {
            if (id == null || !seen.add(id)) {
                continue;
            }
            OfferedService service = offeredServiceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("offered service not found"));
            if (!Boolean.TRUE.equals(service.getActive())) {
                throw new ResourceNotFoundException("offered service not found");
            }
            bookingSelectedServiceRepository.save(BookingSelectedService.builder()
                    .booking(booking)
                    .offeredServiceId(service.getId())
                    .titleJson(service.getTitleJson())
                    .build());
        }
    }

    private Prepared prepare(BookingWriteRequest request, boolean lock, Long excludeBookingId) {
        if (request == null || request.getBranchId() == null || request.getSlotId() == null) {
            throw MissingFieldException.required("branchId, slotId");
        }
        List<String> keys = normalizeKeys(request.getServiceKeys());
        if (keys.isEmpty() && !hasSelection(request)) {
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
        if (SlotOffer.hidden(range)
                || !RangeStatus.AVAILABLE.name().equals(range.getStatus())
                || range.getStart() == null
                || !range.getStart().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("slot_unavailable");
        }
        if (!BookingAvailabilityService.matchesOffer(range, keys)) {
            throw new ConflictException("slot_unavailable");
        }
        int remaining = remaining(range, excludeBookingId);
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

    private int remaining(Range range, Long excludeBookingId) {
        int capacity = range.getWorkerCount() == null ? 0 : range.getWorkerCount();
        int appointments = range.getAppointments() == null ? 0 : range.getAppointments().size();
        long live = 0;
        if (range.getRangeId() != null) {
            live = excludeBookingId == null
                    ? bookingRepository.countByRange_RangeIdAndStatusIn(range.getRangeId(), LIVE)
                    : bookingRepository.countByRange_RangeIdAndStatusInAndIdNot(
                            range.getRangeId(), LIVE, excludeBookingId);
        }
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

    private Car requireOwnedCar(Long customerUserId, BookingWriteRequest request) {
        if (request == null || request.getCarId() == null) {
            throw MissingFieldException.required("carId");
        }
        Customer customer = customerRepository.findByUserId(customerUserId);
        if (customer == null) {
            throw new ResourceNotFoundException("customer not found");
        }
        Car car = carRepository.findByCarIdAndCustomer(request.getCarId(), customer);
        if (car == null) {
            throw new ResourceNotFoundException("car not found");
        }
        return car;
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

    private record Line(String key, String title, Integer priceMin, Integer priceMax, List<String> included) {}

    private record Prepared(Range range, Branch branch, List<String> keys, int priceMin, int priceMax, List<Line> lines) {}
}
