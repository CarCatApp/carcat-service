package com.carland.carland_service.service;

import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceSource;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.IndividualServiceSourceRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tr: Fərdi kataloq, percentage'nin kopyalandığı services satırlarından gelir.
 *     Üç dil adı (boşluk və böyük hərf sayılmaz) aynıysa tek satır. Mock MYF…TKD silinir.
 *     Şablondan düşen ad pasif kalır, satır və şube fiyatı silinmez.
 *     individual_service_sources hər açılışda yenidən yazılır: eyni ada düşən bütün services.id
 *     bir kataloq satırına bağlanır. İstək anında ad oxunmur.
 * en: The individual catalog is the distinct services rows percentages are copied from.
 *     One row when the three names match ignoring space and case. Mock MYF…TKD codes are removed.
 *     A name that leaves the templates stays inactive; the row and branch prices stay.
 *     individual_service_sources is rewritten on every startup: every services.id that shares a name
 *     points at one catalog row. The request path does not read names.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndividualCatalogSync {

    static final Set<String> MOCK_CODES = Set.of(
            "MYF", "HVF", "SLF", "TBY", "RZV", "EBD", "EMD", "YNF",
            "ASD", "ANT", "SGM", "TRY", "KMR", "AKM", "TKD");

    private static final int NAME_MAX = 240;

    private final ServiceEntityRepository serviceEntityRepository;
    private final IndividualServiceRepository individualServiceRepository;
    private final BranchIndividualServiceRepository branchServiceRepository;
    private final BookingIndividualLineRepository bookingLineRepository;
    private final IndividualServiceSourceRepository sourceRepository;
    private final ServiceCategoryJson json;

    @Transactional
    public void sync() {
        int removedMocks = dropMocks();
        List<Desired> desired = collapseDuplicateAz(collect(serviceEntityRepository.findAll()));
        Set<String> codes = desired.stream().map(Desired::code).collect(Collectors.toSet());
        int added = 0;
        int updated = 0;
        for (Desired item : desired) {
            IndividualService row = individualServiceRepository.findByCode(item.code()).orElse(null);
            if (row == null) {
                individualServiceRepository.save(IndividualService.builder()
                        .code(item.code())
                        .titleJson(json.write(item.az(), item.en(), item.ru()))
                        .sortOrder(item.sortOrder())
                        .active(true)
                        .build());
                added++;
                continue;
            }
            row.setTitleJson(json.write(item.az(), item.en(), item.ru()));
            row.setSortOrder(item.sortOrder());
            row.setActive(true);
            individualServiceRepository.save(row);
            updated++;
        }
        int deactivated = 0;
        for (IndividualService row : individualServiceRepository.findAllByOrderBySortOrderAscIdAsc()) {
            if (row.getCode() == null || codes.contains(row.getCode()) || MOCK_CODES.contains(row.getCode())) {
                continue;
            }
            if (!Boolean.TRUE.equals(row.getActive())) {
                continue;
            }
            row.setActive(false);
            individualServiceRepository.save(row);
            deactivated++;
        }
        individualServiceRepository.flush();
        int links = replaceSources(desired);
        log.info("INDIVIDUAL_CATALOG_SYNC added={} updated={} deactivated={} mocks={} links={}",
                added, updated, deactivated, removedMocks, links);
    }

    /**
     * tr: Cədvəli başdan yazır. Pasif kataloqun köhnə service_id-ləri qalmaz.
     * en: Rewrites the table. A deactivated catalog row does not keep old service ids.
     */
    private int replaceSources(List<Desired> desired) {
        sourceRepository.deleteAllInBatch();
        List<IndividualServiceSource> links = new ArrayList<>();
        if (desired != null) {
            for (Desired item : desired) {
                IndividualService row = individualServiceRepository.findByCode(item.code()).orElse(null);
                if (row == null || row.getId() == null || !Boolean.TRUE.equals(row.getActive())) {
                    continue;
                }
                for (Long serviceId : distinctIds(item.serviceIds())) {
                    links.add(IndividualServiceSource.builder()
                            .serviceId(serviceId)
                            .individualServiceId(row.getId())
                            .build());
                }
            }
        }
        if (!links.isEmpty()) {
            sourceRepository.saveAll(links);
        }
        return links.size();
    }

    private static List<Long> distinctIds(List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long serviceId : serviceIds) {
            if (serviceId != null) {
                seen.add(serviceId);
            }
        }
        return List.copyOf(seen);
    }

    /**
     * Same Azerbaijani name is one catalog row. The copy with fewer branch prices
     * and booking lines is deleted. A tie keeps the older source row.
     */
    private List<Desired> collapseDuplicateAz(List<Desired> desired) {
        Map<String, List<Desired>> byAz = new LinkedHashMap<>();
        List<Desired> passthrough = new ArrayList<>();
        for (Desired item : desired) {
            String az = normalize(item.az());
            if (az.isEmpty()) {
                passthrough.add(item);
                continue;
            }
            byAz.computeIfAbsent(az, ignored -> new ArrayList<>()).add(item);
        }
        List<Desired> kept = new ArrayList<>();
        for (List<Desired> group : byAz.values()) {
            if (group.size() == 1) {
                kept.add(group.get(0));
                continue;
            }
            Desired winner = pick(group);
            int winnerScore = usage(winner.code());
            kept.add(winner);
            for (Desired loser : group) {
                if (loser == winner) {
                    continue;
                }
                int loserScore = usage(loser.code());
                if (loserScore < winnerScore || (loserScore == 0 && winnerScore == 0)) {
                    takeIds(winner, loser.serviceIds());
                    dropCatalog(loser.code());
                    continue;
                }
                kept.add(loser);
            }
        }
        kept.addAll(passthrough);
        return kept;
    }

    private Desired pick(List<Desired> group) {
        Desired winner = group.get(0);
        int best = usage(winner.code());
        for (int i = 1; i < group.size(); i++) {
            Desired candidate = group.get(i);
            int score = usage(candidate.code());
            boolean unusedTie = score == 0 && best == 0 && candidate.sourceId() < winner.sourceId();
            if (score > best || unusedTie) {
                winner = candidate;
                best = score;
            }
        }
        return winner;
    }

    private int usage(String code) {
        IndividualService row = individualServiceRepository.findByCode(code).orElse(null);
        if (row == null || row.getId() == null) {
            return 0;
        }
        long prices = branchServiceRepository.countByIndividualService_Id(row.getId());
        long bookings = bookingLineRepository.countByIndividualServiceId(row.getId());
        long total = prices + bookings;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private void dropCatalog(String code) {
        IndividualService row = individualServiceRepository.findByCode(code).orElse(null);
        if (row == null || row.getId() == null) {
            return;
        }
        branchServiceRepository.deleteByIndividualService_Id(row.getId());
        individualServiceRepository.delete(row);
        log.info("INDIVIDUAL_CATALOG_DROP_DUPLICATE code={}", code);
    }

    private int dropMocks() {
        int removed = 0;
        for (String code : MOCK_CODES) {
            IndividualService row = individualServiceRepository.findByCode(code).orElse(null);
            if (row == null || row.getId() == null) {
                continue;
            }
            branchServiceRepository.deleteByIndividualService_Id(row.getId());
            individualServiceRepository.delete(row);
            removed++;
        }
        individualServiceRepository.flush();
        return removed;
    }

    /**
     * tr: Aralık km/ay ayrı xidmət deyil. Boş üç ad atlanır. Sıra Azərbaycan adına göredir.
     * en: Km/month interval is not a separate service. A blank triple is skipped. Order follows the Azerbaijani name.
     */
    static List<Desired> collect(List<ServiceEntity> rows) {
        Map<String, Desired> grouped = new LinkedHashMap<>();
        if (rows != null) {
            for (ServiceEntity row : rows) {
                String az = clip(row.getNameAz());
                String en = clip(row.getNameEn());
                String ru = clip(row.getNameRu());
                String key = key(az, en, ru);
                if (key.isEmpty() || row.getId() == null) {
                    continue;
                }
                Desired current = grouped.get(key);
                if (current == null) {
                    grouped.put(key, new Desired(null, 0, az, en, ru, row.getId(), key, idsOf(row.getId())));
                    continue;
                }
                List<Long> merged = new ArrayList<>(current.serviceIds());
                merged.add(row.getId());
                if (row.getId() < current.sourceId()) {
                    grouped.put(key, new Desired(null, 0, az, en, ru, row.getId(), key, merged));
                } else {
                    grouped.put(key, new Desired(null, 0, current.az(), current.en(), current.ru(),
                            current.sourceId(), key, merged));
                }
            }
        }
        List<Desired> sorted = new ArrayList<>(grouped.values());
        sorted.sort(Comparator
                .comparing((Desired item) -> normalize(item.az()))
                .thenComparing(item -> normalize(item.en()))
                .thenComparing(item -> normalize(item.ru()))
                .thenComparingLong(Desired::sourceId));
        List<Desired> out = new ArrayList<>();
        Set<String> used = new HashSet<>();
        int sort = 1;
        for (Desired item : sorted) {
            String code = codeFor(item.key(), used);
            out.add(new Desired(code, sort, item.az(), item.en(), item.ru(), item.sourceId(), item.key(),
                    new ArrayList<>(item.serviceIds())));
            sort++;
        }
        return out;
    }

    static String codeOf(String az, String en, String ru) {
        String key = key(clip(az), clip(en), clip(ru));
        if (key.isEmpty()) {
            return "";
        }
        return codeFor(key, new HashSet<>());
    }

    static String codeFor(String key, Set<String> used) {
        byte[] hash = sha256(key);
        for (int offset = 0; offset + 4 <= hash.length; offset++) {
            String code = HexFormat.of().withUpperCase().formatHex(hash, offset, offset + 4);
            if (used.add(code)) {
                return code;
            }
        }
        throw new IllegalStateException("individual service code collision");
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT).replace("\u0307", "");
        return lower.replaceAll("\\s+", "");
    }

    private static String key(String az, String en, String ru) {
        String normalized = normalize(az) + "\u0000" + normalize(en) + "\u0000" + normalize(ru);
        if (normalize(az).isEmpty() && normalize(en).isEmpty() && normalize(ru).isEmpty()) {
            return "";
        }
        return normalized;
    }

    private static String clip(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= NAME_MAX ? trimmed : trimmed.substring(0, NAME_MAX);
    }

    private static byte[] sha256(String key) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256", ex);
        }
    }

    private static void takeIds(Desired winner, List<Long> extra) {
        if (extra == null || extra.isEmpty()) {
            return;
        }
        List<Long> ids = winner.serviceIds();
        for (Long id : extra) {
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
    }

    private static List<Long> idsOf(Long serviceId) {
        List<Long> ids = new ArrayList<>();
        ids.add(serviceId);
        return ids;
    }

    record Desired(String code, int sortOrder, String az, String en, String ru, long sourceId, String key,
                   List<Long> serviceIds) {}
}
