package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingInboxResponse;
import com.carland.carland_service.dto.booking.CarResponseForSlotPanel;
import com.carland.carland_service.dto.booking.BookingRejectRequest;
import com.carland.carland_service.dto.booking.BookingServiceLineView;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.StaffNotesResponse;
import com.carland.carland_service.dto.booking.StaffCompletionDraftView;
import com.carland.carland_service.dto.request.StaffCompleteRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingInspection;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.BookingStaffNote;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCompletion;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.util.PlateNumbers;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BrandRepository;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingInspectionRepository;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CarSpec;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.RangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
@Slf4j
public class StaffBookingService {

    static final String DEFAULT_TZ = "Asia/Baku";
    static final String BOARD = "board";
    private static final int HEAD_PAGE = 20;
    /** tr: Sayacı ayrı sorgudan almıyoruz. Listeyi dolduran select'in kendisini sayıyoruz. */
    private static final int HEAD_LOAD = 5000;
    private static final LocalDate DAY_MIN = LocalDate.of(1970, 1, 1);
    private static final LocalDate DAY_MAX = LocalDate.of(9999, 12, 31);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CREATED = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final List<String> PENDING_STATUSES = List.of(BookingStatus.PENDING.apiValue());
    private static final List<String> CONFIRMED_STATUSES = List.of(
            BookingStatus.CONFIRMED.apiValue(), BookingStatus.AUTO_ACCEPTED.apiValue());
    private static final List<String> COMPLETED_STATUSES = List.of(BookingStatus.COMPLETED.apiValue());
    private static final List<String> REJECTED_STATUSES = List.of(
            BookingStatus.REJECTED.apiValue(), BookingStatus.CANCELLED.apiValue(), "canceled",
            BookingStatus.NO_SHOW.apiValue());
    private static final List<String> BOARD_STATUSES = List.of(
            BookingStatus.PENDING.apiValue(),
            BookingStatus.CONFIRMED.apiValue(),
            BookingStatus.AUTO_ACCEPTED.apiValue(),
            BookingStatus.REJECTED.apiValue(),
            BookingStatus.CANCELLED.apiValue(),
            "canceled",
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
    private final ColorRepository colorRepository;
    private final BookingStaffNoteService bookingStaffNoteService;
    private final BrandRepository brandRepository;
    private final BookingPushService bookingPushService;
    private final BookingCompletionService bookingCompletionService;
    private final StaffBookingLiveService staffBookingLiveService;

    @Transactional(readOnly = true)
    public StaffNotesResponse notes(Long userId, boolean mustChangePassword, String kind, String acceptLanguage) {
        requireStaff(userId, mustChangePassword, acceptLanguage);
        return bookingStaffNoteService.list(kind, acceptLanguage);
    }

    @Transactional(readOnly = true)
    public BookingInboxResponse inbox(Long userId, boolean mustChangePassword, String status, Long branchId,
                                      Integer page, Integer pageSize, String timezone, String acceptLanguage) {
        return inbox(userId, mustChangePassword, status, branchId, page, pageSize, timezone, acceptLanguage,
                null, null, null, null, null, null, null);
    }

    /**
     * tr: Şube ve partner admin eski tahta: tek istek, en fazla 200 satır, sayaçlar bu kümeden.
     *     Head admin ayrı: aktif şubeler, sayfa 20, randevu gününe göre, sayaç gerçek toplam.
     * en: Branch and partner admin keep the old board: one request, at most 200 rows.
     *     Head admin is separate: active branches, pages of 20, appointment day, real totals.
     */
    @Transactional(readOnly = true)
    public BookingInboxResponse inbox(Long userId, boolean mustChangePassword, String status, Long branchId,
                                      Integer page, Integer pageSize, String timezone, String acceptLanguage,
                                      String vin, String plate, String firstName, String lastName, String phone,
                                      String dateFrom, String dateTo) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        if (branchId == null && BookingStaffRole.HEAD_ADMIN.name().equals(staff.getRole())) {
            return headInbox(status, page, timezone, acceptLanguage, vin, plate, firstName, lastName, phone,
                    dateFrom, dateTo);
        }
        String wanted = status == null || status.isBlank() ? BookingStatus.PENDING.apiValue() : status.trim().toLowerCase(Locale.ROOT);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 200 : Math.min(pageSize, 200);
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        boolean board = BOARD.equals(wanted) || "all".equals(wanted);
        Page<Booking> result;
        Long scopeBranch = null;
        Long scopePartner = null;
        if (branchId != null) {
            bookingStaffAccess.requireWritableBranch(staff, branchId, acceptLanguage);
            scopeBranch = branchId;
            result = board
                    ? bookingRepository.findByBranch_IdAndStatusInOrderByCreatedAtDesc(branchId, BOARD_STATUSES, pageable)
                    : bookingRepository.findByBranch_IdAndStatusInOrderByCreatedAtDesc(branchId, statusesOf(wanted), pageable);
        } else if (BookingStaffRole.BRANCH_ADMIN.name().equals(staff.getRole())) {
            if (staff.getBranch() == null) {
                throw new ForbiddenException("branch required");
            }
            scopeBranch = staff.getBranch().getId();
            result = board
                    ? bookingRepository.findByBranch_IdAndStatusInOrderByCreatedAtDesc(scopeBranch, BOARD_STATUSES, pageable)
                    : bookingRepository.findByBranch_IdAndStatusInOrderByCreatedAtDesc(scopeBranch, statusesOf(wanted), pageable);
        } else {
            scopePartner = staff.getPartner().getId();
            result = board
                    ? bookingRepository.findByBranch_Partner_IdAndStatusInOrderByCreatedAtDesc(scopePartner, BOARD_STATUSES, pageable)
                    : bookingRepository.findByBranch_Partner_IdAndStatusInOrderByCreatedAtDesc(scopePartner, statusesOf(wanted), pageable);
        }
        boolean newestDay = !board && newestDayFirst(wanted);
        return respond(result.getContent(), safePage, safeSize, timezone, acceptLanguage, newestDay,
                countBranch(scopeBranch, scopePartner, PENDING_STATUSES),
                countBranch(scopeBranch, scopePartner, CONFIRMED_STATUSES),
                countBranch(scopeBranch, scopePartner, COMPLETED_STATUSES),
                countBranch(scopeBranch, scopePartner, REJECTED_STATUSES),
                1);
    }

