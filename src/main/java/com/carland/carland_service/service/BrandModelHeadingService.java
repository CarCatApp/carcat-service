package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminBrandModelHeadingSaveRequest;
import com.carland.carland_service.dto.response.AdminBrandModelHeadingRow;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * tr: Ortak marka başlıkları. Silmek, başlığın altındaki bütün şube ürünlerini de siler.
 * en: Shared brand headings. Deleting a heading also deletes every branch product under it.
 */
@Service
@RequiredArgsConstructor
public class BrandModelHeadingService {

    private final BrandModelServiceRepository brandModelServiceRepository;
    private final BrandModelRepository brandModelRepository;
    private final ServiceCategoryJson json;

    @Transactional(readOnly = true)
    public List<AdminBrandModelHeadingRow> rows() {
        List<AdminBrandModelHeadingRow> rows = new ArrayList<>();
        for (BrandModelService heading : brandModelServiceRepository.findAllByOrderBySortOrderAscIdAsc()) {
            rows.add(toRow(heading));
        }
        return rows;
    }

    @Transactional
    public AdminBrandModelHeadingRow save(AdminBrandModelHeadingSaveRequest body) {
        if (body == null) {
            throw MissingFieldException.required("titleAz");
        }
        String az = required(body.getTitleAz(), "titleAz");
        String en = required(body.getTitleEn(), "titleEn");
        String ru = required(body.getTitleRu(), "titleRu");
        BrandModelService heading;
        if (body.getId() == null) {
            heading = new BrandModelService();
        } else {
            heading = brandModelServiceRepository.findById(body.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("brand model service not found"));
        }
        heading.setTitleJson(json.write(az, en, ru));
        heading.setOil(Boolean.TRUE.equals(body.getOil()));
        heading.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        return toRow(brandModelServiceRepository.save(heading));
    }

    @Transactional
    public void delete(Long id) {
        BrandModelService heading = brandModelServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("brand model service not found"));
        brandModelRepository.deleteByBrandModelService_Id(heading.getId());
        brandModelServiceRepository.delete(heading);
    }

    private AdminBrandModelHeadingRow toRow(BrandModelService heading) {
        Map<String, String> titles = json.read(heading.getTitleJson());
        return AdminBrandModelHeadingRow.builder()
                .id(heading.getId())
                .titleAz(titles.getOrDefault("az", ""))
                .titleEn(titles.getOrDefault("en", ""))
                .titleRu(titles.getOrDefault("ru", ""))
                .sortOrder(heading.getSortOrder())
                .oil(Boolean.TRUE.equals(heading.getOil()))
                .build();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw MissingFieldException.required(field);
        }
        String trimmed = value.trim();
        if (trimmed.length() > 200) {
            throw new MissingFieldException(field + " is too long");
        }
        return trimmed;
    }
}
