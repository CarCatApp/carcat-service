package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServiceFilterView;
import com.carland.carland_service.dto.booking.BookingIndividualServiceFiltersResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServiceView;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * tr: Müşteriye şubenin açık fərdi xidmət listesi.
 * en: Active individual services of a branch for the owner app.
 */
@Service
@RequiredArgsConstructor
public class BookingIndividualCatalogService {

    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {};

    private final BranchRepository branchRepository;
    private final BranchIndividualServiceRepository branchIndividualServiceRepository;
    private final IndividualServiceFilterRepository individualServiceFilterRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BookingIndividualServiceFiltersResponse filters(String acceptLanguage) {
        String lang = BookingMineService.langOf(acceptLanguage);
        List<BookingIndividualServiceFilterView> filters = new ArrayList<>();
        for (IndividualServiceFilter row : individualServiceFilterRepository.findAllByOrderByIdAsc()) {
            filters.add(BookingIndividualServiceFilterView.builder()
                    .id(row.getId())
                    .name(localizedName(row, lang))
                    .build());
        }
        return BookingIndividualServiceFiltersResponse.builder().filters(filters).build();
    }

    @Transactional(readOnly = true)
    public BookingIndividualServicesResponse list(Long branchId, String acceptLanguage, String filter) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }
        String lang = BookingMineService.langOf(acceptLanguage);
        List<BranchIndividualService> found = branchIndividualServiceRepository.findByBranch_Id(branchId);
        List<BranchIndividualService> rows = new ArrayList<>(found == null ? List.of() : found);
        rows.sort(Comparator
                .comparing((BranchIndividualService row) -> sortOf(row.getIndividualService()))
                .thenComparing(row -> row.getIndividualService() == null ? 0L : row.getIndividualService().getId()));
        boolean all = isAll(filter);
        List<BookingIndividualServiceView> services = new ArrayList<>();
        for (BranchIndividualService row : rows) {
            IndividualService catalog = row.getIndividualService();
            if (!Boolean.TRUE.equals(row.getActive())
                    || catalog == null
                    || !Boolean.TRUE.equals(catalog.getActive())) {
                continue;
            }
            if (!all && !matches(catalog.getFilter(), filter)) {
                continue;
            }
            services.add(toView(catalog, row, lang));
        }
        return BookingIndividualServicesResponse.builder().branchId(branchId).services(services).build();
    }

    private BookingIndividualServiceView toView(IndividualService catalog, BranchIndividualService row, String lang) {
        int[] qepik = BookingSelectionViews.qepik(row.getPriceSimple(), row.getPriceMedium(), row.getPriceComplex());
        return BookingIndividualServiceView.builder()
                .id(catalog.getId())
                .code(catalog.getCode())
                .name(BookingMineService.catalogText(titles(catalog.getTitleJson()), lang))
                .priceMin(qepik == null ? null : qepik[0])
                .priceMax(qepik == null ? null : qepik[1])
                .currency("AZN")
                .unit(BookingCreateService.UNIT)
                .build();
    }

    private static boolean isAll(String filter) {
        return filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter.trim());
    }

    private static boolean matches(IndividualServiceFilter chip, String filter) {
        if (chip == null || chip.getId() == null) {
            return false;
        }
        String raw = filter.trim();
        if (raw.equals(String.valueOf(chip.getId()))) {
            return true;
        }
        return equalsName(chip.getNameAz(), raw)
                || equalsName(chip.getNameEn(), raw)
                || equalsName(chip.getNameRu(), raw);
    }

    private static boolean equalsName(String value, String raw) {
        return value != null && value.equalsIgnoreCase(raw);
    }

    private static String localizedName(IndividualServiceFilter row, String lang) {
        return BookingMineService.catalogText(Map.of(
                "az", row.getNameAz() == null ? "" : row.getNameAz(),
                "en", row.getNameEn() == null ? "" : row.getNameEn(),
                "ru", row.getNameRu() == null ? "" : row.getNameRu()
        ), lang);
    }

    private static int sortOf(IndividualService catalog) {
        if (catalog == null || catalog.getSortOrder() == null) {
            return 0;
        }
        return catalog.getSortOrder();
    }

    private Map<String, String> titles(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(json, MAP);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of("az", json);
        }
    }
}
