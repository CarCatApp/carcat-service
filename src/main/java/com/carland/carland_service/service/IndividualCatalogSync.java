package com.carland.carland_service.service;

import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tr: Fərdi kataloq, percentage'nin kopyalandığı services satırlarından gelir.
 *     Üç dil adı (boşluk və böyük hərf sayılmaz) aynıysa tek satır. Mock MYF…TKD silinir.
 *     Şablondan düşen ad pasif kalır, satır və şube fiyatı silinmez.
 * en: The individual catalog is the distinct services rows percentages are copied from.
 *     One row when the three names match ignoring space and case. Mock MYF…TKD codes are removed.
 *     A name that leaves the templates stays inactive; the row and branch prices stay.
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
    private final ServiceCategoryJson json;

    @Transactional
    public void sync() {
        List<Desired> desired = collect(serviceEntityRepository.findAll());
        int removedMocks = dropMocks();
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
        log.info("INDIVIDUAL_CATALOG_SYNC added={} updated={} deactivated={} mocks={}",
                added, updated, deactivated, removedMocks);
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
                if (current == null || row.getId() < current.sourceId()) {
                    grouped.put(key, new Desired(null, 0, az, en, ru, row.getId(), key));
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
            out.add(new Desired(code, sort, item.az(), item.en(), item.ru(), item.sourceId(), item.key()));
            sort++;
        }
        return out;
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

    record Desired(String code, int sortOrder, String az, String en, String ru, long sourceId, String key) {}
}
