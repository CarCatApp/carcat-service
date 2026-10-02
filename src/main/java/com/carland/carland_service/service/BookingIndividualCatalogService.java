package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServiceView;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
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
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BookingIndividualServicesResponse list(Long branchId, String acceptLanguage) {
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
        List<BookingIndividualServiceView> services = new ArrayList<>();
        for (BranchIndividualService row : rows) {
            IndividualService catalog = row.getIndividualService();
            if (!Boolean.TRUE.equals(row.getActive())
                    || catalog == null
                    || !Boolean.TRUE.equals(catalog.getActive())) {
                continue;
            }
            int[] qepik = BookingSelectionViews.qepik(row.getPriceSimple(), row.getPriceMedium(), row.getPriceComplex());
            services.add(BookingIndividualServiceView.builder()
                    .id(catalog.getId())
                    .code(catalog.getCode())
                    .name(BookingMineService.catalogText(titles(catalog.getTitleJson()), lang))
                    .priceMin(qepik == null ? null : qepik[0])
                    .priceMax(qepik == null ? null : qepik[1])
                    .currency("AZN")
                    .unit(BookingCreateService.UNIT)
                    .build());
        }
        return BookingIndividualServicesResponse.builder().branchId(branchId).services(services).build();
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