    private BookingInboxResponse headInbox(String status, Integer page, String timezone, String acceptLanguage,
                                           String vin, String plate, String firstName, String lastName, String phone,
                                           String dateFrom, String dateTo) {
        String wanted = status == null || status.isBlank() ? "pending" : status.trim().toLowerCase(Locale.ROOT);
        List<String> statuses = statusesOf(wanted);
        boolean newest = newestDayFirst(wanted);
        LocalDate from = dayOr(dateFrom, DAY_MIN);
        LocalDate to = dayOr(dateTo, DAY_MAX);
        String vinNeedle = textNeedle(vin);
        String plateNeedle = plateNeedle(plate);
        String firstNeedle = textNeedle(firstName);
        String lastNeedle = textNeedle(lastName);
        String phoneNeedle = phoneNeedle(phone);
        List<Booking> all = bookingRepository.findHeadByDayAsc(
                BOARD_STATUSES, from, to, vinNeedle, plateNeedle, firstNeedle, lastNeedle, phoneNeedle,
                PageRequest.of(0, HEAD_LOAD)).getContent();
        long pending = 0;
        long confirmed = 0;
        long completed = 0;
        long rejected = 0;
        List<Booking> tab = new ArrayList<>();
        boolean everyStatus = "all".equals(wanted) || BOARD.equals(wanted);
        for (Booking booking : all) {
            String rowStatus = booking.getStatus() == null ? "" : booking.getStatus().trim().toLowerCase(Locale.ROOT);
            switch (rowStatus) {
                case "pending" -> pending++;
                case "confirmed", "auto_accepted", "accepted" -> confirmed++;
                case "completed" -> completed++;
                case "rejected", "cancelled", "canceled", "no_show" -> rejected++;
                default -> {
                }
            }
            if (everyStatus || statuses.contains(rowStatus)) {
                tab.add(booking);
            }
        }
        tab.sort((left, right) -> compareBooking(left, right, newest));
        int totalForTab = tab.size();
        int totalPages = totalForTab == 0 ? 1 : (totalForTab + HEAD_PAGE - 1) / HEAD_PAGE;
        int requested = page == null || page < 1 ? 1 : page;
        int safePage = Math.min(requested, totalPages);
        int fromIndex = Math.min((safePage - 1) * HEAD_PAGE, totalForTab);
        int toIndex = Math.min(fromIndex + HEAD_PAGE, totalForTab);
        List<Booking> pageRows = new ArrayList<>(tab.subList(fromIndex, toIndex));
        return respond(pageRows, safePage, HEAD_PAGE, timezone, acceptLanguage, newest,
                pending, confirmed, completed, rejected, totalPages);
    }

