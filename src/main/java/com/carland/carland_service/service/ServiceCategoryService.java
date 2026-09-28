package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffCatalogCategoryListResponse;
import com.carland.carland_service.dto.booking.StaffCatalogCategoryView;
import com.carland.carland_service.dto.request.AdminServiceCategorySaveRequest;
import com.carland.carland_service.dto.response.AdminServiceCategoryRow;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchService;
import com.carland.carland_service.entity.BranchServiceCategory;
import com.carland.carland_service.entity.ServiceCategory;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServiceCategoryRepository;
import com.carland.carland_service.repository.BranchServiceRepository;
import com.carland.carland_service.repository.ServiceCategoryPhotoRepository;
import com.carland.carland_service.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * tr: Hizmet kategorisi: admin kaydı, staff listesi ve şube aç/kapa. Global kapalı kategori staff'a çıkmaz.
 * en: Service categories: admin save, staff list, and per-branch toggle. A globally inactive category is hidden from staff.
 */
@Service
@RequiredArgsConstructor
public class ServiceCategoryService {

    private static final Pattern CODE = Pattern.compile("^[a-z0-9_]+$");

    private final ServiceCategoryRepository categoryRepository;
    private final BranchServiceCategoryRepository branchCategoryRepository;
    private final ServiceCategoryPhotoRepository photoRepository;
    private final BranchServiceRepository branchServiceRepository;
    private final BranchRepository branchRepository;
    private final BookingStaffAccess bookingStaffAccess;
    private final ServiceCategoryJson json;

