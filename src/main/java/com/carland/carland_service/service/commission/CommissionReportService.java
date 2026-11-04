package com.carland.carland_service.service.commission;

import com.carland.carland_service.dto.commission.CommissionDetail;
import com.carland.carland_service.dto.commission.CommissionExport;
import com.carland.carland_service.dto.commission.CommissionScreen;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.CommissionEntry;
import com.carland.carland_service.entity.CommissionRate;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitPart;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CommissionEntryRepository;
import com.carland.carland_service.repository.CommissionRateRepository;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.repository.VisitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * tr: Admin komisyon listesi, özet, detay, oran ekleme ve Excel verisi. DB okur, cache yazmaz.
 * en: Admin commission list, summary, detail, rate insert and Excel data. Reads the DB, writes no cache.
 */
@Service
@RequiredArgsConstructor
public class CommissionReportService {

    private static final int PAGE_SIZE = 25;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CommissionEntryRepository entries;
    private final CommissionRateRepository rates;
    private final PartnerRepository partners;
    private final BranchRepository branches;
    private final VisitRepository visits;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public CommissionScreen screen(String preset, LocalDate from, LocalDate to, Long partnerId, Long branchId,
                                   String status, String search, String sort, String dir, int page) {
        CommissionScreen.Query query = query(preset, from, to, partnerId, branchId, status, search, sort, dir);
        Specification<CommissionEntry> spec = spec(query);
        Totals totals = totals(spec);
        Page<CommissionEntry> result = page(spec, query, page);
        List<Partner> partnerRows = partners.findAllByOrderByIdDesc();
        List<Branch> branchRows = branches.findAll();
        Map<Long, String> partnerNames = names(partnerRows);
        return CommissionScreen.builder()
                .query(query)
                .rows(result.getContent().stream().map(this::row).toList())
                .fresh(totals.fresh)
                .again(totals.again)
                .all(totals.all)
                .pendingCount(totals.pending)
                .empty(totals.all.getCount() == 0)
                .showGroups(distinctPartners(spec) > 1)
                .groups(groups(spec, partnerNames))
                .partners(partnerRows.stream()
                        .sorted(Comparator.comparing(partner -> partner.getName() == null ? "" : partner.getName(), String.CASE_INSENSITIVE_ORDER))
                        .map(partner -> new CommissionScreen.Option(partner.getId(), partner.getName(), partner.getId().equals(query.getPartnerId())))
                        .toList())
                .branches(branchRows.stream()
                        .sorted(Comparator.comparing(branch -> label(branch, partnerNames), String.CASE_INSENSITIVE_ORDER))
                        .map(branch -> new CommissionScreen.Option(branch.getId(), label(branch, partnerNames), branch.getId().equals(query.getBranchId())))
                        .toList())
                .rates(rateRows(partnerNames))
                .currentPage(result.getNumber() + 1)
                .totalPages(Math.max(result.getTotalPages(), 1))
                .windowStart(windowStart(result.getNumber() + 1, Math.max(result.getTotalPages(), 1)))
                .windowEnd(windowEnd(result.getNumber() + 1, Math.max(result.getTotalPages(), 1)))
                .build();
    }

    @Transactional(readOnly = true)
    public CommissionDetail detail(Long id) {
        CommissionEntry entry = entries.findById(id).orElse(null);
        if (entry == null) {
            return null;
        }
        boolean shared = sharedDay(entry);
        List<Visit> source = visits.findCommissionByBookingId(entry.getBookingId());
        if (source.isEmpty() && entry.getVin() != null && entry.getPartnerId() != null) {
            source = visits.findCommissionHyperVisits(
                    entry.getVin(), CommissionClock.bakuDate(entry.getCompletedAt()), entry.getPartnerId());
        }
        return CommissionDetail.builder()
                .id(entry.getId())
                .completedAt(CommissionClock.dateTime(entry.getCompletedAt()))
                .partnerName(text(entry.getPartnerName()))
                .branchName(text(entry.getBranchName()))
                .vin(text(entry.getVin()))
                .plate(text(entry.getPlate()))
                .ref(text(entry.getRef()))
                .statusLabel(CommissionStatus.label(entry.getCustomerStatus()))
                .rate(CommissionMoney.percent(entry.getRatePercent()))
                .pending(!CommissionEntry.READY.equals(entry.getAmountState()))
                .serviceAmount(CommissionMoney.azn(entry.getServiceAmount()))
                .commission(CommissionMoney.azn(entry.getCommissionAmount()))
                .decidedAt(CommissionClock.dateTime(entry.getStatusDecidedAt()))
                .outcome(CommissionStatusRules.outcomeLabel(entry.getHyperOutcome()))
                .earliest(entry.getEarliestServiceDate() == null ? "—" : CommissionClock.date(entry.getEarliestServiceDate()))
                .snapshot(entry.getHyperSnapshot())
                .why(why(entry))
                .sharedDay(shared && !CommissionEntry.READY.equals(entry.getAmountState()))
                .lines(lines(source))
                .build();
    }

