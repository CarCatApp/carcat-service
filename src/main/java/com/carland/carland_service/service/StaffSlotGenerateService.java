package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffSlotDayResponse;
import com.carland.carland_service.dto.booking.StaffSlotGenerateResponse;
import com.carland.carland_service.dto.booking.StaffSlotSkipView;
import com.carland.carland_service.dto.booking.StaffSlotView;
import com.carland.carland_service.dto.request.StaffSlotDayRule;
import com.carland.carland_service.dto.request.StaffSlotGenerateRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.entity.ServiceCategory;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.CalendarStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceCategoryRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.RangeRepository;
import com.carland.carland_service.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * tr: Partner panelinden gün gün slot üretir. Dolu gün o hedef için atlanır.
 * en: Generates slots from the partner panel, day by day. An occupied day is skipped for that target.
 */
@Service
@RequiredArgsConstructor
public class StaffSlotGenerateService {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int CAPACITY_MAX = 50;

    private final BookingStaffAccess bookingStaffAccess;
    private final BranchRepository branchRepository;
    private final CalendarRepository calendarRepository;
    private final RangeRepository rangeRepository;
    private final BookingRepository bookingRepository;
    private final BranchCarePackageRepository carePackageRepository;
    private final IndividualServiceRepository individualServiceRepository;
    private final BranchIndividualServiceRepository branchIndividualServiceRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final BranchServiceCategoryRepository branchCategoryRepository;
    private final ServiceCategoryJson json;