    private static int compareBooking(Booking left, Booking right, boolean newest) {
        String leftDay = appointmentDay(left);
        String rightDay = appointmentDay(right);
        int day = compareDay(leftDay, rightDay);
        if (newest && leftDay != null && rightDay != null) {
            day = -day;
        }
        if (day != 0) {
            return day;
        }
        OffsetDateTime leftStart = left.getRange() == null ? null : left.getRange().getStart();
        OffsetDateTime rightStart = right.getRange() == null ? null : right.getRange().getStart();
        if (leftStart == null && rightStart == null) {
            return Long.compare(idOf(left), idOf(right));
        }
        if (leftStart == null) {
            return 1;
        }
        if (rightStart == null) {
            return -1;
        }
        int start = leftStart.compareTo(rightStart);
        if (start != 0) {
            return start;
        }
        return Long.compare(idOf(left), idOf(right));
    }

    private static String appointmentDay(Booking booking) {
        if (booking.getRange() == null || booking.getRange().getCalendar() == null
                || booking.getRange().getCalendar().getDay() == null) {
            return null;
        }
        return booking.getRange().getCalendar().getDay().toString();
    }

    private static long idOf(Booking booking) {
        return booking.getId() == null ? Long.MAX_VALUE : booking.getId();
    }

    private long countBranch(Long branchId, Long partnerId, List<String> statuses) {
        if (branchId != null) {
            return bookingRepository.countBranch(branchId, statuses);
        }
        return bookingRepository.countPartner(partnerId, statuses);
    }

    private BookingInboxResponse respond(List<Booking> rows, int page, int pageSize, String timezone,
                                         String acceptLanguage, boolean newestDay,
                                         long pending, long confirmed, long completed, long rejected,
                                         int totalPages) {
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
        items.sort((left, right) -> byAppointment(left, right, newestDay));
        long total = pending + confirmed + completed + rejected;
        return BookingInboxResponse.builder()
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .totalPages(totalPages)
                .pending(pending)
                .confirmed(confirmed)
                .completed(completed)
                .rejected(rejected)
                .items(items)
                .build();
    }

    private static List<String> statusesOf(String wanted) {
        return switch (wanted) {
            case "confirmed" -> CONFIRMED_STATUSES;
            case "completed" -> COMPLETED_STATUSES;
            case "rejected" -> REJECTED_STATUSES;
            case "all", "board" -> BOARD_STATUSES;
            case "pending" -> PENDING_STATUSES;
            default -> List.of(wanted);
        };
    }

    private static boolean newestDayFirst(String wanted) {
        return "completed".equals(wanted) || "rejected".equals(wanted);
    }

