package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.ServicedBranchResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Visit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * tr: Rozet sırasındaki şube kartları. Gün {@code last_service_date}, saat {@code created_at}.
 *     Saat 24 saattir. Az dilinde ayın baş harfi büyür.
 * en: Branch cards in badge order. The day is {@code last_service_date}, the clock is {@code created_at}.
 *     The clock is 24-hour. The Azerbaijani month starts with a capital letter.
 */
public final class ServicedBranchList {

    private ServicedBranchList() {
    }

    public static List<ServicedBranchResponse> cards(List<Long> branchIds,
                                                     List<Visit> visits,
                                                     Map<Long, Branch> branches,
                                                     String acceptLanguage) {
        Map<Long, List<Visit>> byBranch = new HashMap<>();
        if (visits != null) {
            for (Visit visit : visits) {
                Long branchId = ServicedBadgeIds.resolvedBranchId(visit);
                if (branchId == null) {
                    continue;
                }
                byBranch.computeIfAbsent(branchId, id -> new ArrayList<>()).add(visit);
            }
        }
        List<ServicedBranchResponse> cards = new ArrayList<>();
        if (branchIds == null || branches == null) {
            return cards;
        }
        for (Long branchId : branchIds) {
            Branch branch = branches.get(branchId);
            if (branch == null) {
                continue;
            }
            List<Visit> matched = byBranch.getOrDefault(branchId, List.of());
            Visit latest = latest(matched);
            cards.add(ServicedBranchResponse.builder()
                    .branchId(branch.getId())
                    .branchName(branch.getName())
                    .lat(branch.getLat())
                    .lng(branch.getLng())
                    .completedServiceCount(matched.size())
                    .lastServiceAt(latest == null ? null : format(latest, acceptLanguage))
                    .build());
        }
        return cards;
    }

    /**
     * tr: En yeni ziyaret: önce hizmet günü, sonra kayıt saati, sonra id.
     * en: Newest visit: service day, then the row clock, then id.
     */
    static Visit latest(List<Visit> visits) {
        Visit best = null;
        for (Visit visit : visits) {
            if (visit == null) {
                continue;
            }
            if (best == null || newer(visit, best)) {
                best = visit;
            }
        }
        return best;
    }

    static String format(Visit visit, String acceptLanguage) {
        if (visit == null) {
            return null;
        }
        LocalDate day = visit.getLastServiceDate();
        LocalDateTime createdAt = visit.getCreatedAt();
        if (day == null && createdAt == null) {
            return null;
        }
        if (day == null) {
            day = createdAt.toLocalDate();
        }
        Locale locale = Locale.forLanguageTag(acceptLanguage == null ? "az" : acceptLanguage);
        DateTimeFormatter formatter = createdAt == null
                ? DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
                : DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", locale);
        LocalDateTime stamp = createdAt == null ? day.atStartOfDay() : day.atTime(createdAt.toLocalTime());
        return capitalizeAzMonth(stamp.format(formatter), locale);
    }

    private static boolean newer(Visit candidate, Visit current) {
        int byDay = compare(candidate.getLastServiceDate(), current.getLastServiceDate());
        if (byDay != 0) {
            return byDay > 0;
        }
        int byClock = compare(candidate.getCreatedAt(), current.getCreatedAt());
        if (byClock != 0) {
            return byClock > 0;
        }
        long left = candidate.getId() == null ? Long.MIN_VALUE : candidate.getId();
        long right = current.getId() == null ? Long.MIN_VALUE : current.getId();
        return left > right;
    }

    private static <T extends Comparable<? super T>> int compare(T left, T right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static String capitalizeAzMonth(String text, Locale locale) {
        if (text == null || locale == null || !"az".equals(locale.getLanguage())) {
            return text;
        }
        String[] parts = text.split(" ");
        if (parts.length < 2 || parts[1].isEmpty()) {
            return text;
        }
        parts[1] = parts[1].substring(0, 1).toUpperCase(locale) + parts[1].substring(1);
        return String.join(" ", parts);
    }
}
