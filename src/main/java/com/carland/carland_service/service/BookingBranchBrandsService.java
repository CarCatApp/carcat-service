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
 * tr: Müşteri şube marka çipleri. Boş başlık dönmez.
 * en: Owner branch brand chips. Empty headings are omitted.
 */
@Service
@RequiredArgsConstructor
public class BookingBranchBrandsService {

    private final BranchRepository branchRepository;
    private final BrandModelServiceRepository brandModelServiceRepository;
    private final BrandModelRepository brandModelRepository;

    @Transactional(readOnly = true)
    public BookingBrandModelsResponse list(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }

        List<BrandModelService> headings =
                brandModelServiceRepository.findByBranch_IdOrderBySortOrderAscIdAsc(branchId);
        if (headings.isEmpty()) {
            return BookingBrandModelsResponse.builder().branchId(branchId).groups(List.of()).build();
        }

        List<Long> headingIds = headings.stream().map(BrandModelService::getId).toList();
        Map<Long, List<BookingBrandChipView>> brandsByHeading = new LinkedHashMap<>();
        for (BrandModel model : brandModelRepository.findByBrandModelService_IdInOrderByIdAsc(headingIds)) {
            if (model.getBrandModelService() == null || model.getBrandModelService().getId() == null) {
                continue;
            }
            brandsByHeading
                    .computeIfAbsent(model.getBrandModelService().getId(), id -> new ArrayList<>())
                    .add(BookingBrandChipView.builder().id(model.getId()).name(model.getName()).build());
        }

        List<BookingBrandModelGroupView> groups = new ArrayList<>();
        for (BrandModelService heading : headings) {
            List<BookingBrandChipView> brands = brandsByHeading.get(heading.getId());
            if (brands == null || brands.isEmpty()) {
                continue;
            }
            groups.add(BookingBrandModelGroupView.builder()
                    .id(heading.getId())
                    .title(heading.getTitle())
                    .brands(brands)
                    .build());
        }
        return BookingBrandModelsResponse.builder().branchId(branchId).groups(groups).build();
    }
}
