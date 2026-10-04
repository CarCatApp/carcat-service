package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCalendarDayView;
import com.carland.carland_service.dto.booking.BookingCalendarRequest;
import com.carland.carland_service.dto.booking.BookingCalendarResponse;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
        Selection selection = selection(branch, request == null ? new BookingCalendarRequest() : request, lang);
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        OffsetDateTime now = OffsetDateTime.now(StaffSlotWindows.ZONE);
        Map<LocalDate, List<Range>> byDay = rangesByDay(
                calendarRepository.findByBranchIdAndDayBetween(branch.getId(), today, end));

        List<BookingCalendarDayView> days = new ArrayList<>();
        for (LocalDate day = today; !day.isAfter(end); day = day.plusDays(1)) {
            boolean open = false;
            for (Range range : byDay.getOrDefault(day, List.of())) {
                if (bookable(range, now) && matches(range, selection)) {
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

    private Selection selection(Branch branch, BookingCalendarRequest request, String lang) {
        List<String> problems = new ArrayList<>();
        Long packageId = null;
        if (request.getPackageId() != null) {
            BranchCarePackage pkg = carePackageRepository.findById(request.getPackageId()).orElse(null);
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
        if (request.getIndividualServiceIds() != null) {
            for (Long serviceId : request.getIndividualServiceIds()) {
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
        boolean repair = request.getIssue() != null && !request.getIssue().isBlank();
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

    private boolean bookable(Range range, OffsetDateTime now) {
        if (range == null || range.getStart() == null || !range.getStart().isAfter(now)) {
            return false;
        }
        if (!RangeStatus.AVAILABLE.name().equals(range.getStatus())) {
            return false;
        }
        int capacity = range.getWorkerCount() == null ? 0 : range.getWorkerCount();
        int appointments = range.getAppointments() == null ? 0 : range.getAppointments().size();
        long booked = range.getRangeId() == null ? 0
                : bookingRepository.countByRange_RangeIdAndStatusIn(
                range.getRangeId(), BookingStatus.occupyingCapacity());
        return capacity - appointments - (int) booked > 0;
    }

    private static boolean matches(Range range, Selection selection) {
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

    private record Selection(Long packageId, Set<Long> serviceIds, boolean repair) {
    }
}
