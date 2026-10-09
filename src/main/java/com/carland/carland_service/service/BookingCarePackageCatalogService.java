package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingCarePackageGroupView;
import com.carland.carland_service.dto.booking.BookingCarePackageServiceView;
import com.carland.carland_service.dto.booking.BookingCarePackageView;
import com.carland.carland_service.dto.booking.BookingCarePackagesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.ServiceBehavior;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.OfferedServicePhotoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * tr: Müşteriye şubenin açık dövri qulluq paketleri. Fiyat manat.
 * en: Active routine-care packages of a branch for the owner app. Price is whole manat.
 */
@Service
@RequiredArgsConstructor
public class BookingCarePackageCatalogService {

    static final String ICON_PATH = "/api/v1/photo/for/offered-service/get?offeredServiceId=";

    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {};

    private final BranchRepository branchRepository;
    private final BranchCarePackageRepository packageRepository;
    private final BranchCarePackageItemRepository itemRepository;
    private final OfferedServicePhotoRepository photoRepository;
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
            List<BookingCarePackageGroupView> groups = groupsOf(row.getId(), lang);
            int count = 0;
            for (BookingCarePackageGroupView group : groups) {
                count += group.getServices().size();
            }
            packages.add(BookingCarePackageView.builder()
                    .id(row.getId())
                    .name(row.getName())
                    .price(row.getPrice())
                    .currency(row.getCurrency() == null ? "AZN" : row.getCurrency())
                    .count(count)
                    .groups(groups)
                    .build());
        }
        return BookingCarePackagesResponse.builder().branchId(branchId).packages(packages).build();
    }

    private List<BookingCarePackageGroupView> groupsOf(Long packageId, String lang) {
        List<BranchCarePackageItem> items = itemRepository.findByCarePackage_IdOrderByIdAsc(packageId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<Line> lines = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (BranchCarePackageItem item : items) {
            OfferedService service = item.getOfferedService();
            if (!Boolean.TRUE.equals(item.getEnabled()) || service == null || service.getId() == null) {
                continue;
            }
            ServiceBehavior behavior = service.getBehavior();
            if (behavior == null || behavior.getId() == null) {
                continue;
            }
            lines.add(new Line(service, behavior));
            ids.add(service.getId());
        }
        if (lines.isEmpty()) {
            return List.of();
        }
        lines.sort(Comparator
                .comparingInt((Line line) -> sortOf(line.behavior().getSortOrder()))
                .thenComparing(line -> line.behavior().getId())
                .thenComparingInt(line -> sortOf(line.service().getSortOrder()))
                .thenComparing(line -> line.service().getId()));
        Set<Long> withIcon = new HashSet<>(idsWithImage(ids));
        Map<Long, BookingCarePackageGroupView> groups = new LinkedHashMap<>();
        for (Line line : lines) {
            ServiceBehavior behavior = line.behavior();
            BookingCarePackageGroupView group = groups.computeIfAbsent(behavior.getId(), id ->
                    BookingCarePackageGroupView.builder()
                            .id(id)
                            .name(BookingMineService.catalogText(titles(behavior.getTitleJson()), lang))
                            .services(new ArrayList<>())
                            .build());
            Long serviceId = line.service().getId();
            group.getServices().add(BookingCarePackageServiceView.builder()
                    .id(serviceId)
                    .name(BookingMineService.catalogText(titles(line.service().getTitleJson()), lang))
                    .iconUrl(withIcon.contains(serviceId) ? ICON_PATH + serviceId : null)
                    .build());
        }
        return new ArrayList<>(groups.values());
    }

    private List<Long> idsWithImage(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> found = photoRepository.findOfferedServiceIdsWithImage(ids);
        return found == null ? List.of() : found;
    }

    private static int sortOf(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private record Line(OfferedService service, ServiceBehavior behavior) {
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
