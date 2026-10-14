package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCalendarDayView;
import com.carland.carland_service.dto.booking.BookingCalendarRequest;
import com.carland.carland_service.dto.booking.BookingCalendarResponse;
import com.carland.carland_service.dto.booking.BookingDayRangeView;
import com.carland.carland_service.dto.booking.BookingDayRangesRequest;
import com.carland.carland_service.dto.booking.BookingDayRangesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * tr: Flutter ay takvimi. Bugünden ay sonuna, yazılabilen saati olan gün available.
 * en: Flutter month calendar. From today through month end, a day is available when a bookable hour exists.
 */
@Service
@RequiredArgsConstructor
public class BookingCalendarService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final BranchRepository branchRepository;
    private final CalendarRepository calendarRepository;
    private final BookingRepository bookingRepository;
    private final BranchCarePackageRepository carePackageRepository;
    private final IndividualServiceRepository individualServiceRepository;
    private final BranchIndividualServiceRepository branchIndividualServiceRepository;
    private final ServiceCategoryJson json;

    /**
     * tr: Seçilen paket, hizmet veya şikayet için bu ayın günlerini boyar.
     * en: Paints this month's days for the selected package, services, or complaint.
     */
    @Transactional(readOnly = true)
    public BookingCalendarResponse month(Long branchId, BookingCalendarRequest request, String acceptLanguage) {
        String lang = lang(acceptLanguage);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }
        BookingCalendarRequest body = request == null ? new BookingCalendarRequest() : request;
        Selection selection = selection(branch, body.getPackageId(), body.getIndividualServiceIds(), body.getIssue(), lang);
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        OffsetDateTime now = OffsetDateTime.now(StaffSlotWindows.ZONE);
        Set<Long> held = heldByCar(body.getCarId());
        boolean single = selection.single();
        Map<LocalDate, List<Range>> byDay = rangesByDay(
                calendarRepository.findByBranchIdAndDayBetween(branch.getId(), today, end));

        List<BookingCalendarDayView> days = new ArrayList<>();
        for (LocalDate day = today; !day.isAfter(end); day = day.plusDays(1)) {
            List<Range> dayRanges = byDay.getOrDefault(day, List.of());
            Set<LocalTime> blocked = single ? heldMatchingStarts(held, dayRanges, selection) : Set.of();
            boolean open = false;
            for (Range range : dayRanges) {
                if (bookable(range, now) && matches(range, selection) && !taken(range, held, blocked, single)) {
                    open = true;
                    break;
                }
            }
            days.add(BookingCalendarDayView.builder()
                    .date(day.format(DAY))
                    .available(open)
                    .build());
        }
        return BookingCalendarResponse.builder()
                .days(days)
                .build();
    }

    /**
     * tr: Seçilen günün boş yerli saatleri. Aynı arabanın tuttuğu range elenir.
     * Tek hizmetse o saat düşer. Paket veya individual hâlâ yazılabiliyorsa o range 09:00 olarak kalır.
     * en: Free hours of the chosen day. A range this car already holds is skipped.
     * A single offering loses that hour. A usable package or individual keeps 09:00.
     */
    @Transactional(readOnly = true)
    public BookingDayRangesResponse day(Long branchId, BookingDayRangesRequest request, String acceptLanguage) {
        String lang = lang(acceptLanguage);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }
        BookingDayRangesRequest body = request == null ? new BookingDayRangesRequest() : request;
        Selection selection = selection(branch, body.getPackageId(), body.getIndividualServiceIds(), body.getIssue(), lang);
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate day = parseDay(body.getDate(), today, end, lang);
        OffsetDateTime now = OffsetDateTime.now(StaffSlotWindows.ZONE);
        Set<Long> held = heldByCar(body.getCarId());
        boolean single = selection.single();
        List<Range> ranges = rangesByDay(calendarRepository.findByBranchIdAndDayBetween(branch.getId(), day, day))
                .getOrDefault(day, List.of());
        Set<LocalTime> blocked = single ? heldMatchingStarts(held, ranges, selection) : Set.of();

        Map<LocalTime, Range> winners = new HashMap<>();
        Map<LocalTime, Integer> seats = new HashMap<>();
        for (Range range : ranges) {
            if (range.getEnd() == null || !bookable(range, now) || !matches(range, selection)
                    || taken(range, held, blocked, single)) {
                continue;
            }
            LocalTime start = clock(range.getStart());
            int left = remaining(range);
            Range current = winners.get(start);
            if (current == null || better(range, left, current, seats.get(start))) {
                winners.put(start, range);
                seats.put(start, left);
            }
        }
        List<BookingDayRangeView> views = winners.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> BookingDayRangeView.builder()
                        .rangeId(entry.getValue().getRangeId())
                        .start(entry.getKey().format(CLOCK))
                        .end(clock(entry.getValue().getEnd()).format(CLOCK))
                        .remaining(seats.get(entry.getKey()))
                        .build())
                .toList();
        return BookingDayRangesResponse.builder().ranges(views).build();
    }

    Selection selection(Branch branch, Long requestedPackageId, List<Long> requestedServiceIds,
                        String issue, String lang) {
        List<String> problems = new ArrayList<>();
        Long packageId = null;
        if (requestedPackageId != null) {
            BranchCarePackage pkg = carePackageRepository.findById(requestedPackageId).orElse(null);
            if (pkg == null || pkg.getBranch() == null || !branch.getId().equals(pkg.getBranch().getId())) {
                problems.add(sentence(lang, "Seçilmiş paket bu şubədə yoxdur.",
                        "The selected package is not on this branch.",
                        "Выбранный пакет не принадлежит этому филиалу."));
            } else if (!Boolean.TRUE.equals(pkg.getActive())) {
                problems.add(sentence(lang, pkg.getName() + " bağlıdır.",
                        pkg.getName() + " is off.", pkg.getName() + " выключен."));
            } else {
                packageId = pkg.getId();
            }
        }
        Set<Long> serviceIds = new LinkedHashSet<>();
        if (requestedServiceIds != null) {
            for (Long serviceId : requestedServiceIds) {
                if (serviceId == null || !serviceIds.add(serviceId)) {
                    continue;
                }
                IndividualService catalog = individualServiceRepository.findById(serviceId).orElse(null);
                String name = catalog == null ? null : title(catalog.getTitleJson(), lang, catalog.getCode());
                if (catalog == null || !Boolean.TRUE.equals(catalog.getActive())) {
                    problems.add(name == null
                            ? sentence(lang, "Seçilmiş xidmət bu şubədə yoxdur.",
                            "The selected service is not on this branch.",
                            "Выбранная услуга не принадлежит этому филиалу.")
                            : sentence(lang, name + " bağlıdır.", name + " is off.", name + " выключен."));
                    serviceIds.remove(serviceId);
                    continue;
                }
                BranchIndividualService row = branchIndividualServiceRepository
                        .findByBranch_IdAndIndividualService_Id(branch.getId(), serviceId)
                        .orElse(null);
                if (row == null || !Boolean.TRUE.equals(row.getActive())) {
                    problems.add(sentence(lang, name + " bağlıdır.", name + " is off.", name + " выключен."));
                    serviceIds.remove(serviceId);
                }
            }
        }
        boolean repair = issue != null && !issue.isBlank();
        if (!problems.isEmpty()) {
            throw new ConflictException(String.join(" ", new LinkedHashSet<>(problems)));
        }
        if (packageId == null && serviceIds.isEmpty() && !repair) {
            throw new MissingFieldException(sentence(lang,
                    "Ən azı bir paket, xidmət və ya şikayət mətni göndərin.",
                    "Send at least a package, a service, or a complaint.",
                    "Отправьте пакет, услугу или текст жалобы."));
        }
        return new Selection(packageId, serviceIds, repair);
    }

    private static Map<LocalDate, List<Range>> rangesByDay(List<Calendar> calendars) {
        Map<LocalDate, List<Range>> byDay = new HashMap<>();
        if (calendars == null) {
            return byDay;
        }
        for (Calendar calendar : calendars) {
            if (calendar.getDay() == null || calendar.getTimeRanges() == null) {
                continue;
            }
            byDay.computeIfAbsent(calendar.getDay(), ignored -> new ArrayList<>()).addAll(calendar.getTimeRanges());
        }
        return byDay;
    }

    private Set<Long> heldByCar(Long carId) {
        if (carId == null) {
            return Set.of();
        }
        List<Long> ids = bookingRepository.findRangeIdsByCarIdAndStatusIn(carId, BookingStatus.holdingCarOnRange());
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> held = new HashSet<>();
        for (Number id : ids) {
            if (id != null) {
                held.add(id.longValue());
            }
        }
        return held;
    }

    /**
     * tr: Tek hizmette tutulan range'in saati. Aynı başlangıçtaki kardeş range de düşer.
     * en: Start times a single offering already holds. A sibling range at that start drops too.
     */
    private static Set<LocalTime> heldMatchingStarts(Set<Long> held, List<Range> ranges, Selection selection) {
        if (held.isEmpty() || ranges == null || ranges.isEmpty()) {
            return Set.of();
        }
        Set<LocalTime> starts = new HashSet<>();
        for (Range range : ranges) {
            if (range.getRangeId() != null && held.contains(range.getRangeId())
                    && range.getStart() != null && matches(range, selection)) {
                starts.add(clock(range.getStart()));
            }
        }
        return starts;
    }

    private static boolean taken(Range range, Set<Long> held, Set<LocalTime> blocked, boolean single) {
        if (range.getRangeId() != null && held.contains(range.getRangeId())) {
            return true;
        }
        return single && range.getStart() != null && blocked.contains(clock(range.getStart()));
    }

    /**
     * tr: Tek hizmette aynı başlangıcı da kapatır. Karışık seçimde yalnız aynı range.
     * en: A single offering also blocks the same start. A mixed selection blocks only that range.
     */
    boolean carRepeatsOffering(Long carId, Range wanted, Selection selection) {
        if (carId == null || wanted == null || wanted.getRangeId() == null) {
            return false;
        }
        Set<Long> held = heldByCar(carId);
        if (held.contains(wanted.getRangeId())) {
            return true;
        }
        if (selection == null || !selection.single() || wanted.getStart() == null || wanted.getCalendar() == null
                || wanted.getCalendar().getDay() == null || wanted.getCalendar().getBranch() == null) {
            return false;
        }
        LocalDate day = wanted.getCalendar().getDay();
        Long branchId = wanted.getCalendar().getBranch().getId();
        List<Range> ranges = rangesByDay(calendarRepository.findByBranchIdAndDayBetween(branchId, day, day))
                .getOrDefault(day, List.of());
        return heldMatchingStarts(held, ranges, selection).contains(clock(wanted.getStart()));
    }

    private boolean bookable(Range range, OffsetDateTime now) {
        if (range == null || range.getStart() == null || range.getEnd() == null || !range.getStart().isAfter(now)) {
            return false;
        }
        if (SlotOffer.hidden(range) || !RangeStatus.AVAILABLE.name().equals(range.getStatus())) {
            return false;
        }
        return remaining(range) > 0;
    }

    private int remaining(Range range) {
        int capacity = range.getWorkerCount() == null ? 0 : range.getWorkerCount();
        int appointments = range.getAppointments() == null ? 0 : range.getAppointments().size();
        long booked = range.getRangeId() == null ? 0
                : bookingRepository.countByRange_RangeIdAndStatusIn(
                range.getRangeId(), BookingStatus.occupyingCapacity());
        return capacity - appointments - (int) booked;
    }

    private static LocalTime clock(OffsetDateTime time) {
        return time.atZoneSameInstant(StaffSlotWindows.ZONE).toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private static boolean better(Range candidate, int candidateLeft, Range current, int currentLeft) {
        if (candidateLeft != currentLeft) {
            return candidateLeft > currentLeft;
        }
        int rank = Integer.compare(priority(candidate), priority(current));
        if (rank != 0) {
            return rank < 0;
        }
        long candidateId = candidate.getRangeId() == null ? Long.MAX_VALUE : candidate.getRangeId();
        long currentId = current.getRangeId() == null ? Long.MAX_VALUE : current.getRangeId();
        return candidateId < currentId;
    }

    private static int priority(Range range) {
        if (StaffSlotTargets.PACKAGE.equals(range.getSlotTarget())) {
            return 0;
        }
        if (StaffSlotTargets.INDIVIDUAL.equals(range.getSlotTarget())) {
            return 1;
        }
        return 2;
    }

    private static LocalDate parseDay(String raw, LocalDate today, LocalDate end, String lang) {
        if (raw == null || raw.isBlank()) {
            throw new MissingFieldException(sentence(lang, "Tarix göndərin.",
                    "Send a date.", "Отправьте дату."));
        }
        LocalDate day;
        try {
            day = LocalDate.parse(raw.trim(), DAY);
        } catch (DateTimeParseException ex) {
            throw new MissingFieldException(sentence(lang, "Tarix gün.ay.il olmalıdır.",
                    "The date must be day.month.year.", "Дата должна быть в виде день.месяц.год."));
        }
        if (day.isBefore(today) || day.isAfter(end)) {
            throw new MissingFieldException(sentence(lang,
                    "Tarix bu ayın bugündən sonuna qədər olmalıdır.",
                    "The date must be from today through the end of this month.",
                    "Дата должна быть с сегодняшнего дня до конца этого месяца."));
        }
        return day;
    }

    static boolean matches(Range range, Selection selection) {
        String target = range.getSlotTarget();
        if (StaffSlotTargets.PACKAGE.equals(target) && selection.packageId != null
                && range.getCarePackage() != null
                && selection.packageId.equals(range.getCarePackage().getId())) {
            return true;
        }
        if (StaffSlotTargets.INDIVIDUAL.equals(target) && range.getIndividualService() != null
                && selection.serviceIds.contains(range.getIndividualService().getId())) {
            return true;
        }
        return selection.repair && StaffSlotTargets.REPAIR_INSPECTION.equals(target);
    }

    private String title(String titleJson, String lang, String fallback) {
        Map<String, String> titles = json.read(titleJson);
        String named = titles.get(lang);
        if (named == null || named.isBlank()) {
            named = titles.get("az");
        }
        return named == null || named.isBlank() ? fallback : named;
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

    record Selection(Long packageId, Set<Long> serviceIds, boolean repair) {
        /** Paket, tek individual veya yalnız təmir. Birkaç hizmet birlikte değil. */
        boolean single() {
            int services = serviceIds == null ? 0 : serviceIds.size();
            int kinds = (packageId != null ? 1 : 0) + (services > 0 ? 1 : 0) + (repair ? 1 : 0);
            return kinds == 1 && services <= 1;
        }
    }
}