    /**
     * tr: Seçilen paket, tekil hizmet ve təmir+yoxlama için saat dilimlerini yazar.
     * en: Writes time slices for the selected packages, individual services, and repair+inspection.
     */
    @Transactional
    public StaffSlotGenerateResponse generate(Long userId, boolean mustChangePassword,
                                              StaffSlotGenerateRequest request, String acceptLanguage) {
        String lang = lang(acceptLanguage);
        if (request == null) {
            throw new MissingFieldException(sentence(lang, "Ən azı bir xidmət növü seçin.",
                    "Select at least one service.", "Выберите хотя бы одну услугу."));
        }
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, lang);
        Branch branch = resolveBranch(staff, request.getBranchId(), lang);
        List<Target> targets = resolveTargets(branch, request, lang);
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new MissingFieldException(sentence(lang, "Tarix aralığını seçin.",
                    "Select a date range.", "Выберите диапазон дат."));
        }
        if (request.getStartDate().isBefore(today)) {
            throw new MissingFieldException(sentence(lang, "Keçmiş tarixə slot yaratmaq mümkün deyil.",
                    "Slots cannot be created in the past.", "Нельзя создать слоты на прошедшую дату."));
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new MissingFieldException(sentence(lang, "Bitmə tarixi başlama tarixindən əvvəl ola bilməz.",
                    "The end date cannot be before the start date.", "Дата окончания не может быть раньше даты начала."));
        }
        int duration = request.getDurationMin() == null ? 0 : request.getDurationMin();
        if (duration <= 0) {
            throw new MissingFieldException(sentence(lang, "Slot intervalı 0-dan böyük olmalıdır.",
                    "The slot interval must be greater than 0.", "Интервал слота должен быть больше 0."));
        }
        Hours weekday = hours(request.getWeekday(), lang, false);
        boolean saturday = Boolean.TRUE.equals(request.getSaturday());
        boolean sunday = Boolean.TRUE.equals(request.getSunday());
        Hours weekend = saturday || sunday ? hours(request.getWeekend(), lang, true) : null;
        OffsetDateTime now = OffsetDateTime.now(StaffSlotWindows.ZONE);

        int created = 0;
        for (LocalDate day = request.getStartDate(); !day.isAfter(request.getEndDate()); day = day.plusDays(1)) {
            Hours rule = ruleFor(day, weekday, weekend, saturday, sunday);
            if (rule == null) {
                continue;
            }
            List<StaffSlotWindows.Slice> slices = StaffSlotWindows.slices(
                    day, rule.start, rule.end, duration, rule.breakStart, rule.breakEnd, now);
            Calendar calendar = null;
            for (Target target : targets) {
                if (occupied(branch.getId(), day, target)) {
                    target.skippedDays.add(day.toString());
                    continue;
                }
                if (slices.isEmpty()) {
                    continue;
                }
                if (calendar == null) {
                    calendar = calendarFor(branch, day, rule, duration);
                }
                for (StaffSlotWindows.Slice slice : slices) {
                    Range range = Range.builder()
                            .start(slice.start())
                            .end(slice.end())
                            .workerCount(rule.capacity)
                            .status(RangeStatus.AVAILABLE.name())
                            .bookingMode(rule.mode)
                            .slotTarget(target.kind)
                            .carePackage(target.carePackage)
                            .individualService(target.individualService)
                            .calendar(calendar)
                            .build();
                    if (calendar.getTimeRanges() != null) {
                        calendar.getTimeRanges().add(range);
                    }
                    rangeRepository.save(range);
                    created++;
                }
                target.writtenDays++;
            }
        }

        int createdDays = targets.stream().mapToInt(t -> t.writtenDays).sum();
        List<StaffSlotSkipView> skipped = new ArrayList<>();
        for (Target target : targets) {
            for (String day : target.skippedDays) {
                skipped.add(StaffSlotSkipView.builder()
                        .day(day)
                        .name(target.name)
                        .packageId(target.packageId)
                        .individualServiceId(target.individualServiceId)
                        .repairInspection(StaffSlotTargets.REPAIR_INSPECTION.equals(target.kind))
                        .build());
            }
        }
        return StaffSlotGenerateResponse.builder()
                .created(created)
                .createdDays(createdDays)
                .skipped(skipped)
                .message(message(lang, targets))
                .build();
    }

    /**
     * tr: Şubenin bir günündeki staff slotları.
     * en: Staff slots of a branch for one day.
     */
    @Transactional(readOnly = true)
    public StaffSlotDayResponse day(Long userId, boolean mustChangePassword, Long branchId, String dayRaw,
                                    String acceptLanguage) {
        String lang = lang(acceptLanguage);
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, lang);
        Branch branch = resolveBranch(staff, branchId, lang);
        if (dayRaw == null || dayRaw.isBlank()) {
            throw new MissingFieldException(sentence(lang, "Tarix seçin.", "Select a date.", "Выберите дату."));
        }
        LocalDate day;
        try {
            day = LocalDate.parse(dayRaw.trim());
        } catch (Exception ex) {
            throw new MissingFieldException(sentence(lang, "Tarix seçin.", "Select a date.", "Выберите дату."));
        }
        List<Range> ranges = new ArrayList<>();
        for (Calendar calendar : calendarRepository.findByBranchIdAndDayBetween(branch.getId(), day, day)) {
            if (calendar.getTimeRanges() == null) {
                continue;
            }
            for (Range range : calendar.getTimeRanges()) {
                if (range.getSlotTarget() != null && !range.getSlotTarget().isBlank()) {
                    ranges.add(range);
                }
            }
        }
        ranges.sort(Comparator.comparing(Range::getStart, Comparator.nullsLast(Comparator.naturalOrder())));
        List<StaffSlotView> slots = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Range range : ranges) {
            if (range.getRangeId() != null && !seen.add(range.getRangeId())) {
                continue;
            }
            int booked = 0;
            if (range.getRangeId() != null) {
                booked = (int) bookingRepository.countByRange_RangeIdAndStatusIn(
                        range.getRangeId(), BookingStatus.occupyingCapacity());
            }
            slots.add(StaffSlotView.builder()
                    .slotId(range.getRangeId())
                    .day(day.toString())
                    .start(clock(range.getStart()))
                    .end(clock(range.getEnd()))
                    .capacity(range.getWorkerCount())
                    .bookedCount(booked)
                    .bookingMode(range.getBookingMode())
                    .status(range.getStatus())
                    .packageId(range.getCarePackage() == null ? null : range.getCarePackage().getId())
                    .individualServiceId(range.getIndividualService() == null ? null : range.getIndividualService().getId())
                    .repairInspection(StaffSlotTargets.REPAIR_INSPECTION.equals(range.getSlotTarget()))
                    .build());
        }
        return StaffSlotDayResponse.builder()
                .branchId(branch.getId())
                .day(day.toString())
                .slots(slots)
                .build();
    }

    private List<Target> resolveTargets(Branch branch, StaffSlotGenerateRequest request, String lang) {
        List<String> problems = new ArrayList<>();
        List<Target> targets = new ArrayList<>();
        Set<Long> packageIds = ids(request.getPackageIds());
        Set<Long> serviceIds = ids(request.getIndividualServiceIds());
        for (Long packageId : packageIds) {
            BranchCarePackage pkg = carePackageRepository.findById(packageId).orElse(null);
            if (pkg == null || pkg.getBranch() == null || !branch.getId().equals(pkg.getBranch().getId())) {
                problems.add(sentence(lang, "Seçilmiş paket bu şubədə yoxdur.",
                        "The selected package is not on this branch.", "Выбранный пакет не принадлежит этому филиалу."));
                continue;
            }
            if (!Boolean.TRUE.equals(pkg.getActive())) {
                problems.add(closed(lang, pkg.getName()));
                continue;
            }
            targets.add(new Target(StaffSlotTargets.PACKAGE, packageId, null, pkg, null, pkg.getName()));
        }
        for (Long serviceId : serviceIds) {
            IndividualService catalog = individualServiceRepository.findById(serviceId).orElse(null);
            String name = catalog == null ? null : title(catalog.getTitleJson(), lang, catalog.getCode());
            if (catalog == null || !Boolean.TRUE.equals(catalog.getActive())) {
                problems.add(name == null
                        ? sentence(lang, "Seçilmiş xidmət bu şubədə yoxdur.",
                        "The selected service is not on this branch.", "Выбранная услуга не принадлежит этому филиалу.")
                        : closed(lang, name));
                continue;
            }
            BranchIndividualService row = branchIndividualServiceRepository
                    .findByBranch_IdAndIndividualService_Id(branch.getId(), serviceId)
                    .orElse(null);
            if (row == null || !Boolean.TRUE.equals(row.getActive())) {
                problems.add(closed(lang, name));
                continue;
            }
            targets.add(new Target(StaffSlotTargets.INDIVIDUAL, null, serviceId, null, catalog, name));
        }
        if (Boolean.TRUE.equals(request.getRepairInspection())) {
            String repairName = sentence(lang, "Təmir və yoxlama", "Repair and inspection", "Ремонт и осмотр");
            ServiceCategory category = categoryRepository.findByCode(StaffSlotTargets.REPAIR_INSPECTION).orElse(null);
            if (category != null) {
                Map<String, String> titles = json.read(category.getTitleJson());
                String titled = titles.get(lang);
                if (titled == null || titled.isBlank()) {
                    titled = titles.get("az");
                }
                if (titled != null && !titled.isBlank()) {
                    repairName = titled;
                }
            }
            if (!repairEnabled(category, branch.getId())) {
                problems.add(closed(lang, repairName));
            } else {
                targets.add(new Target(StaffSlotTargets.REPAIR_INSPECTION, null, null, null, null, repairName));
            }
        }
        if (!problems.isEmpty()) {
            throw new ConflictException(String.join(" ", distinct(problems)));
        }
        if (targets.isEmpty()) {
            throw new MissingFieldException(sentence(lang, "Ən azı bir xidmət növü seçin.",
                    "Select at least one service.", "Выберите хотя бы одну услугу."));
        }
        return targets;
    }

    private boolean repairEnabled(ServiceCategory category, Long branchId) {
        if (category == null || !Boolean.TRUE.equals(category.getActive())) {
            return false;
        }
        if (!Boolean.TRUE.equals(category.getToggleable())) {
            return true;
        }
        return branchCategoryRepository.findByBranch_IdAndCategory_Id(branchId, category.getId())
                .map(row -> Boolean.TRUE.equals(row.getActive()))
                .orElse(false);
    }

    private boolean occupied(Long branchId, LocalDate day, Target target) {
        if (StaffSlotTargets.PACKAGE.equals(target.kind)) {
            return rangeRepository.countCarePackageOnDay(branchId, day, target.packageId) > 0;
        }
        if (StaffSlotTargets.INDIVIDUAL.equals(target.kind)) {
            return rangeRepository.countIndividualOnDay(branchId, day, target.individualServiceId) > 0;
        }
        return rangeRepository.countTargetOnDay(branchId, day, StaffSlotTargets.REPAIR_INSPECTION) > 0;
    }

    private Calendar calendarFor(Branch branch, LocalDate day, Hours hours, int durationMin) {
        Calendar found = calendarRepository.findByDayAndServiceCategoryAndBranch(
                day, StaffSlotTargets.CALENDAR_CATEGORY, branch);
        if (found != null) {
            return found;
        }
        Calendar calendar = Calendar.builder()
                .day(day)
                .start(ZonedDateTime.of(day, hours.start, StaffSlotWindows.ZONE).toOffsetDateTime())
                .end(ZonedDateTime.of(day, hours.end, StaffSlotWindows.ZONE).toOffsetDateTime())
                .branch(branch)
                .rangeMinutes(durationMin)
                .status(CalendarStatus.ACTIVE.name())
                .serviceCategory(StaffSlotTargets.CALENDAR_CATEGORY)
                .timeRanges(new ArrayList<>())
                .build();
        return calendarRepository.save(calendar);
    }

    private Hours hours(StaffSlotDayRule rule, String lang, boolean weekend) {
        if (rule == null || rule.getStart() == null || rule.getEnd() == null || rule.getCapacity() == null) {
            throw new MissingFieldException(weekend
                    ? sentence(lang, "Həftəsonu saatlarını seçin.", "Select weekend hours.", "Выберите часы выходных.")
                    : sentence(lang, "İş saatlarını seçin.", "Select working hours.", "Выберите рабочие часы."));
        }
        if (!rule.getStart().isBefore(rule.getEnd())) {
            throw new MissingFieldException(weekend
                    ? sentence(lang, "Həftəsonu bitmə saatı başlama saatından sonra olmalıdır.",
                    "Weekend end must be after the start.", "Конец выходного должен быть позже начала.")
                    : sentence(lang, "İş bitmə saatı başlama saatından sonra olmalıdır.",
                    "Work end must be after the start.", "Конец рабочего дня должен быть позже начала."));
        }
        if (rule.getCapacity() < 1 || rule.getCapacity() > CAPACITY_MAX) {
            throw new MissingFieldException(sentence(lang, "Eyni vaxtda yer sayı 1 ilə 50 arasında olmalıdır.",
                    "Places at the same time must be between 1 and 50.", "Число мест одновременно должно быть от 1 до 50."));
        }
        String mode = BookingMode.normalizeOrDefault(rule.getBookingMode());
        if (rule.getBookingMode() == null || rule.getBookingMode().isBlank() || mode == null) {
            throw new MissingFieldException(sentence(lang, "Rezervasiya qaydası ani və ya təsdiqli olmalıdır.",
                    "The booking rule must be instant or approval.", "Правило записи должно быть мгновенным или с подтверждением."));
        }
        boolean breakTouched = rule.getBreakStart() != null || rule.getBreakEnd() != null;
        if (breakTouched && (rule.getBreakStart() == null || rule.getBreakEnd() == null
                || !rule.getBreakStart().isBefore(rule.getBreakEnd()))) {
            throw new MissingFieldException(weekend
                    ? sentence(lang, "Həftəsonu nahar fasiləsinin bitmə saatı başlama saatından sonra olmalıdır.",
                    "Weekend lunch end must be after the start.", "Конец обеда в выходной должен быть позже начала.")
                    : sentence(lang, "Nahar fasiləsinin bitmə saatı başlama saatından sonra olmalıdır.",
                    "Lunch end must be after the start.", "Конец обеда должен быть позже начала."));
        }
        return new Hours(rule.getStart(), rule.getEnd(), rule.getCapacity(), mode,
                breakTouched ? rule.getBreakStart() : null,
                breakTouched ? rule.getBreakEnd() : null);
    }

    private static Hours ruleFor(LocalDate day, Hours weekday, Hours weekend, boolean saturday, boolean sunday) {
        DayOfWeek dow = day.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) {
            return saturday ? weekend : null;
        }
        if (dow == DayOfWeek.SUNDAY) {
            return sunday ? weekend : null;
        }
        return weekday;
    }

    private Branch resolveBranch(BookingStaff staff, Long branchId, String lang) {
        if (branchId != null) {
            return bookingStaffAccess.requireWritableBranch(staff, branchId, lang);
        }
        if (BookingStaffRole.BRANCH_ADMIN.name().equals(staff.getRole())) {
            if (staff.getBranch() == null) {
                throw new ForbiddenException(sentence(lang, "Şöbə tapılmadı.", "Branch was not found.", "Филиал не найден."));
            }
            return staff.getBranch();
        }
        List<Branch> branches = branchRepository.findByPartnerOrderByIdAsc(staff.getPartner());
        if (branches.isEmpty()) {
            throw new ResourceNotFoundException(sentence(lang, "Şöbə tapılmadı.", "Branch was not found.", "Филиал не найден."));
        }
        return branches.get(0);
    }

    private String title(String titleJson, String lang, String fallback) {
        Map<String, String> titles = json.read(titleJson);
        String named = titles.get(lang);
        if (named == null || named.isBlank()) {
            named = titles.get("az");
        }
        if (named == null || named.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return named;
    }

    private String message(String lang, List<Target> targets) {
        List<String> parts = new ArrayList<>();
        for (Target target : targets) {
            if (target.writtenDays == 0 && target.skippedDays.isEmpty()) {
                continue;
            }
            parts.add(line(lang, target));
        }
        if (parts.isEmpty()) {
            return sentence(lang, "Açıq saat qalmadı.", "No open hours left.", "Свободных часов не осталось.");
        }
        return String.join(" ", parts);
    }

    private String line(String lang, Target target) {
        String days = String.join(", ", target.skippedDays);
        if (!target.skippedDays.isEmpty() && target.writtenDays > 0) {
            return sentence(lang,
                    target.name + ": " + days + " atlandı, " + target.writtenDays + " gün yazıldı.",
                    target.name + ": " + days + " skipped, " + target.writtenDays + " days written.",
                    target.name + ": " + days + " пропущен, записано " + target.writtenDays + " дн.");
        }
        if (!target.skippedDays.isEmpty()) {
            return sentence(lang,
                    target.name + ": " + days + " atlandı.",
                    target.name + ": " + days + " skipped.",
                    target.name + ": " + days + " пропущен.");
        }
        return sentence(lang,
                target.name + ": " + target.writtenDays + " gün yazıldı.",
                target.name + ": " + target.writtenDays + " days written.",
                target.name + ": записано " + target.writtenDays + " дн.");
    }

    private static String closed(String lang, String name) {
        return sentence(lang, name + " bağlıdır.", name + " is off.", name + " выключен.");
    }

    private static String clock(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZoneSameInstant(StaffSlotWindows.ZONE).toLocalTime().format(CLOCK);
    }

    private static Set<Long> ids(List<Long> raw) {
        Set<Long> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        for (Long id : raw) {
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    private static List<String> distinct(List<String> lines) {
        return new ArrayList<>(new LinkedHashSet<>(lines));
    }

    static String lang(String header) {
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

    private static final class Target {
        final String kind;
        final Long packageId;
        final Long individualServiceId;
        final BranchCarePackage carePackage;
        final IndividualService individualService;
        final String name;
        final List<String> skippedDays = new ArrayList<>();
        int writtenDays;

        Target(String kind, Long packageId, Long individualServiceId, BranchCarePackage carePackage,
               IndividualService individualService, String name) {
            this.kind = kind;
            this.packageId = packageId;
            this.individualServiceId = individualServiceId;
            this.carePackage = carePackage;
            this.individualService = individualService;
            this.name = name;
        }
    }

    private record Hours(LocalTime start, LocalTime end, int capacity, String mode,
                         LocalTime breakStart, LocalTime breakEnd) {
    }
}