    /**
     * tr: Staff'ın göreceği aktif kategoriler. branchId yoksa rolüne göre şube seçilir.
     * en: Active categories for a staff member. When branchId is omitted the branch is chosen from the role.
     */
    @Transactional(readOnly = true)
    public StaffCatalogCategoryListResponse list(Long userId, boolean mustChangePassword, Long branchId,
                                                 String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, branchId, acceptLanguage);
        Map<Long, Boolean> flags = branchFlags(branch.getId());
        List<StaffCatalogCategoryView> items = new ArrayList<>();
        for (ServiceCategory category : categoryRepository.findByActiveTrueOrderBySortOrderAscIdAsc()) {
            items.add(toView(category, effectiveActive(category, flags)));
        }
        return StaffCatalogCategoryListResponse.builder()
                .branchId(branch.getId())
                .items(items)
                .build();
    }

    /**
     * tr: Toggleable kategoriyi şube için açar/kapatır ve directionKeys satırlarını aynı değere çeker.
     * en: Turns a toggleable category on or off for a branch and sets matching directionKeys rows to the same value.
     */
    @Transactional
    public StaffCatalogCategoryView toggle(Long userId, boolean mustChangePassword, Long categoryId,
                                           Long branchId, Boolean active, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        if (active == null) {
            throw MissingFieldException.required("active");
        }
        Branch branch = resolveBranch(staff, branchId, acceptLanguage);
        ServiceCategory category = categoryRepository.findById(categoryId)
                .filter(row -> Boolean.TRUE.equals(row.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("category not found"));
        if (!Boolean.TRUE.equals(category.getToggleable())) {
            throw new ConflictException("category is not toggleable");
        }
        BranchServiceCategory row = branchCategoryRepository
                .findByBranch_IdAndCategory_Id(branch.getId(), category.getId())
                .orElseGet(() -> BranchServiceCategory.builder()
                        .branch(branch)
                        .category(category)
                        .build());
        row.setActive(active);
        branchCategoryRepository.save(row);
        syncDirections(branch, category, active);
        return toView(category, active);
    }

    /**
     * tr: Admin tablo satırları. Pasif kategoriler de gelir.
     * en: Admin table rows. Inactive categories are included.
     */
    @Transactional(readOnly = true)
    public List<AdminServiceCategoryRow> adminRows() {
        List<AdminServiceCategoryRow> rows = new ArrayList<>();
        for (ServiceCategory category : categoryRepository.findAllByOrderBySortOrderAscIdAsc()) {
            rows.add(toAdminRow(category));
        }
        return rows;
    }

    /**
     * tr: id null ise oluşturur (code zorunlu ve tekil). id varsa code değişmez.
     * en: Null id creates a row (code required and unique). An existing id does not change code.
     */
    @Transactional
    public AdminServiceCategoryRow save(AdminServiceCategorySaveRequest body) {
        if (body == null || body.getTitleAz() == null || body.getTitleAz().isBlank()) {
            throw MissingFieldException.required("titleAz");
        }
        ServiceCategory category;
        if (body.getId() == null) {
            String code = normalizeCode(body.getCode());
            if (categoryRepository.existsByCode(code)) {
                throw new ConflictException("code already exists");
            }
            category = ServiceCategory.builder()
                    .code(code)
                    .iconVersion(0)
                    .build();
        } else {
            category = categoryRepository.findById(body.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("category not found"));
        }
        category.setTitleJson(json.write(body.getTitleAz().trim(), trimToEmpty(body.getTitleEn()), trimToEmpty(body.getTitleRu())));
        category.setDescriptionJson(json.writeOrNull(body.getDescriptionAz(), body.getDescriptionEn(), body.getDescriptionRu()));
        category.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        category.setOpenable(body.getOpenable() == null ? Boolean.FALSE : body.getOpenable());
        category.setToggleable(body.getToggleable() == null ? Boolean.TRUE : body.getToggleable());
        category.setActive(body.getActive() == null ? Boolean.TRUE : body.getActive());
        category.setDirectionKeys(normalizeDirectionKeys(body.getDirectionKeys()));
        return toAdminRow(categoryRepository.save(category));
    }

    private void syncDirections(Branch branch, ServiceCategory category, boolean active) {
        for (String key : splitDirectionKeys(category.getDirectionKeys())) {
            BranchService service = branchServiceRepository.findByBranch_IdAndServiceKey(branch.getId(), key)
                    .orElseGet(() -> BranchService.builder()
                            .branch(branch)
                            .serviceKey(key)
                            .kind("DIRECTION")
                            .titleJson(category.getTitleJson())
                            .currency("AZN")
                            .build());
            service.setActive(active);
            branchServiceRepository.save(service);
        }
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

    private Map<Long, Boolean> branchFlags(Long branchId) {
        Map<Long, Boolean> flags = new HashMap<>();
        for (BranchServiceCategory row : branchCategoryRepository.findByBranch_Id(branchId)) {
            if (row.getCategory() != null && row.getCategory().getId() != null) {
                flags.put(row.getCategory().getId(), row.getActive());
            }
        }
        return flags;
    }

    private static boolean effectiveActive(ServiceCategory category, Map<Long, Boolean> flags) {
        if (!Boolean.TRUE.equals(category.getToggleable())) {
            return true;
        }
        return Boolean.TRUE.equals(flags.get(category.getId()));
    }

    private StaffCatalogCategoryView toView(ServiceCategory category, boolean activeForBranch) {
        boolean hasIcon = photoRepository.existsByCategoryId(category.getId());
        int version = category.getIconVersion() == null ? 0 : category.getIconVersion();
        return StaffCatalogCategoryView.builder()
                .id(category.getId())
                .code(category.getCode())
                .title(json.read(category.getTitleJson()))
                .description(json.read(category.getDescriptionJson()))
                .iconUrl(hasIcon
                        ? "/api/v1/photo/for/service-category/get?categoryId=" + category.getId() + "&v=" + version
                        : null)
                .openable(Boolean.TRUE.equals(category.getOpenable()))
                .toggleable(Boolean.TRUE.equals(category.getToggleable()))
                .active(activeForBranch)
                .sortOrder(category.getSortOrder())
                .build();
    }

    private AdminServiceCategoryRow toAdminRow(ServiceCategory category) {
        Map<String, String> title = json.read(category.getTitleJson());
        Map<String, String> description = json.read(category.getDescriptionJson());
        return AdminServiceCategoryRow.builder()
                .id(category.getId())
                .code(category.getCode())
                .titleAz(title.get("az"))
                .titleEn(title.get("en"))
                .titleRu(title.get("ru"))
                .descriptionAz(description.get("az"))
                .descriptionEn(description.get("en"))
                .descriptionRu(description.get("ru"))
                .sortOrder(category.getSortOrder())
                .openable(category.getOpenable())
                .toggleable(category.getToggleable())
                .active(category.getActive())
                .directionKeys(category.getDirectionKeys())
                .iconVersion(category.getIconVersion())
                .hasIcon(photoRepository.existsByCategoryId(category.getId()))
                .build();
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw MissingFieldException.required("code");
        }
        String trimmed = code.trim();
        if (trimmed.length() > 64 || !CODE.matcher(trimmed).matches()) {
            throw new MissingFieldException("code must match ^[a-z0-9_]+$");
        }
        return trimmed;
    }

    private static String normalizeDirectionKeys(String raw) {
        List<String> keys = splitDirectionKeys(raw);
        if (keys.isEmpty()) {
            return null;
        }
        String joined = String.join(",", keys);
        if (joined.length() > 256) {
            throw new MissingFieldException("directionKeys is too long");
        }
        return joined;
    }

    private static List<String> splitDirectionKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (String part : raw.split(",")) {
            String key = part.trim();
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        return keys;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