    @Transactional(readOnly = true)
    public CommissionExport export(String preset, LocalDate from, LocalDate to, Long partnerId, Long branchId,
                                   String status, String search) {
        CommissionScreen.Query query = query(preset, from, to, partnerId, branchId, status, search, "completed", "desc");
        Specification<CommissionEntry> spec = spec(query);
        Totals totals = totals(spec);
        Map<Long, String> partnerNames = names(partners.findAll());
        Map<Long, String> branchNames = new HashMap<>();
        for (Branch branch : branches.findAll()) {
            branchNames.put(branch.getId(), label(branch, partnerNames));
        }
        List<CommissionExport.Line> lines = entries.findAll(spec, Sort.by(Sort.Order.desc("completedAt"), Sort.Order.desc("id")))
                .stream()
                .map(this::exportLine)
                .toList();
        return new CommissionExport(
                query.getFrom(),
                query.getTo(),
                filters(query, partnerNames, branchNames),
                lines,
                totals.fresh,
                totals.again,
                totals.all,
                totals.pending,
                groups(spec, partnerNames));
    }

    @Transactional
    public String addRate(Long partnerId, String status, String percentText, LocalDate effectiveFrom, String note) {
        if (partnerId == null || partners.findById(partnerId).isEmpty()) {
            return "Servis seçin.";
        }
        if (!CommissionStatus.known(status)) {
            return "Status seçin.";
        }
        BigDecimal percent = parsePercent(percentText);
        if (percent == null || percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            return "Faiz 0 ilə 100 arasında olmalıdır.";
        }
        if (effectiveFrom == null) {
            return "Qüvvəyə minmə tarixini seçin.";
        }
        rates.save(CommissionRate.builder()
                .partnerId(partnerId)
                .customerStatus(status)
                .percent(percent.setScale(2, RoundingMode.HALF_UP))
                .effectiveFrom(effectiveFrom)
                .note(cut(note, 200))
                .build());
        return null;
    }

    private Page<CommissionEntry> page(Specification<CommissionEntry> spec, CommissionScreen.Query query, int page) {
        int requested = Math.max(page, 1) - 1;
        Page<CommissionEntry> result = entries.findAll(spec, PageRequest.of(requested, PAGE_SIZE, sort(query)));
        if (requested > 0 && result.getTotalPages() > 0 && requested >= result.getTotalPages()) {
            result = entries.findAll(spec, PageRequest.of(result.getTotalPages() - 1, PAGE_SIZE, sort(query)));
        }
        return result;
    }

