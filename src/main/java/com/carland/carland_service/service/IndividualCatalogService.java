package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffIndividualServiceListResponse;
import com.carland.carland_service.dto.booking.StaffIndividualServiceView;
import com.carland.carland_service.dto.request.AdminIndividualServiceSaveRequest;
import com.carland.carland_service.dto.request.StaffIndividualServicePricesRequest;
import com.carland.carland_service.dto.response.AdminIndividualServiceRow;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * tr: Fərdi xidmət kataloğu adminde durur. Şube yalnızca kendi durumunu ve üç fiyatını yazar.
 * en: The individual-service catalog lives in admin. A branch writes only its own status and three prices.
 */
@Service
@RequiredArgsConstructor
public class IndividualCatalogService {

    private static final int PRICE_MAX = 1_000_000;
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9]{2,8}$");

    private final IndividualServiceRepository individualServiceRepository;
    private final IndividualServiceFilterRepository individualServiceFilterRepository;
    private final BranchIndividualServiceRepository branchServiceRepository;
    private final BranchRepository branchRepository;
    private final BookingStaffAccess bookingStaffAccess;
    private final ServiceCategoryJson json;

    /**
     * tr: Katalogda açık xidmətlər. Şube satırı yoksa kapalı ve fiyatsız gelir. Kapalı şube satırının fiyatı durur.
     * en: Services that are on in the catalog. A missing branch row is off and unpriced. An off branch row keeps its prices.
     */
    @Transactional(readOnly = true)
    public StaffIndividualServiceListResponse listForStaff(Long userId, boolean mustChangePassword, Long branchId,
                                                            String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, branchId, acceptLanguage);
        Map<Long, BranchIndividualService> byService = new HashMap<>();
        for (BranchIndividualService row : branchServiceRepository.findByBranch_Id(branch.getId())) {
            if (row.getIndividualService() != null && row.getIndividualService().getId() != null) {
                byService.put(row.getIndividualService().getId(), row);
            }
        }
        List<StaffIndividualServiceView> services = new ArrayList<>();
        for (IndividualService catalog : individualServiceRepository.findByActiveTrueOrderBySortOrderAscIdAsc()) {
            services.add(toStaffView(catalog, byService.get(catalog.getId())));
        }
        return StaffIndividualServiceListResponse.builder()
                .branchId(branch.getId())
                .services(services)
                .build();
    }

    /**
     * tr: Şube xidməti açar veya kapatır. Fiyat kolonlarına dokunmaz.
     * en: Turns the branch service on or off. Price columns are left as they are.
     */
    @Transactional
    public StaffIndividualServiceView setActive(Long userId, boolean mustChangePassword, Long serviceId,
                                                 Boolean active, String acceptLanguage) {
        if (active == null) {
            throw MissingFieldException.required("active");
        }
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, null, acceptLanguage);
        IndividualService catalog = requireOffered(serviceId);
        BranchIndividualService row = branchRow(branch, catalog);
        row.setActive(active);
        return toStaffView(catalog, branchServiceRepository.save(row));
    }

    /**
     * tr: Üç fiyatı yazar. Şube durumunu değiştirmez. Boş alan o seviyeyi temizler.
     * en: Writes the three prices. Branch status stays. A null field clears that tier.
     */
    @Transactional
    public StaffIndividualServiceView updatePrices(Long userId, boolean mustChangePassword, Long serviceId,
                                                    StaffIndividualServicePricesRequest body, String acceptLanguage) {
        if (body == null) {
            throw MissingFieldException.required("priceSimple");
        }
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, null, acceptLanguage);
        IndividualService catalog = requireOffered(serviceId);
        BranchIndividualService row = branchRow(branch, catalog);
        row.setPriceSimple(price(body.getPriceSimple(), "priceSimple"));
        row.setPriceMedium(price(body.getPriceMedium(), "priceMedium"));
        row.setPriceComplex(price(body.getPriceComplex(), "priceComplex"));
        return toStaffView(catalog, branchServiceRepository.save(row));
    }

    @Transactional(readOnly = true)
    public List<IndividualServiceFilter> adminFilters() {
        return individualServiceFilterRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public List<AdminIndividualServiceRow> adminRows() {
        List<AdminIndividualServiceRow> rows = new ArrayList<>();
        for (IndividualService catalog : individualServiceRepository.findAllByOrderBySortOrderAscIdAsc()) {
            rows.add(toAdminRow(catalog));
        }
        return rows;
    }

    /**
     * tr: Katalog satırı oluşturur veya günceller. Şube fiyatlarına dokunmaz.
     * en: Creates or updates a catalog row. Branch prices are untouched.
     */
    @Transactional
    public AdminIndividualServiceRow saveAdmin(AdminIndividualServiceSaveRequest body) {
        if (body == null || body.getTitleAz() == null || body.getTitleAz().isBlank()) {
            throw MissingFieldException.required("titleAz");
        }
        String code = requireCode(body.getCode());
        IndividualService row = body.getId() == null
                ? new IndividualService()
                : individualServiceRepository.findById(body.getId())
                .orElseThrow(() -> new ResourceNotFoundException("individual service not found"));
        individualServiceRepository.findByCode(code).ifPresent(existing -> {
            if (row.getId() == null || !row.getId().equals(existing.getId())) {
                throw new ConflictException("code already exists");
            }
        });
        row.setCode(code);
        row.setTitleJson(json.write(body.getTitleAz().trim(), trim(body.getTitleEn()), trim(body.getTitleRu())));
        row.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        row.setActive(body.getActive() == null || body.getActive());
        if (body.getFilterId() == null) {
            throw MissingFieldException.required("filterId");
        }
        IndividualServiceFilter filter = individualServiceFilterRepository.findById(body.getFilterId())
                .orElseThrow(() -> new ResourceNotFoundException("individual service filter not found"));
        row.setFilter(filter);
        return toAdminRow(individualServiceRepository.save(row));
    }

    private BranchIndividualService branchRow(Branch branch, IndividualService catalog) {
        return branchServiceRepository
                .findByBranch_IdAndIndividualService_Id(branch.getId(), catalog.getId())
                .orElseGet(() -> BranchIndividualService.builder()
                        .branch(branch)
                        .individualService(catalog)
                        .active(false)
                        .build());
    }

    private IndividualService requireOffered(Long serviceId) {
        IndividualService catalog = individualServiceRepository.findById(serviceId == null ? -1L : serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("individual service not found"));
        if (!Boolean.TRUE.equals(catalog.getActive())) {
            throw new ResourceNotFoundException("individual service not found");
        }
        return catalog;
    }

    private StaffIndividualServiceView toStaffView(IndividualService catalog, BranchIndividualService row) {
        return StaffIndividualServiceView.builder()
                .id(catalog.getId())
                .code(catalog.getCode())
                .title(json.read(catalog.getTitleJson()))
                .active(row != null && Boolean.TRUE.equals(row.getActive()))
                .priceSimple(row == null ? null : row.getPriceSimple())
                .priceMedium(row == null ? null : row.getPriceMedium())
                .priceComplex(row == null ? null : row.getPriceComplex())
                .build();
    }

    private AdminIndividualServiceRow toAdminRow(IndividualService catalog) {
        Map<String, String> title = json.read(catalog.getTitleJson());
        return AdminIndividualServiceRow.builder()
                .id(catalog.getId())
                .code(catalog.getCode())
                .titleAz(title.getOrDefault("az", ""))
                .titleEn(title.getOrDefault("en", ""))
                .titleRu(title.getOrDefault("ru", ""))
                .sortOrder(catalog.getSortOrder())
                .active(catalog.getActive())
                .filterId(catalog.getFilter() == null ? null : catalog.getFilter().getId())
                .filterName(catalog.getFilter() == null ? null : catalog.getFilter().getNameEn())
                .build();
    }

    private Branch resolveBranch(BookingStaff staff, Long branchId, String acceptLanguage) {
        if (branchId != null) {
            return bookingStaffAccess.requireWritableBranch(staff, branchId, acceptLanguage);
        }
        if (BookingStaffRole.BRANCH_ADMIN.name().equals(staff.getRole())) {
            if (staff.getBranch() == null) {
                throw new ForbiddenException("branch required");
            }
            return staff.getBranch();
        }
        List<Branch> branches = branchRepository.findByPartnerOrderByIdAsc(staff.getPartner());
        if (branches.isEmpty()) {
            throw new ResourceNotFoundException("branch not found");
        }
        return branches.get(0);
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw MissingFieldException.required("code");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(normalized).matches()) {
            throw new MissingFieldException("code is invalid");
        }
        return normalized;
    }

    private static Integer price(Integer value, String field) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > PRICE_MAX) {
            throw new MissingFieldException(field + " is invalid");
        }
        return value;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
