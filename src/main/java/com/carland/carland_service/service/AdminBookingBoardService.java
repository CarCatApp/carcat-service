package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.AdminBookingBoardPage;
import com.carland.carland_service.dto.booking.AdminBookingMessageView;
import com.carland.carland_service.dto.booking.BookingRejectRequest;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.dto.booking.StaffCarHistoryResponse;
import com.carland.carland_service.dto.booking.StaffCompletionDraftView;
import com.carland.carland_service.dto.booking.StaffNotesResponse;
import com.carland.carland_service.dto.request.StaffCompleteRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingMessage;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingMessageRepository;
import com.carland.carland_service.repository.BookingRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * tr: Admin tahtası bütün aktiv şubeleri tek listede gösterir. Şube seçimi yok.
 *     Sayaclar filtrelenmiş gerçek toplamdır. Yazma, partner panelindeki kuralla gider.
 * en: The admin board lists every active branch. There is no branch picker.
 *     Counters are filtered real totals. Writes follow the partner-panel rule.
 */
@Service
@RequiredArgsConstructor
public class AdminBookingBoardService {

    public static final int PAGE_SIZE = 20;
    private static final ZoneId ZONE = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final BookingRepository bookingRepository;
    private final BookingMessageRepository messageRepository;
    private final StaffBookingService staffBookingService;
    private final StaffCarHistoryService staffCarHistoryService;
    private final BookingStaffNoteService bookingStaffNoteService;

