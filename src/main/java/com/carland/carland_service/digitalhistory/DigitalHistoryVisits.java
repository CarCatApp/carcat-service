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
 * tr: Ziyareti PDF satırına çevirir. Paket, fərdi və təmir, tamamlanma anında satıra yazılmış {@code kind} değeridir.
 *     Hyper satırında {@code kind} boştur; servis adı ve parça düz liste olur, Dövri/Təmir etiketi konmaz.
 * en: Turns a visit into PDF lines. Package, individual and repair come from the {@code kind} stored at completion.
 *     A Hyper line has an empty {@code kind}; the service name and parts stay a flat list, with no routine/repair label.
 */
final class DigitalHistoryVisits {

    private DigitalHistoryVisits() {
    }

    static DigitalHistoryReport.VisitBlock toBlock(Visit visit, Branch branch, Partner partner) {
        List<VisitServiceLine> lines = sortedLines(visit);
        boolean routine = hasText(visit.getPackageName())
                || lines.stream().anyMatch(line -> kind(line, "package", "package_info", "individual", "one_time"));
        boolean repair = hasText(visit.getInspectionNote())
                || lines.stream().anyMatch(line -> kind(line, "inspection"));
        boolean panel = visit.getBookingId() != null
                || "panel".equalsIgnoreCase(text(visit.getSource()))
                || lines.stream().anyMatch(line -> hasText(line.getKind()));

        List<String> packageLines = new ArrayList<>();
        List<String> individualLines = new ArrayList<>();
        List<String> inspectionLines = new ArrayList<>();
        List<String> plainLines = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (VisitServiceLine line : lines) {
            String printed = lineText(line);
            remember(seen, line.getProductName());
            if (printed == null) {
                continue;
            }
            if (!panel) {
                plainLines.add(printed);
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
            String printed = partText(part);
            if (printed == null) {
                continue;
            }
            String key = text(part.getName()).toLowerCase(Locale.ROOT);
            if (!key.isEmpty() && seen.contains(key)) {
                continue;
            }
            plainLines.add(printed);
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
                DigitalHistoryFormat.money(visit.getFinalCostAmount(), visit.getFinalCostCurrency())
        );
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

    private static String lineText(VisitServiceLine line) {
        List<String> bits = new ArrayList<>();
        add(bits, line.getServiceName());
        add(bits, line.getProductName());
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

    private static String partText(VisitPart part) {
        if (!hasText(part.getName())) {
            return null;
        }
        List<String> bits = new ArrayList<>();
        bits.add(part.getName().trim());
        String qty = DigitalHistoryFormat.qty(part.getQty());
        if (!qty.isEmpty()) {
            bits.add(qty);
        }
        add(bits, part.getUnit());
        return String.join(" · ", bits);
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