    private Sort sort(CommissionScreen.Query query) {
        Sort.Direction direction = query.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = switch (query.getSort()) {
            case "partner" -> "partnerName";
            case "amount" -> "serviceAmount";
            case "commission" -> "commissionAmount";
            default -> "completedAt";
        };
        return Sort.by(direction, field).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private Totals totals(Specification<CommissionEntry> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<CommissionEntry> root = cq.from(CommissionEntry.class);
        cq.multiselect(
                root.get("customerStatus"),
                root.get("amountState"),
                cb.count(root),
                cb.sum(root.get("serviceAmount")),
                cb.sum(root.get("commissionAmount")));
        cq.where(spec.toPredicate(root, cq, cb));
        cq.groupBy(root.get("customerStatus"), root.get("amountState"));
        long newCount = 0;
        long oldCount = 0;
        long pending = 0;
        BigDecimal newService = ZERO;
        BigDecimal oldService = ZERO;
        BigDecimal newCommission = ZERO;
        BigDecimal oldCommission = ZERO;
        for (Tuple tuple : entityManager.createQuery(cq).getResultList()) {
            String status = tuple.get(0, String.class);
            String state = tuple.get(1, String.class);
            long count = number(tuple.get(2));
            if (CommissionStatus.OLD.equals(status)) {
                oldCount += count;
            } else {
                newCount += count;
            }
            if (!CommissionEntry.READY.equals(state)) {
                pending += count;
                continue;
            }
            BigDecimal service = money(tuple.get(3));
            BigDecimal commission = money(tuple.get(4));
            if (CommissionStatus.OLD.equals(status)) {
                oldService = oldService.add(service);
                oldCommission = oldCommission.add(commission);
            } else {
                newService = newService.add(service);
                newCommission = newCommission.add(commission);
            }
        }
        CommissionScreen.Bucket fresh = new CommissionScreen.Bucket(newCount, newService, newCommission);
        CommissionScreen.Bucket again = new CommissionScreen.Bucket(oldCount, oldService, oldCommission);
        CommissionScreen.Bucket all = new CommissionScreen.Bucket(
                newCount + oldCount, newService.add(oldService), newCommission.add(oldCommission));
        return new Totals(fresh, again, all, pending);
    }

    private long distinctPartners(Specification<CommissionEntry> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<CommissionEntry> root = cq.from(CommissionEntry.class);
        cq.select(root.get("partnerId")).distinct(true);
        cq.where(spec.toPredicate(root, cq, cb));
        return entityManager.createQuery(cq).getResultList().stream().filter(id -> id != null).distinct().count();
    }

    private List<CommissionScreen.PartnerGroup> groups(Specification<CommissionEntry> spec, Map<Long, String> partnerNames) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<CommissionEntry> root = cq.from(CommissionEntry.class);
        cq.multiselect(
                root.get("partnerId"),
                root.get("partnerName"),
                root.get("customerStatus"),
                root.get("amountState"),
                cb.count(root),
                cb.sum(root.get("serviceAmount")),
                cb.sum(root.get("commissionAmount")));
        cq.where(spec.toPredicate(root, cq, cb));
        cq.groupBy(root.get("partnerId"), root.get("partnerName"), root.get("customerStatus"), root.get("amountState"));
        Map<Long, GroupAcc> folded = new HashMap<>();
        for (Tuple tuple : entityManager.createQuery(cq).getResultList()) {
            Long partnerId = tuple.get(0, Long.class);
            GroupAcc acc = folded.computeIfAbsent(partnerId == null ? -1L : partnerId, id -> new GroupAcc());
            String stored = tuple.get(1, String.class);
            acc.name = stored != null ? stored : partnerNames.getOrDefault(partnerId, "—");
            acc.count += number(tuple.get(4));
            if (!CommissionEntry.READY.equals(tuple.get(3, String.class))) {
                continue;
            }
            BigDecimal service = money(tuple.get(5));
            BigDecimal commission = money(tuple.get(6));
            acc.service = acc.service.add(service);
            acc.commission = acc.commission.add(commission);
            if (CommissionStatus.OLD.equals(tuple.get(2, String.class))) {
                acc.oldCommission = acc.oldCommission.add(commission);
            } else {
                acc.newCommission = acc.newCommission.add(commission);
            }
        }
        return folded.values().stream()
                .sorted(Comparator.comparing(acc -> acc.name == null ? "" : acc.name, String.CASE_INSENSITIVE_ORDER))
                .map(acc -> new CommissionScreen.PartnerGroup(
                        acc.name == null ? "—" : acc.name, acc.count, acc.service, acc.newCommission, acc.oldCommission, acc.commission))
                .toList();
    }

    private boolean sharedDay(CommissionEntry entry) {
        if (entry.getVin() == null) {
            return false;
        }
        LocalDate day = CommissionClock.bakuDate(entry.getCompletedAt());
        return entries.findByVin(entry.getVin()).stream()
                .filter(row -> same(row.getPartnerId(), entry.getPartnerId()))
                .filter(row -> day.equals(CommissionClock.bakuDate(row.getCompletedAt())))
                .count() > 1;
    }