    private static LocalDate dayOr(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private static String textNeedle(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String plateNeedle(String raw) {
        String key = CarSpec.plateNeedle(raw);
        return key == null ? "" : key;
    }

    private static String phoneNeedle(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.replace("-", "").replace(" ", "").trim().toLowerCase(Locale.ROOT);
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
                BookingStaffNote.REJECT, request, acceptLanguage);
        return decide(userId, mustChangePassword, bookingId, BookingStatus.REJECTED.apiValue(),
                applied, timezone, acceptLanguage);
    }

    /**
     * tr: "Xidməti tamamla" pəncərəsinin taslağı. Yalnız təsdiqlənmiş bron üçün.
     * en: Draft for the "Complete service" dialog. Confirmed bookings only.
     */
    @Transactional(readOnly = true)
    public StaffCompletionDraftView completionDraft(Long userId, boolean mustChangePassword, Long bookingId,
                                                    String acceptLanguage) {
        Booking booking = requireConfirmed(userId, mustChangePassword, bookingId, acceptLanguage);
        return bookingCompletionService.draft(booking, acceptLanguage);
    }

    /**
     * tr: Təsdiqlənmiş booku completed edir. detailed şöbədə gövdə gəlibsə forma eyni tranzaksiyada
     *     rəqəmsal tarixçəyə (visits) yazılır. webhook şöbəsində gövdə nəzərə alınmır — xidmət sətirləri webhookdan gəlir.
     * en: Marks a confirmed booking completed. On a detailed branch a body is written to the digital history
     *     (visits) in the same transaction. On a webhook branch the body is ignored — lines arrive from the webhook.
     */
    @Transactional
    public BookingView complete(Long userId, boolean mustChangePassword, Long bookingId, StaffCompleteRequest request,
                                String timezone, String acceptLanguage) {
        if (request != null) {
            Booking booking = requireConfirmed(userId, mustChangePassword, bookingId, acceptLanguage);
            String mode = BranchCompletion.shown(booking.getBranch().getCompletionMode());
            if (BranchCompletion.DETAILED.equals(mode)) {
                bookingCompletionService.write(booking, request, timezone, acceptLanguage);
            }
        }
        return decide(userId, mustChangePassword, bookingId, BookingStatus.COMPLETED.apiValue(),
                null, timezone, acceptLanguage);
    }

    private Booking requireConfirmed(Long userId, boolean mustChangePassword, Long bookingId, String acceptLanguage) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        bookingStaffAccess.requireWritableBranch(staff, booking.getBranch().getId(), acceptLanguage);
        String current = booking.getStatus() == null ? "" : booking.getStatus().toLowerCase();
        if (!BookingStatus.CONFIRMED.apiValue().equals(current)
                && !BookingStatus.AUTO_ACCEPTED.apiValue().equals(current)) {
            throw new ConflictException("booking is not confirmed");
        }
        return booking;
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

    /**
     * tr: Partner siyahısından gizlədir. Status, yer və müştəri tərəfi eyni qalır. Push yoxdur.
     * en: Hides the row from the partner list. Status, the place and the customer side stay the same. No push.
     */
    @Transactional
    public BookingView hide(Long userId, boolean mustChangePassword, Long bookingId, String timezone,
                            String acceptLanguage) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        bookingStaffAccess.requireWritableBranch(staff, booking.getBranch().getId(), acceptLanguage);
        booking.setStaffHidden(true);
        bookingRepository.save(booking);
        staffBookingLiveService.refreshAfterCommit(
                booking.getBranch() == null ? null : booking.getBranch().getId());
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

    /**
     * tr: Admin paneli, aktif şubedeki rezervi şube personeliyle aynı kuralla ilerletir.
     * en: The admin panel advances a booking on an active branch with the same rule as branch staff.
     */
    @Transactional
    public BookingView adminAccept(Long bookingId, String timezone, String acceptLanguage) {
        return transition(operable(bookingId, acceptLanguage), BookingStatus.CONFIRMED.apiValue(),
                null, timezone, acceptLanguage);
    }

    @Transactional
    public BookingView adminReject(Long bookingId, BookingRejectRequest request, String timezone,
                                   String acceptLanguage) {
        BookingStaffNoteService.Applied applied = bookingStaffNoteService.apply(
                BookingStaffNote.REJECT, request, acceptLanguage);
        return transition(operable(bookingId, acceptLanguage), BookingStatus.REJECTED.apiValue(),
                applied, timezone, acceptLanguage);
    }

    @Transactional
    public BookingView adminNoShow(Long bookingId, BookingRejectRequest request, String timezone,
                                   String acceptLanguage) {
        BookingStaffNoteService.Applied applied = bookingStaffNoteService.apply(
                BookingStaffNote.NO_SHOW, request, acceptLanguage);
        return transition(operable(bookingId, acceptLanguage), BookingStatus.NO_SHOW.apiValue(),
                applied, timezone, acceptLanguage);
    }

    @Transactional(readOnly = true)
    public StaffCompletionDraftView adminDraft(Long bookingId, String acceptLanguage) {
        return bookingCompletionService.draft(operableConfirmed(bookingId, acceptLanguage), acceptLanguage);
    }

    @Transactional
    public BookingView adminComplete(Long bookingId, StaffCompleteRequest request, String timezone,
                                     String acceptLanguage) {
        Booking booking = operableConfirmed(bookingId, acceptLanguage);
        String mode = BranchCompletion.shown(booking.getBranch().getCompletionMode());
        if (BranchCompletion.DETAILED.equals(mode)) {
            bookingCompletionService.write(booking,
                    request == null ? new StaffCompleteRequest() : request, timezone, acceptLanguage);
        }
        return transition(booking, BookingStatus.COMPLETED.apiValue(), null, timezone, acceptLanguage);
    }

    @Transactional
    public BookingView adminHide(Long bookingId, String timezone, String acceptLanguage) {
        Booking booking = operable(bookingId, acceptLanguage);
        booking.setStaffHidden(true);
        bookingRepository.save(booking);
        staffBookingLiveService.refreshAfterCommit(
                booking.getBranch() == null ? null : booking.getBranch().getId());
        return viewOf(booking, timezone, acceptLanguage);
    }

    private BookingView decide(Long userId, boolean mustChangePassword, Long bookingId, String nextStatus,
                               BookingStaffNoteService.Applied applied, String timezone, String acceptLanguage) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        bookingStaffAccess.requireWritableBranch(staff, booking.getBranch().getId(), acceptLanguage);
        return transition(booking, nextStatus, applied, timezone, acceptLanguage);
    }

