package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * tr: Staff tahtası. Sayfa 20 satır. Sıra randevu günü, sonra range başlangıcı. Oluşturma zamanı değil.
 *     Tamamlanan ve reddedilen günler yeniden eskiye; diğer sekmeler eski günden yeniye.
 * en: Staff board. A page is 20 rows. Order is the appointment day, then the range start. Not createdAt.
 *     Completed and rejected days run newest first. The other tabs run oldest day first.
 */
public final class BookingInboxSpec {

    public static final int PAGE_SIZE = 20;

    private static final List<String> PENDING = List.of("pending");
    private static final List<String> CONFIRMED = List.of("confirmed", "auto_accepted");
    private static final List<String> COMPLETED = List.of("completed");
    private static final List<String> REJECTED = List.of("rejected", "cancelled", "canceled", "no_show");
    private static final List<String> ALL = List.of(
            "pending", "confirmed", "auto_accepted", "rejected", "cancelled", "canceled",
            "completed", "no_show");

    private BookingInboxSpec() {
    }

    public static String tab(String status) {
        if (status == null || status.isBlank()) {
            return "pending";
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> statuses(String wanted) {
        return switch (tab(wanted)) {
            case "confirmed" -> CONFIRMED;
            case "completed" -> COMPLETED;
            case "rejected" -> REJECTED;
            case "all", "board" -> ALL;
            case "pending" -> PENDING;
            default -> List.of(tab(wanted));
        };
    }

    public static boolean newestDayFirst(String wanted) {
        String key = tab(wanted);
        return "completed".equals(key) || "rejected".equals(key);
    }

    public static Pageable page(int page, boolean newestDayFirst) {
        int index = Math.max(page, 1) - 1;
        Sort.Order day = newestDayFirst
                ? Sort.Order.desc("range.calendar.day")
                : Sort.Order.asc("range.calendar.day");
        return PageRequest.of(index, PAGE_SIZE, Sort.by(day, Sort.Order.asc("range.start"), Sort.Order.asc("id")));
    }

    public static Specification<Booking> where(
            Long branchId,
            Long partnerId,
            boolean activePlaces,
            Collection<String> statuses,
            String vin,
            String plate,
            String firstName,
            String lastName,
            String phone,
            LocalDate dayFrom,
            LocalDate dayTo
    ) {
        String vinNeedle = like(vin);
        String plateNeedle = CarSpec.plateNeedle(plate);
        String firstNeedle = like(firstName);
        String lastNeedle = like(lastName);
        String phoneNeedle = phoneNeedle(phone);
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.or(cb.isNull(root.get("staffHidden")), cb.isFalse(root.get("staffHidden"))));
            if (branchId != null) {
                parts.add(cb.equal(root.get("branch").get("id"), branchId));
            } else if (partnerId != null) {
                parts.add(cb.equal(root.get("branch").get("partner").get("id"), partnerId));
            }
            if (activePlaces) {
                parts.add(open(cb, root.get("branch").get("active")));
                parts.add(open(cb, root.get("branch").get("partner").get("active")));
            }
            if (statuses != null && !statuses.isEmpty()) {
                parts.add(root.get("status").in(statuses));
            }
            if (vinNeedle != null) {
                parts.add(cb.like(cb.lower(cb.coalesce(root.get("vin"), "")), vinNeedle));
            }
            if (plateNeedle != null && query != null) {
                parts.add(plateExists(root, query, cb, "%" + plateNeedle + "%"));
            }
            if ((firstNeedle != null || lastNeedle != null || phoneNeedle != null) && query != null) {
                parts.add(customerExists(root, query, cb, firstNeedle, lastNeedle, phoneNeedle));
            }
            if (dayFrom != null || dayTo != null) {
                var calendar = root.join("range", JoinType.LEFT).join("calendar", JoinType.LEFT);
                if (dayFrom != null) {
                    parts.add(cb.greaterThanOrEqualTo(calendar.get("day"), dayFrom));
                }
                if (dayTo != null) {
                    parts.add(cb.lessThanOrEqualTo(calendar.get("day"), dayTo));
                }
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    public static Specification<Booking> status(String wanted) {
        List<String> statuses = statuses(wanted);
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    private static Predicate open(CriteriaBuilder cb, Expression<Boolean> flag) {
        return cb.or(cb.isNull(flag), cb.isTrue(flag));
    }

    private static Predicate plateExists(Root<Booking> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                         CriteriaBuilder cb, String needle) {
        Subquery<Integer> exists = query.subquery(Integer.class);
        Root<Car> car = exists.from(Car.class);
        exists.select(cb.literal(1));
        exists.where(
                cb.equal(car.get("carId"), root.get("carId")),
                cb.like(normalizedPlate(cb, cb.coalesce(car.get("plateNumber"), "")), needle));
        return cb.exists(exists);
    }

    private static Predicate customerExists(Root<Booking> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                            CriteriaBuilder cb, String firstNeedle, String lastNeedle,
                                            String phoneNeedle) {
        Subquery<Integer> exists = query.subquery(Integer.class);
        Root<Customer> customer = exists.from(Customer.class);
        List<Predicate> parts = new ArrayList<>();
        parts.add(cb.equal(customer.get("userId"), root.get("customerUserId")));
        if (firstNeedle != null) {
            parts.add(cb.like(cb.lower(cb.coalesce(customer.get("name"), "")), firstNeedle));
        }
        if (lastNeedle != null) {
            parts.add(cb.like(cb.lower(cb.coalesce(customer.get("surname"), "")), lastNeedle));
        }
        if (phoneNeedle != null) {
            parts.add(cb.like(phoneKey(cb, cb.coalesce(customer.get("phoneNumber"), "")), "%" + phoneNeedle + "%"));
        }
        exists.select(cb.literal(1));
        exists.where(parts.toArray(Predicate[]::new));
        return cb.exists(exists);
    }

    private static Expression<String> normalizedPlate(CriteriaBuilder cb, Expression<String> plate) {
        Expression<String> value = plate;
        value = cb.function("replace", String.class, value, cb.literal("-"), cb.literal(""));
        value = cb.function("replace", String.class, value, cb.literal(" "), cb.literal(""));
        value = cb.function("replace", String.class, value, cb.literal("."), cb.literal(""));
        value = cb.function("replace", String.class, value, cb.literal("\u00A0"), cb.literal(""));
        return cb.upper(value);
    }

    private static Expression<String> phoneKey(CriteriaBuilder cb, Expression<String> phone) {
        Expression<String> value = cb.lower(phone);
        value = cb.function("replace", String.class, value, cb.literal("-"), cb.literal(""));
        value = cb.function("replace", String.class, value, cb.literal(" "), cb.literal(""));
        return value;
    }

    private static String like(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return "%" + raw.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static String phoneNeedle(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String value = phone.replace("-", "").replace(" ", "").trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }
}
