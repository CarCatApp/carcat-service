package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCatalogDirectionView;
import com.carland.carland_service.dto.booking.BookingCatalogItemView;
import com.carland.carland_service.dto.booking.BookingCatalogResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPackage;
import com.carland.carland_service.entity.BranchService;
import com.carland.carland_service.entity.BranchServiceBrand;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchPackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceBrandRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * tr: Müşteri şube menüsü (CRCT-282). remainingPercent yok — 283.
 * en: Owner-app branch catalog (CRCT-282). remainingPercent is 283.
 */
@Service
@RequiredArgsConstructor
public class BookingCatalogService {

    static final String KIND_DIRECTION = "DIRECTION";
    static final String DURATION_NOTE = "Estimate only. Bookable window is the slot length (30 min).";

    private final BranchRepository branchRepository;
    private final BranchPackageRepository packageRepository;
    private final BranchServiceRepository serviceRepository;
    private final BranchServiceBrandRepository brandRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BookingCatalogResponse catalog(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }

        List<BranchService> rows = serviceRepository.findByBranchIdAndActiveTrueOrderByIdAsc(branchId);
        Map<Long, List<String>> brandsByService = brandsByService(rows);

        List<BookingCatalogDirectionView> directions = new ArrayList<>();
        List<BookingCatalogItemView> services = new ArrayList<>();
        for (BranchService row : rows) {
            if (KIND_DIRECTION.equalsIgnoreCase(row.getKind())) {
                directions.add(BookingCatalogDirectionView.builder()
                        .serviceKey(row.getServiceKey())
                        .active(true)
                        .title(titles(row.getTitleJson()))
                        .build());
                continue;
            }
            services.add(BookingCatalogItemView.builder()
                    .serviceKey(row.getServiceKey())
                    .branchId(branchId)
                    .type("service")
                    .title(titles(row.getTitleJson()))
                    .durationMin(row.getDurationMin())
                    .priceMin(row.getPriceMin())
                    .priceMax(row.getPriceMax())
                    .currency(row.getCurrency() == null ? "AZN" : row.getCurrency())
                    .unit("qepik")
                    .brands(brandsByService.getOrDefault(row.getId(), List.of()))
                    .active(true)
                    .build());
        }

        List<BookingCatalogItemView> packages = new ArrayList<>();
        for (BranchPackage pkg : packageRepository.findByBranchIdAndActiveTrueOrderByIdAsc(branchId)) {
            packages.add(BookingCatalogItemView.builder()
                    .serviceKey(pkg.getServiceKey())
                    .branchId(branchId)
                    .type("package")
                    .title(titles(pkg.getTitleJson()))
                    .durationMin(pkg.getDurationMin())
                    .durationNote(DURATION_NOTE)
                    .priceMin(pkg.getPriceMin())
                    .priceMax(pkg.getPriceMax())
                    .currency(pkg.getCurrency() == null ? "AZN" : pkg.getCurrency())
                    .unit("qepik")
                    .includedServiceKeys(stringList(pkg.getIncludedServiceKeys()))
                    .active(true)
                    .build());
        }

        return BookingCatalogResponse.builder()
                .branchId(branchId)
                .directions(directions)
                .packages(packages)
                .services(services)
                .build();
    }

    private Map<Long, List<String>> brandsByService(List<BranchService> rows) {
        List<Long> ids = rows.stream().map(BranchService::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return brandRepository.findByBranchService_IdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        b -> b.getBranchService().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(BranchServiceBrand::getBrand, Collectors.toList())
                ));
    }

    private Map<String, String> titles(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception ex) {
            return Map.of("az", json);
        }
    }

    private List<String> stringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