    private List<CommissionDetail.Line> lines(List<Visit> source) {
        List<CommissionDetail.Line> lines = new ArrayList<>();
        for (Visit visit : source) {
            String visitLabel = CommissionClock.date(visit.getLastServiceDate());
            if (visit.getInvoiceNumber() != null && !visit.getInvoiceNumber().isBlank()) {
                visitLabel = visitLabel + " · " + visit.getInvoiceNumber();
            }
            BigDecimal visitAmount = visit.getFinalCostAmount() != null ? visit.getFinalCostAmount() : visit.getCostAmount();
            lines.add(CommissionDetail.Line.builder()
                    .visit(visitLabel)
                    .title("Ziyarət cəmi")
                    .amount(visitAmount == null ? "—" : CommissionMoney.azn(visitAmount))
                    .build());
            List<VisitServiceLine> services = visit.getServices() == null ? List.of() : visit.getServices();
            for (VisitServiceLine line : services) {
                BigDecimal amount = line.getFinalAmount() != null ? line.getFinalAmount()
                        : line.getServiceAmount() != null ? line.getServiceAmount() : line.getCostAmount();
                lines.add(CommissionDetail.Line.builder()
                        .visit(visitLabel)
                        .title(line.getServiceName() == null || line.getServiceName().isBlank() ? "Xidmət" : line.getServiceName())
                        .amount(amount == null ? "—" : CommissionMoney.azn(amount))
                        .build());
                if (line.getProductName() != null && !line.getProductName().isBlank()) {
                    lines.add(CommissionDetail.Line.builder()
                            .visit(visitLabel)
                            .title(line.getProductName())
                            .amount(line.getProductAmount() == null ? "—" : CommissionMoney.azn(line.getProductAmount()))
                            .build());
                }
            }
            if (services.isEmpty() && visit.getParts() != null) {
                for (VisitPart part : visit.getParts()) {
                    lines.add(CommissionDetail.Line.builder()
                            .visit(visitLabel)
                            .title(part.getName() == null ? "Məhsul" : part.getName())
                            .amount(part.getPrice() == null ? "—" : CommissionMoney.azn(part.getPrice()))
                            .build());
                }
            }
        }
        return lines;
    }

    private CommissionScreen.Row row(CommissionEntry entry) {
        boolean pending = !CommissionEntry.READY.equals(entry.getAmountState());
        return new CommissionScreen.Row(
                entry.getId(),
                CommissionClock.dateTime(entry.getCompletedAt()),
                text(entry.getPartnerName()),
                text(entry.getBranchName()),
                text(entry.getVin()),
                text(entry.getPlate()),
                entry.getCustomerStatus(),
                CommissionStatus.label(entry.getCustomerStatus()),
                pending,
                pending ? "Məbləğ gözlənilir" : CommissionMoney.azn(entry.getServiceAmount()),
                CommissionMoney.percent(entry.getRatePercent()),
                pending ? "—" : CommissionMoney.azn(entry.getCommissionAmount()));
    }

    private CommissionExport.Line exportLine(CommissionEntry entry) {
        boolean pending = !CommissionEntry.READY.equals(entry.getAmountState());
        return new CommissionExport.Line(
                CommissionClock.dateTime(entry.getCompletedAt()),
                text(entry.getPartnerName()),
                text(entry.getBranchName()),
                text(entry.getVin()),
                text(entry.getPlate()),
                CommissionStatus.label(entry.getCustomerStatus()),
                pending ? null : entry.getServiceAmount(),
                entry.getRatePercent(),
                pending ? null : entry.getCommissionAmount(),
                pending,
                text(entry.getRef()));
    }

    private List<CommissionScreen.RateRow> rateRows(Map<Long, String> partnerNames) {
        return rates.findTop30ByOrderByIdDesc().stream()
                .map(rate -> new CommissionScreen.RateRow(
                        partnerNames.getOrDefault(rate.getPartnerId(), "#" + rate.getPartnerId()),
                        CommissionStatus.label(rate.getCustomerStatus()),
                        CommissionMoney.percent(rate.getPercent()),
                        CommissionClock.date(rate.getEffectiveFrom()),
                        CommissionClock.dateTime(rate.getCreatedAt()),
                        rate.getNote() == null ? "" : rate.getNote()))
                .toList();
    }

    static CommissionScreen.Query query(String preset, LocalDate from, LocalDate to, Long partnerId, Long branchId,
                                        String status, String search, String sort, String dir) {
        CommissionPeriod period = CommissionPeriod.resolve(preset, from, to, LocalDate.now(CommissionClock.BAKU));
        String chosenSort = switch (sort == null ? "" : sort) {
            case "partner", "amount", "commission" -> sort;
            default -> "completed";
        };
        String text = search == null ? null : search.trim();
        if (text != null && text.isEmpty()) {
            text = null;
        }
        if (text != null && text.length() > 40) {
            text = text.substring(0, 40);
        }
        return new CommissionScreen.Query(
                period.from(),
                period.to(),
                period.preset(),
                partnerId == null || partnerId <= 0 ? null : partnerId,
                branchId == null || branchId <= 0 ? null : branchId,
                CommissionStatus.known(status) ? status : null,
                text,
                chosenSort,
                "asc".equalsIgnoreCase(dir));
    }