    private BookingView transition(Booking booking, String nextStatus, BookingStaffNoteService.Applied applied,
                                   String timezone, String acceptLanguage) {
        Long bookingId = booking.getId();
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
            notifyCustomer(booking, true);
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
            if (BookingStatus.REJECTED.apiValue().equals(nextStatus)) {
                notifyCustomer(booking, false);
            } else if (BookingStatus.NO_SHOW.apiValue().equals(nextStatus)) {
                notifyNoShow(booking);
            } else if (BookingStatus.COMPLETED.apiValue().equals(nextStatus)) {
                notifyCompleted(booking);
            }
        }
        return viewOf(booking, timezone, acceptLanguage);
    }

    private Booking operable(Long bookingId, String acceptLanguage) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        Branch branch = booking.getBranch();
        boolean open = branch != null
                && !Boolean.FALSE.equals(branch.getActive())
                && branch.getPartner() != null
                && !Boolean.FALSE.equals(branch.getPartner().getActive());
        if (!open) {
            throw new ForbiddenException(MessagesLangValues.COOPERATION_SUSPENDED.getMessageByLang(acceptLanguage));
        }
        return booking;
    }

    private Booking operableConfirmed(Long bookingId, String acceptLanguage) {
        Booking booking = operable(bookingId, acceptLanguage);
        String current = booking.getStatus() == null ? "" : booking.getStatus().toLowerCase();
        if (!BookingStatus.CONFIRMED.apiValue().equals(current)
                && !BookingStatus.AUTO_ACCEPTED.apiValue().equals(current)) {
            throw new ConflictException("booking is not confirmed");
        }
        return booking;
    }

    /** tr: Admin tahtası satırı. Saat Asia/Baku, dil az. en: Admin board row. Clock is Asia/Baku, language az. */
    public BookingView viewForAdmin(Booking booking) {
        return viewOf(booking, "Asia/Baku", "az");
    }

    private BookingView viewOf(Booking booking, String timezone, String acceptLanguage) {
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone.trim();
        String lang = BookingMineService.langOf(acceptLanguage);
        Long bookingId = booking.getId();
        List<String> keys = bookingItemRepository.findByBooking_IdOrderByIdAsc(bookingId).stream()
                .map(BookingItem::getServiceKey)
                .toList();
        List<BookingIndividualLine> lines = individualLineRepository.findByBooking_IdOrderByIdAsc(bookingId);
        java.util.Optional<BookingInspection> found = inspectionRepository.findByBooking_Id(bookingId);
        BookingInspection inspection = found == null ? null : found.orElse(null);
        return toView(booking, keys, tz, lang, lines == null ? List.of() : lines, inspection, new HashMap<>());
    }

    /**
     * tr: Push xətası qəbul və ya rəddi geri almır.
     * en: A push failure does not undo accept or reject.
     */
    private void notifyCustomer(Booking booking, boolean accepted) {
        try {
            if (accepted) {
                bookingPushService.accepted(booking);
            } else {
                bookingPushService.rejected(booking);
            }
        } catch (Exception ex) {
            log.warn("booking push skipped id={} accepted={}", booking == null ? null : booking.getId(), accepted);
        }
    }

    private void notifyNoShow(Booking booking) {
        try {
            bookingPushService.noShow(booking);
        } catch (Exception ex) {
            log.warn("booking no-show push skipped id={}", booking == null ? null : booking.getId());
        }
    }

    private void notifyCompleted(Booking booking) {
        try {
            bookingPushService.completed(booking);
        } catch (Exception ex) {
            log.warn("booking complete push skipped id={}", booking == null ? null : booking.getId());
        }
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
                .plateNumber(car == null ? null : PlateNumbers.format(car.getPlateNumber()))
                .carBrand(car == null ? null : car.getBrand())
                .carModel(car == null ? null : car.getModel())
                .car(carPanel(car, brandIds, lang))
                .cancelReasonCode(booking.getCancelReasonCode())
                .cancelNote(BookingSystemNotes.resolve(booking.getCancelReasonCode(), booking.getCancelNote(), lang))
                .serviceKeys(keys)
                .packageName(booking.getPackageName())
                .packagePrice(booking.getPackagePrice())
                .individualServices(individualServices)
                .inspection(BookingSelectionViews.inspection(inspection))
                .branchName(booking.getBranch() == null ? null : booking.getBranch().getName())
                .completionMode(booking.getBranch() == null
                        ? null : BranchCompletion.shown(booking.getBranch().getCompletionMode()))
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
                .createdAt(createdStamp(booking.getCreatedAt()))
                .build();
    }

    private CarResponseForSlotPanel carPanel(Car car, Map<String, Long> brandIds, String lang) {
        if (car == null) {
            return null;
        }
        return CarResponseForSlotPanel.builder()
                .carId(car.getCarId())
                .brandId(brandId(car.getBrand(), brandIds))
                .brand(car.getBrand())
                .model(car.getModel())
                .plateNumber(PlateNumbers.format(car.getPlateNumber()))
                .vin(car.getVin())
                .bodyType(car.getBodyType())
                .engineType(car.getEngineType())
                .modelYear(car.getModelYear())
                .engineVolume(car.getEngineVolume())
                .mileage(car.getMileage())
                .color(colorName(car, lang))
                .build();
    }

    private String colorName(Car car, String lang) {
        if (car.getColorId() == null) {
            String text = car.getIddaColor();
            return text == null || text.isBlank() ? null : text;
        }
        Color color = colorRepository.findByColorId(car.getColorId());
        if (color == null) {
            return null;
        }
        String name = color.nameForLang(lang);
        if (name == null || name.isBlank()) {
            return null;
        }
        return name;
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

    /**
     * tr: Gün, sonra range başlangıcı. newestDay tamamlanan ve reddedilen sekmesi içindir.
     * en: Day, then the range start. newestDay is the completed and rejected tabs.
     */
    private static int byAppointment(BookingView left, BookingView right, boolean newestDay) {
        int day = compareDay(left.getDay(), right.getDay());
        if (newestDay && left.getDay() != null && right.getDay() != null) {
            day = -day;
        }
        if (day != 0) {
            return day;
        }
        int start = compareText(left.getStart(), right.getStart());
        if (start != 0) {
            return start;
        }
        long leftId = left.getBookingId() == null ? Long.MAX_VALUE : left.getBookingId();
        long rightId = right.getBookingId() == null ? Long.MAX_VALUE : right.getBookingId();
        return Long.compare(leftId, rightId);
    }

    private static int compareDay(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static int compareText(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static String createdStamp(OffsetDateTime time) {
        return time == null ? null : CREATED.format(time);
    }

    private static String clock(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).toLocalTime().format(CLOCK);
    }
}
