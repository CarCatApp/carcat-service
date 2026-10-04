package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingAppointmentRequest;
import com.carland.carland_service.dto.booking.BookingAppointmentResponse;
import com.carland.carland_service.dto.booking.BookingAppointmentServiceView;
import com.carland.carland_service.dto.booking.BookingServiceLineView;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.RangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * tr: Seçilen range'e paket, fərdi xidmət və şikayəti tek book olarak yazar.
 * en: Writes the package, individual services and complaint onto one booking for the chosen range.
 */
@Service
@RequiredArgsConstructor
public class BookingAppointmentService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> LIVE = BookingStatus.occupyingCapacity();

    private final RangeRepository rangeRepository;
    private final BookingRepository bookingRepository;
    private final BranchCarePackageRepository carePackageRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final BookingCalendarService calendarService;
    private final BookingSelectionWriter selectionWriter;

    /**
     * tr: Range kilitlenir, yeri varsa book ve alt satırları yazılır.
     * en: Locks the range and writes the booking plus its lines when a place remains.
     */
    @Transactional
    public BookingAppointmentResponse create(Long branchId, BookingAppointmentRequest request,
                                             Long customerUserId, String acceptLanguage) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        BookingAppointmentRequest body = request == null ? new BookingAppointmentRequest() : request;
        if (body.getRangeId() == null) {
            throw MissingFieldException.required("rangeId");
        }
        if (body.getCarId() == null) {
            throw MissingFieldException.required("carId");
        }
        String lang = lang(acceptLanguage);
        Range range = rangeRepository.lockByRangeId(body.getRangeId())
                .orElseThrow(() -> new ResourceNotFoundException("slot not found"));
        Calendar calendar = range.getCalendar();
        Branch branch = calendar == null ? null : calendar.getBranch();
        if (branch == null
                || !branch.getId().equals(branchId)
                || !Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("slot not found");
        }
        BookingCalendarService.Selection selection = calendarService.selection(
                branch, body.getPackageId(), body.getIndividualServiceIds(), body.getIssue(), lang);
        if (!RangeStatus.AVAILABLE.name().equals(range.getStatus())
                || range.getStart() == null
                || !range.getStart().isAfter(OffsetDateTime.now(StaffSlotWindows.ZONE))
                || !BookingCalendarService.matches(range, selection)) {
            throw new ConflictException(sentence(lang, "Bu saat artıq yazıla bilməz.",
                    "This hour can no longer be booked.",
                    "Это время уже нельзя забронировать."));
        }
        if (remaining(range) <= 0) {
            throw new ConflictException(sentence(lang, "Bu saatda yer qalmayıb.",
                    "This hour has no places left.",
                    "На это время мест не осталось."));
        }
        Customer customer = customerRepository.findByUserId(customerUserId);
        if (customer == null) {
            throw new ResourceNotFoundException("customer not found");
        }
        Car car = carRepository.findByCarIdAndCustomer(body.getCarId(), customer);
        if (car == null) {
            throw new ResourceNotFoundException("car not found");
        }
        String mode = range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
        String status = BookingMode.APPROVAL.apiValue().equals(mode)
                ? BookingStatus.PENDING.apiValue()
                : BookingStatus.AUTO_ACCEPTED.apiValue();
        BookingSelectionWriter.Priced priced = extras(branch, body);
        int packageQepik = packageQepik(selection.packageId());
        Booking booking = bookingRepository.save(Booking.builder()
                .ref(nextRef())
                .customerUserId(customerUserId)
                .branch(branch)
                .range(range)
                .status(status)
                .priceMin(packageQepik + (priced == null ? 0 : priced.priceMin()))
                .priceMax(packageQepik + (priced == null ? 0 : priced.priceMax()))
                .currency("AZN")
                .vin(car.getVin())
                .carId(car.getCarId())
                .carePackageId(selection.packageId())
                .packageName(packageName(selection.packageId()))
                .packagePrice(selection.packageId() == null ? null : packageQepik)
                .build());
        if (priced != null) {
            selectionWriter.save(booking, customer, car, priced);
        }
        return BookingAppointmentResponse.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .status(status)
                .bookingMode(mode)
                .branchId(branch.getId())
                .rangeId(range.getRangeId())
                .date(day(calendar))
                .start(clock(range.getStart()))
                .end(clock(range.getEnd()))
                .carId(car.getCarId())
                .vin(car.getVin())
                .plateNumber(car.getPlateNumber())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carYear(car.getModelYear())
                .packageId(booking.getCarePackageId())
                .packageName(booking.getPackageName())
                .packagePrice(booking.getPackagePrice())
                .individualServices(serviceViews(priced, lang))
                .issue(priced == null ? null : priced.message())
                .priceMin(booking.getPriceMin())
                .priceMax(booking.getPriceMax())
                .currency("AZN")
                .unit(BookingCreateService.UNIT)
                .build();
    }

    private BookingSelectionWriter.Priced extras(Branch branch, BookingAppointmentRequest body) {
        boolean services = body.getIndividualServiceIds() != null && body.getIndividualServiceIds().stream().anyMatch(id -> id != null);
        boolean issue = body.getIssue() != null && !body.getIssue().isBlank();
        if (!services && !issue) {
            return null;
        }
        return selectionWriter.price(branch, body.getIndividualServiceIds(), body.getIssue());
    }

    private int packageQepik(Long packageId) {
        if (packageId == null) {
            return 0;
        }
        BranchCarePackage pkg = carePackageRepository.findById(packageId).orElse(null);
        if (pkg == null || pkg.getPrice() == null) {
            return 0;
        }
        return Math.multiplyExact(pkg.getPrice(), 100);
    }

    private String packageName(Long packageId) {
        if (packageId == null) {
            return null;
        }
        BranchCarePackage pkg = carePackageRepository.findById(packageId).orElse(null);
        return pkg == null ? null : pkg.getName();
    }

    private int remaining(Range range) {
        int capacity = range.getWorkerCount() == null ? 0 : range.getWorkerCount();
        int appointments = range.getAppointments() == null ? 0 : range.getAppointments().size();
        long booked = range.getRangeId() == null ? 0
                : bookingRepository.countByRange_RangeIdAndStatusIn(range.getRangeId(), LIVE);
        return capacity - appointments - (int) booked;
    }

    private List<BookingAppointmentServiceView> serviceViews(BookingSelectionWriter.Priced priced, String lang) {
        if (priced == null || priced.rows().isEmpty()) {
            return List.of();
        }
        List<BookingAppointmentServiceView> views = new ArrayList<>();
        for (BranchIndividualService row : priced.rows()) {
            IndividualService catalog = row.getIndividualService();
            BookingServiceLineView line = BookingSelectionViews.line(BookingIndividualLine.builder()
                    .individualServiceId(catalog.getId())
                    .code(catalog.getCode())
                    .titleJson(catalog.getTitleJson())
                    .priceSimple(row.getPriceSimple())
                    .priceMedium(row.getPriceMedium())
                    .priceComplex(row.getPriceComplex())
                    .build(), lang);
            views.add(BookingAppointmentServiceView.builder()
                    .id(catalog.getId())
                    .name(line.getName())
                    .priceMin(line.getPriceMin())
                    .priceMax(line.getPriceMax())
                    .currency("AZN")
                    .unit(BookingCreateService.UNIT)
                    .build());
        }
        return views;
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

    private static String day(Calendar calendar) {
        LocalDate date = calendar == null ? null : calendar.getDay();
        return date == null ? null : date.format(DAY);
    }

    private static String clock(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZoneSameInstant(StaffSlotWindows.ZONE).toLocalTime().format(CLOCK);
    }

    private static String lang(String header) {
        if (header == null || header.isBlank()) {
            return "az";
        }
        String value = header.toLowerCase(Locale.ROOT);
        if (value.startsWith("en")) {
            return "en";
        }
        if (value.startsWith("ru")) {
            return "ru";
        }
        return "az";
    }

    private static String sentence(String lang, String az, String en, String ru) {
        if ("en".equals(lang)) {
            return en;
        }
        if ("ru".equals(lang)) {
            return ru;
        }
        return az;
    }
}
