package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBrandChipView;
import com.carland.carland_service.dto.booking.BookingBrandModelGroupView;
import com.carland.carland_service.dto.booking.BookingBrandModelsResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchServicedGood;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServicedGoodRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer-facing serviced goods. Only a brand name under a shared heading.
 * Empty headings are omitted.
 */
@Service
@RequiredArgsConstructor
public class BookingBranchBrandsService {

    private final BranchRepository branchRepository;
    private final BrandModelServiceRepository brandModelServiceRepository;
    private final BranchServicedGoodRepository servicedGoodRepository;
    private final ServiceCategoryJson serviceCategoryJson;

    @Transactional(readOnly = true)
    public BookingBrandModelsResponse list(Long branchId, String acceptLanguage) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }
        return groups(branchId, acceptLanguage, false);
    }

    BookingBrandModelsResponse groups(Long branchId, String acceptLanguage, boolean includeEmpty) {
        List<BrandModelService> headings = brandModelServiceRepository.findAllByOrderBySortOrderAscIdAsc();
        if (headings.isEmpty()) {
            return BookingBrandModelsResponse.builder().branchId(branchId).groups(List.of()).build();
        }
        Map<Long, List<BookingBrandChipView>> brandsByHeading = new LinkedHashMap<>();
        for (BranchServicedGood good : servicedGoodRepository.findByBranch_IdOrderByIdAsc(branchId)) {
            if (good.getBrandModelService() == null || good.getBrandModelService().getId() == null) {
                continue;
            }
            brandsByHeading
                    .computeIfAbsent(good.getBrandModelService().getId(), id -> new ArrayList<>())
                    .add(BookingBrandChipView.builder()
                            .id(good.getId())
                            .name(good.getName())
                            .price(includeEmpty ? good.getPrice() : null)
                            .build());
        }
        String lang = BookingMineService.langOf(acceptLanguage);
        List<BookingBrandModelGroupView> groups = new ArrayList<>();
        for (BrandModelService heading : headings) {
            List<BookingBrandChipView> brands = brandsByHeading.get(heading.getId());
            if (!includeEmpty && (brands == null || brands.isEmpty())) {
                continue;
            }
            String title = BookingMineService.catalogText(serviceCategoryJson.read(heading.getTitleJson()), lang);
            groups.add(BookingBrandModelGroupView.builder()
                    .id(heading.getId())
                    .title(title == null ? "" : title)
                    .brands(brands == null ? List.of() : brands)
                    .build());
        }
        return BookingBrandModelsResponse.builder().branchId(branchId).groups(groups).build();
    }
}
