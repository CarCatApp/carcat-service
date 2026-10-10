package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBrandChipView;
import com.carland.carland_service.dto.booking.BookingBrandModelGroupView;
import com.carland.carland_service.dto.booking.BookingBrandModelsResponse;
import com.carland.carland_service.entity.BrandModel;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Müşteri şube markaları. Boş başlık dönmez. Başlık adı Accept-Language ilə seçilir.
 * en: Owner branch brands. Empty headings are omitted. The heading name follows Accept-Language.
 */
@Service
@RequiredArgsConstructor
public class BookingBranchBrandsService {

    private final BranchRepository branchRepository;
    private final BrandModelServiceRepository brandModelServiceRepository;
    private final BrandModelRepository brandModelRepository;
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

        List<BrandModelService> headings = brandModelServiceRepository.findAllByOrderBySortOrderAscIdAsc();
        if (headings.isEmpty()) {
            return BookingBrandModelsResponse.builder().branchId(branchId).groups(List.of()).build();
        }

        Map<Long, List<BookingBrandChipView>> brandsByHeading = new LinkedHashMap<>();
        for (BrandModel model : brandModelRepository.findByBranch_IdOrderByIdAsc(branchId)) {
            if (model.getBrandModelService() == null || model.getBrandModelService().getId() == null) {
                continue;
            }
            brandsByHeading
                    .computeIfAbsent(model.getBrandModelService().getId(), id -> new ArrayList<>())
                    .add(BookingBrandChipView.builder().id(model.getId()).name(model.getName()).build());
        }

        String lang = BookingMineService.langOf(acceptLanguage);
        List<BookingBrandModelGroupView> groups = new ArrayList<>();
        for (BrandModelService heading : headings) {
            List<BookingBrandChipView> brands = brandsByHeading.get(heading.getId());
            if (brands == null || brands.isEmpty()) {
                continue;
            }
            String title = BookingMineService.catalogText(serviceCategoryJson.read(heading.getTitleJson()), lang);
            groups.add(BookingBrandModelGroupView.builder()
                    .id(heading.getId())
                    .title(title == null ? "" : title)
                    .brands(brands)
                    .build());
        }
        return BookingBrandModelsResponse.builder().branchId(branchId).groups(groups).build();
    }
}