    public static List<String> statuses(String bucket) {
        String key = bucket == null ? "" : bucket.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "ready" -> List.of(
                    BookingStatus.CONFIRMED.apiValue(),
                    BookingStatus.AUTO_ACCEPTED.apiValue());
            case "completed" -> List.of(BookingStatus.COMPLETED.apiValue());
            case "rejected" -> List.of(
                    BookingStatus.REJECTED.apiValue(),
                    BookingStatus.CANCELLED.apiValue(),
                    "canceled",
                    BookingStatus.NO_SHOW.apiValue());
            default -> List.of(BookingStatus.PENDING.apiValue());
        };
    }

    public static String bucket(String raw) {
        String key = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "ready", "completed", "rejected" -> key;
            default -> "pending";
        };
    }

    @Transactional(readOnly = true)
    public AdminBookingBoardPage page(String bucketRaw, int page, String vin, String plate, String firstName,
                                      String lastName, String phone, LocalDate dateFrom, LocalDate dateTo) {
        String bucket = bucket(bucketRaw);
        int safePage = Math.max(page, 1);
        Filter filter = new Filter(blank(vin), blank(plate), blank(firstName), blank(lastName), blank(phone),
                dateFrom, dateTo);
        boolean newest = "completed".equals(bucket) || "rejected".equals(bucket);
        Page<Booking> result = bookingRepository.findAll(
                spec(filter, statuses(bucket), newest),
                PageRequest.of(safePage - 1, PAGE_SIZE));
        List<BookingView> items = new ArrayList<>();
        for (Booking booking : result.getContent()) {
            items.add(staffBookingService.viewForAdmin(booking));
        }
        long total = result.getTotalElements();
        int pages = result.getTotalPages();
        return AdminBookingBoardPage.builder()
                .pending(bookingRepository.count(spec(filter, statuses("pending"), null)))
                .ready(bookingRepository.count(spec(filter, statuses("ready"), null)))
                .completed(bookingRepository.count(spec(filter, statuses("completed"), null)))
                .rejected(bookingRepository.count(spec(filter, statuses("rejected"), null)))
                .page(safePage)
                .pageSize(PAGE_SIZE)
                .total(total)
                .totalPages(pages)
                .items(items)
                .build();
    }

    @Transactional
    public BookingView accept(Long bookingId) {
        return staffBookingService.adminAccept(bookingId, "Asia/Baku", "az");
    }

    @Transactional
    public BookingView reject(Long bookingId, BookingRejectRequest request) {
        return staffBookingService.adminReject(bookingId, request, "Asia/Baku", "az");
    }

    @Transactional
    public BookingView noShow(Long bookingId, BookingRejectRequest request) {
        return staffBookingService.adminNoShow(bookingId, request, "Asia/Baku", "az");
    }

    @Transactional
    public BookingView hide(Long bookingId) {
        return staffBookingService.adminHide(bookingId, "Asia/Baku", "az");
    }

    @Transactional(readOnly = true)
    public StaffCompletionDraftView draft(Long bookingId) {
        return staffBookingService.adminDraft(bookingId, "az");
    }

    @Transactional
    public BookingView complete(Long bookingId, StaffCompleteRequest request) {
        return staffBookingService.adminComplete(bookingId, request, "Asia/Baku", "az");
    }

    @Transactional(readOnly = true)
    public StaffNotesResponse notes(String kind) {
        return bookingStaffNoteService.list(kind, "az");
    }

    @Transactional(readOnly = true)
    public StaffCarHistoryResponse history(Long bookingId, Long carId, String vin) {
        Branch branch = activeBranch(bookingId);
        return staffCarHistoryService.historyOnBranch(branch, carId, vin, "az");
    }

    @Transactional(readOnly = true)
    public List<AdminBookingMessageView> messages(Long bookingId) {
        activeBranch(bookingId);
        List<AdminBookingMessageView> rows = new ArrayList<>();
        for (BookingMessage message : messageRepository.findByBooking_IdOrderByCreatedAtAsc(bookingId)) {
            rows.add(toMessage(message));
        }
        return rows;
    }

    @Transactional
    public AdminBookingMessageView send(Long bookingId, String body) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        activeBranch(bookingId);
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw MissingFieldException.required("body");
        }
        if (text.length() > 2000) {
            throw new MissingFieldException("body is too long");
        }
        BookingMessage saved = messageRepository.save(BookingMessage.builder()
                .booking(booking)
                .fromRole("branch")
                .body(text)
                .createdAt(OffsetDateTime.now(ZONE))
                .build());
        return toMessage(saved);
    }

    private Branch activeBranch(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        Branch branch = booking.getBranch();
        boolean open = branch != null
                && !Boolean.FALSE.equals(branch.getActive())
                && branch.getPartner() != null
                && !Boolean.FALSE.equals(branch.getPartner().getActive());
        if (!open) {
            throw new ForbiddenException("Əməkdaşlıq dayandırılıb");
        }
        return branch;
    }

    private static AdminBookingMessageView toMessage(BookingMessage message) {
        OffsetDateTime at = message.getCreatedAt() == null ? OffsetDateTime.now(ZONE) : message.getCreatedAt();
        return AdminBookingMessageView.builder()
                .id(message.getId())
                .fromRole(message.getFromRole())
                .body(message.getBody())
                .createdAt(STAMP.format(at.atZoneSameInstant(ZONE)))
                .build();
    }

    private Specification<Booking> spec(Filter filter, List<String> statuses, Boolean newestFirst) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            Join<?, ?> branch = root.join("branch", JoinType.INNER);
            Join<?, ?> partner = branch.join("partner", JoinType.INNER);
            parts.add(cb.or(cb.isNull(branch.get("active")), cb.isTrue(branch.get("active"))));
            parts.add(cb.or(cb.isNull(partner.get("active")), cb.isTrue(partner.get("active"))));
            parts.add(cb.or(cb.isNull(root.get("staffHidden")), cb.isFalse(root.get("staffHidden"))));
            parts.add(root.get("status").in(statuses));
            if (filter.vin != null) {
                parts.add(cb.like(cb.lower(root.get("vin")), filter.vin, '\\'));
            }
            if (filter.plate != null) {
                parts.add(cb.exists(carMatch(query.subquery(Long.class), root, cb, filter.plate)));
            }
            if (filter.firstName != null || filter.lastName != null || filter.phone != null) {
                parts.add(cb.exists(customerMatch(query.subquery(Long.class), root, cb, filter)));
            }
            Join<?, ?> range = root.join("range", JoinType.LEFT);
            Join<?, ?> calendar = range.join("calendar", JoinType.LEFT);
            if (filter.dateFrom != null) {
                parts.add(cb.greaterThanOrEqualTo(calendar.get("day"), filter.dateFrom));
            }
            if (filter.dateTo != null) {
                parts.add(cb.lessThanOrEqualTo(calendar.get("day"), filter.dateTo));
            }
            if (newestFirst != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(
                        Boolean.TRUE.equals(newestFirst) ? cb.desc(calendar.get("day")) : cb.asc(calendar.get("day")),
                        Boolean.TRUE.equals(newestFirst) ? cb.desc(range.get("start")) : cb.asc(range.get("start")),
                        Boolean.TRUE.equals(newestFirst) ? cb.desc(root.get("id")) : cb.asc(root.get("id")));
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    private static Subquery<Long> carMatch(Subquery<Long> sq, Root<Booking> booking,
                                           jakarta.persistence.criteria.CriteriaBuilder cb, String plate) {
        Root<Car> car = sq.from(Car.class);
        sq.select(car.get("carId"));
        sq.where(
                cb.equal(car.get("carId"), booking.get("carId")),
                cb.like(cb.lower(car.get("plateNumber")), plate, '\\'));
        return sq;
    }

    private static Subquery<Long> customerMatch(Subquery<Long> sq, Root<Booking> booking,
                                                jakarta.persistence.criteria.CriteriaBuilder cb, Filter filter) {
        Root<Customer> customer = sq.from(Customer.class);
        List<Predicate> parts = new ArrayList<>();
        parts.add(cb.equal(customer.get("userId"), booking.get("customerUserId")));
        if (filter.firstName != null) {
            parts.add(cb.like(cb.lower(customer.get("name")), filter.firstName, '\\'));
        }
        if (filter.lastName != null) {
            parts.add(cb.like(cb.lower(customer.get("surname")), filter.lastName, '\\'));
        }
        if (filter.phone != null) {
            parts.add(cb.like(cb.lower(customer.get("phoneNumber")), filter.phone, '\\'));
        }
        sq.select(customer.get("userId"));
        sq.where(parts.toArray(Predicate[]::new));
        return sq;
    }

    private static String blank(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + trimmed + "%";
    }

    private record Filter(String vin, String plate, String firstName, String lastName, String phone,
                          LocalDate dateFrom, LocalDate dateTo) {
    }
}
