package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitPart;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.service.BookingKindLabel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tr: Ziyarəti PDF sətirinə çevirir. Hyper sətri Toyota kimi solda xidmət, sağda hissədir.
 *     Eyni ad iki sətir olmur. Panel sətri {@code kind} ilə qalır.
 * en: Turns a visit into PDF lines. A Hyper row is service on the left and the part on the right, as on Toyota.
 *     The same name is not printed twice. A panel row still follows {@code kind}.
 */
final class DigitalHistoryVisits {

    private DigitalHistoryVisits() {
    }

    interface Names {
        String service(VisitServiceLine line);

        String product(VisitServiceLine line);
    }

    static DigitalHistoryReport.VisitBlock toBlock(Visit visit, Branch branch, Partner partner, Names names) {
        List<VisitServiceLine> lines = sortedLines(visit);
        boolean repair = hasText(visit.getInspectionNote())
                || lines.stream().anyMatch(line -> kind(line, "inspection"));
        boolean routine = hasText(visit.getPackageName())
                || lines.stream().anyMatch(line -> kind(line, "package", "package_info", "individual", "one_time"));
        boolean panel = visit.getBookingId() != null
                || "panel".equalsIgnoreCase(text(visit.getSource()))
                || lines.stream().anyMatch(line -> hasText(line.getKind()));

        List<String> packageLines = new ArrayList<>();
        List<String> individualLines = new ArrayList<>();
        List<String> inspectionLines = new ArrayList<>();
        List<String> plainLines = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (!panel) {
            hyperLines(visit, lines, names, plainLines);
        } else {
            for (VisitServiceLine line : lines) {
                String printed = lineText(line, names);
                remember(seen, names.product(line));
                if (printed == null) {
                    continue;
                }
                if (kind(line, "package", "package_info")) {
                    packageLines.add(printed);
                } else if (kind(line, "individual", "one_time")) {
                    individualLines.add(printed);
                } else if (kind(line, "inspection")) {
                    inspectionLines.add(printed);
                } else {
                    plainLines.add(printed);
                }
            }
            for (VisitPart part : sortedParts(visit)) {
                String printed = partRow(part);
                if (printed == null) {
                    continue;
                }
                String key = text(part.getName()).toLowerCase(Locale.ROOT);
                if (!key.isEmpty() && seen.contains(key)) {
                    continue;
                }
                plainLines.add(printed);
            }
        }

        String label = routine || repair ? BookingKindLabel.of(routine, repair) : null;
        return new DigitalHistoryReport.VisitBlock(
                DigitalHistoryFormat.km(visit.getLastServiceMileage()),
                DigitalHistoryFormat.day(visit.getLastServiceDate()),
                centerName(visit, branch, partner),
                branch == null ? "" : text(branch.getAddress()),
                label,
                clean(visit.getPackageName()),
                List.copyOf(packageLines),
                List.copyOf(individualLines),
                clean(visit.getInspectionNote()),
                List.copyOf(inspectionLines),
                List.copyOf(plainLines),
                DigitalHistoryFormat.money(visit.getFinalCostAmount(), visit.getFinalCostCurrency()),
                repair
        );
    }

    /**
     * tr: Xidmət solda, eyni adlı hissənin miqdarı sağda. Qalan hissə öz sətrindədir.
     * en: Service on the left, quantity of a same-named part on the right. A remaining part is its own row.
     */
    private static void hyperLines(Visit visit, List<VisitServiceLine> lines, Names names, List<String> plainLines) {
        List<VisitPart> parts = sortedParts(visit);
        boolean[] used = new boolean[parts.size()];
        Set<String> seen = new HashSet<>();
        for (VisitServiceLine line : lines) {
            String name = text(names.service(line));
            if (name.isEmpty()) {
                continue;
            }
            String detail = takeParts(parts, used, name);
            plainLines.add(detail.isEmpty() ? name : name + " · " + detail);
            remember(seen, name);
        }
        for (int i = 0; i < parts.size(); i++) {
            if (used[i]) {
                continue;
            }
            String name = text(parts.get(i).getName());
            if (name.isEmpty() || seen.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            String detail = partDetail(parts.get(i));
            plainLines.add(detail.isEmpty() ? name : name + " · " + detail);
        }
    }

    private static String takeParts(List<VisitPart> parts, boolean[] used, String serviceName) {
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (used[i] || !serviceName.equalsIgnoreCase(text(parts.get(i).getName()))) {
                continue;
            }
            used[i] = true;
            String qty = partDetail(parts.get(i));
            if (qty.isEmpty()) {
                continue;
            }
            if (!detail.isEmpty()) {
                detail.append(", ");
            }
            detail.append(qty);
        }
        return detail.toString();
    }

    static String centerName(Visit visit, Branch branch, Partner partner) {
        if (branch != null && hasText(branch.getName())) {
            return branch.getName().trim();
        }
        if (partner != null && hasText(partner.getName())) {
            return partner.getName().trim();
        }
        return text(visit.getServiceCenterName());
    }

    static String phone(Branch branch, Partner partner) {
        if (branch != null && hasText(branch.getContactPhone())) {
            return branch.getContactPhone().trim();
        }
        if (partner != null && hasText(partner.getContactPhone())) {
            return partner.getContactPhone().trim();
        }
        return "";
    }

    static String email(Branch branch, Partner partner) {
        if (branch != null && hasText(branch.getContactEmail())) {
            return branch.getContactEmail().trim();
        }
        if (partner != null && hasText(partner.getContactEmail())) {
            return partner.getContactEmail().trim();
        }
        return "";
    }

    private static List<VisitServiceLine> sortedLines(Visit visit) {
        if (visit.getServices() == null) {
            return List.of();
        }
        return visit.getServices().stream()
                .sorted(Comparator.comparing(VisitServiceLine::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private static List<VisitPart> sortedParts(Visit visit) {
        if (visit.getParts() == null) {
            return List.of();
        }
        return visit.getParts().stream()
                .sorted(Comparator.comparing(VisitPart::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private static boolean kind(VisitServiceLine line, String... expected) {
        if (line.getKind() == null || line.getKind().isBlank()) {
            return false;
        }
        String value = line.getKind().trim().toLowerCase(Locale.ROOT);
        for (String one : expected) {
            if (one.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String lineText(VisitServiceLine line, Names names) {
        List<String> bits = new ArrayList<>();
        add(bits, names.service(line));
        add(bits, names.product(line));
        String qty = DigitalHistoryFormat.qty(line.getProductQty());
        if (!qty.isEmpty()) {
            bits.add(qty);
        }
        add(bits, line.getNotes());
        if (bits.isEmpty()) {
            return null;
        }
        return String.join(" · ", bits);
    }

    private static String partRow(VisitPart part) {
        if (!hasText(part.getName())) {
            return null;
        }
        String detail = partDetail(part);
        return detail.isEmpty() ? part.getName().trim() : part.getName().trim() + " · " + detail;
    }

    private static String partDetail(VisitPart part) {
        String qty = DigitalHistoryFormat.qty(part.getQty());
        String unit = text(part.getUnit());
        if (qty.isEmpty()) {
            return unit;
        }
        if (unit.isEmpty()) {
            return qty;
        }
        return qty + " " + unit;
    }

    private static void add(List<String> bits, String value) {
        if (hasText(value)) {
            bits.add(value.trim());
        }
    }

    private static void remember(Set<String> seen, String value) {
        if (hasText(value)) {
            seen.add(value.trim().toLowerCase(Locale.ROOT));
        }
    }

    private static String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
