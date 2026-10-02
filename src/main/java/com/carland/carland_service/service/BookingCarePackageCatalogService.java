package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCarePackageServiceView;
import com.carland.carland_service.dto.booking.BookingCarePackageView;
import com.carland.carland_service.dto.booking.BookingCarePackagesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * tr: Müşteriye şubenin açık dövri qulluq paketleri. Fiyat manat.
 * en: Active routine-care packages of a branch for the owner app. Price is whole manat.
 */
@Service
@RequiredArgsConstructor
public class BookingCarePackageCatalogService {

    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {};

    private final BranchRepository branchRepository;
    private final BranchCarePackageRepository packageRepository;
    private final BranchCarePackageItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BookingCarePackagesResponse list(Long branchId, String acceptLanguage) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }
        String lang = BookingMineService.langOf(acceptLanguage);
        List<BookingCarePackageView> packages = new ArrayList<>();
        for (BranchCarePackage row : packageRepository.findByBranch_IdOrderByIdAsc(branchId)) {
            if (!Boolean.TRUE.equals(row.getActive())) {
                continue;
            }
            List<BookingCarePackageServiceView> services = servicesOf(row.getId(), lang);
            packages.add(BookingCarePackageView.builder()
                    .id(row.getId())
                    .name(row.getName())
                    .price(row.getPrice())
                    .currency(row.getCurrency() == null ? "AZN" : row.getCurrency())
                    .count(services.size())
                    .services(services)
                    .build());
        }
        return BookingCarePackagesResponse.builder().branchId(branchId).packages(packages).build();
    }

    private List<BookingCarePackageServiceView> servicesOf(Long packageId, String lang) {
        List<BranchCarePackageItem> items = itemRepository.findByCarePackage_IdOrderByIdAsc(packageId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<BookingCarePackageServiceView> services = new ArrayList<>();
        for (BranchCarePackageItem item : items) {
            OfferedService service = item.getOfferedService();
            if (!Boolean.TRUE.equals(item.getEnabled()) || service == null || service.getId() == null) {
                continue;
            }
            services.add(BookingCarePackageServiceView.builder()
                    .id(service.getId())
                    .name(BookingMineService.catalogText(titles(service.getTitleJson()), lang))
                    .build());
        }
        return services;
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