    static Specification<CommissionEntry> spec(CommissionScreen.Query query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("completedAt"), CommissionClock.start(query.getFrom())));
            predicates.add(cb.lessThan(root.get("completedAt"), CommissionClock.start(query.getTo().plusDays(1))));
            if (query.getPartnerId() != null) {
                predicates.add(cb.equal(root.get("partnerId"), query.getPartnerId()));
            }
            if (query.getBranchId() != null) {
                predicates.add(cb.equal(root.get("branchId"), query.getBranchId()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("customerStatus"), query.getStatus()));
            }
            if (query.getSearch() != null) {
                String raw = query.getSearch().toLowerCase(Locale.ROOT);
                String compact = raw.replace(" ", "");
                var vin = cb.lower(cb.coalesce(root.get("vin"), ""));
                var plate = cb.lower(cb.coalesce(root.get("plate"), ""));
                var plateCompact = cb.function("replace", String.class, plate, cb.literal(" "), cb.literal(""));
                predicates.add(cb.or(
                        cb.like(vin, like(raw), '\\'),
                        cb.like(plate, like(raw), '\\'),
                        cb.like(plateCompact, like(compact), '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String filters(CommissionScreen.Query query, Map<Long, String> partnerNames, Map<Long, String> branchNames) {
        String partner = query.getPartnerId() == null ? "Hamısı" : partnerNames.getOrDefault(query.getPartnerId(), "#" + query.getPartnerId());
        String branch = query.getBranchId() == null ? "Hamısı" : branchNames.getOrDefault(query.getBranchId(), "#" + query.getBranchId());
        String status = query.getStatus() == null ? "Hamısı" : CommissionStatus.label(query.getStatus());
        String search = query.getSearch() == null ? "—" : query.getSearch();
        return "Servis: " + partner + "; Filial: " + branch + "; Status: " + status + "; Axtarış: " + search;
    }

    private static Map<Long, String> names(List<Partner> rows) {
        Map<Long, String> names = new HashMap<>();
        for (Partner partner : rows) {
            names.put(partner.getId(), partner.getName() == null ? "#" + partner.getId() : partner.getName());
        }
        return names;
    }

    private static String label(Branch branch, Map<Long, String> partnerNames) {
        Long partnerId = branch.getPartner() == null ? null : branch.getPartner().getId();
        String partner = partnerId == null ? "" : partnerNames.getOrDefault(partnerId, "");
        String name = branch.getName() == null ? "#" + branch.getId() : branch.getName();
        return partner == null || partner.isBlank() ? name : partner + " — " + name;
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static String why(CommissionEntry entry) {
        if (CommissionStatusRules.FOUND.equals(entry.getHyperOutcome())
                && CommissionStatus.OLD.equals(entry.getCustomerStatus())) {
            return "Ən erkən servis günü randevunun açıldığı gündən əvvəldir.";
        }
        if (CommissionStatusRules.FOUND.equals(entry.getHyperOutcome())) {
            return "Ən erkən servis günü randevu günü və ya sonradır.";
        }
        return CommissionStatusRules.outcomeLabel(entry.getHyperOutcome());
    }

    private static BigDecimal parsePercent(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal money(Object value) {
        if (value == null) {
            return ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String like(String value) {
        return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static boolean same(Long left, Long right) {
        return left == null ? right == null : left.equals(right);
    }

    private static int windowStart(int current, int total) {
        int start = Math.max(1, current - 2);
        int end = Math.min(total, start + 4);
        return Math.max(1, end - 4);
    }

    private static int windowEnd(int current, int total) {
        int start = windowStart(current, total);
        return Math.min(total, start + 4);
    }

    private record Totals(CommissionScreen.Bucket fresh, CommissionScreen.Bucket again, CommissionScreen.Bucket all, long pending) {
    }

    private static final class GroupAcc {
        private String name;
        private long count;
        private BigDecimal service = ZERO;
        private BigDecimal newCommission = ZERO;
        private BigDecimal oldCommission = ZERO;
        private BigDecimal commission = ZERO;
    }
}
