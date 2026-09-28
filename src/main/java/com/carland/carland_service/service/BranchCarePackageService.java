package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffCarePackageItemView;
import com.carland.carland_service.dto.booking.StaffCarePackageListResponse;
import com.carland.carland_service.dto.booking.StaffCarePackageView;
import com.carland.carland_service.dto.request.StaffCarePackageSaveRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.ServiceBehavior;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * tr: Şube dövri qulluq paketleri. Liste şubeye aittir. Müşteri kataloğu branch_packages değişmez.
 * en: Branch routine-care packages. The list belongs to the branch. Customer-catalog branch_packages is untouched.
 */
@Service
@RequiredArgsConstructor
public class BranchCarePackageService {

    private static final int NAME_MAX = 80;
    private static final int PRICE_MAX = 1_000_000;

    private final BranchCarePackageRepository packageRepository;
    private final BranchCarePackageItemRepository itemRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final BranchRepository branchRepository;
    private final BookingStaffAccess bookingStaffAccess;
    private final ServiceCategoryJson json;

    /**
     * tr: Şubenin paketleri, pasif olanlar dahil. branchId yoksa rolün şubesi seçilir.
     * en: Packages for the branch, including inactive ones. When branchId is omitted the role picks the branch.
     */
    @Transactional(readOnly = true)
    public StaffCarePackageListResponse list(Long userId, boolean mustChangePassword, Long branchId,
                                              String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, branchId, acceptLanguage);
        List<StaffCarePackageView> packages = new ArrayList<>();
        for (BranchCarePackage row : packageRepository.findByBranch_IdOrderByIdAsc(branch.getId())) {
            packages.add(toView(row));
        }
        return StaffCarePackageListResponse.builder()
                .branchId(branch.getId())
                .packages(packages)
                .build();
    }

    /**
     * tr: Seçilen xidmətlerle paket oluşturur. Paket bu şubeye yazılır.
     * en: Creates a package from the selected services. The package is stored on this branch.
     */
    @Transactional
    public StaffCarePackageView create(Long userId, boolean mustChangePassword, StaffCarePackageSaveRequest body,
                                       String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        Branch branch = resolveBranch(staff, body.getBranchId(), acceptLanguage);
        String name = requireName(body.getName());
        int price = requirePrice(body.getPrice());
        Set<Long> serviceIds = requireServiceIds(body.getServiceIds());
        BranchCarePackage row = packageRepository.save(BranchCarePackage.builder()
                .branch(branch)
                .name(name)
                .price(price)
                .active(body.getActive() == null || body.getActive())
                .build());
        applyServices(row, serviceIds, false);
        return toView(row);
    }

    /**
     * tr: Ad, fiyat ve açık xidmətləri günceller. Listede olmayan eski satır enabled false olur.
     * en: Updates name, price, and the services switched on. Older lines missing from the list become enabled false.
     */
    @Transactional
    public StaffCarePackageView update(Long userId, boolean mustChangePassword, Long packageId,
                                       StaffCarePackageSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        Branch branch = resolveBranch(staff, body.getBranchId(), acceptLanguage);
        BranchCarePackage row = requirePackage(packageId, branch.getId());
        row.setName(requireName(body.getName()));
        row.setPrice(requirePrice(body.getPrice()));
        if (body.getActive() != null) {
            row.setActive(body.getActive());
        }
        packageRepository.save(row);
        applyServices(row, requireServiceIds(body.getServiceIds()), true);
        return toView(row);
    }

    /**
     * tr: Paketi açar veya kapatır. Xidmət satırları aynı kalır.
     * en: Turns the package on or off. Service lines stay unchanged.
     */
    @Transactional
    public StaffCarePackageView setActive(Long userId, boolean mustChangePassword, Long packageId,
                                          Boolean active, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        if (active == null) {
            throw MissingFieldException.required("active");
        }
        Branch branch = resolveBranch(staff, null, acceptLanguage);
        BranchCarePackage row = requirePackage(packageId, branch.getId());
        row.setActive(active);
        packageRepository.save(row);
        return toView(row);
    }

    private void applyServices(BranchCarePackage row, Set<Long> serviceIds, boolean keepMissingDisabled) {
        Map<Long, BranchCarePackageItem> existing = new HashMap<>();
        if (keepMissingDisabled) {
            for (BranchCarePackageItem item : itemRepository.findByCarePackage_IdOrderByIdAsc(row.getId())) {
                if (item.getOfferedService() != null && item.getOfferedService().getId() != null) {
                    existing.put(item.getOfferedService().getId(), item);
                }
            }
        }
        for (Long serviceId : serviceIds) {
            OfferedService service = offeredServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("service not found"));
            if (!Boolean.TRUE.equals(service.getActive())) {
                throw new ConflictException("service inactive");
            }
            BranchCarePackageItem item = existing.remove(serviceId);
            if (item == null) {
                item = BranchCarePackageItem.builder()
                        .carePackage(row)
                        .offeredService(service)
                        .enabled(true)
                        .build();
            } else {
                item.setEnabled(true);
            }
            itemRepository.save(item);
        }
        for (BranchCarePackageItem item : existing.values()) {
            item.setEnabled(false);
            itemRepository.save(item);
        }
    }

    private StaffCarePackageView toView(BranchCarePackage row) {
        List<StaffCarePackageItemView> services = new ArrayList<>();
        for (BranchCarePackageItem item : itemRepository.findByCarePackage_IdOrderByIdAsc(row.getId())) {
            OfferedService service = item.getOfferedService();
            if (service == null) {
                continue;
            }
            ServiceBehavior behavior = service.getBehavior();
            services.add(StaffCarePackageItemView.builder()
                    .offeredServiceId(service.getId())
                    .title(json.read(service.getTitleJson()))
                    .enabled(Boolean.TRUE.equals(item.getEnabled()))
                    .behaviorId(behavior == null ? null : behavior.getId())
                    .behaviorCode(behavior == null ? null : behavior.getCode())
                    .behaviorTitle(behavior == null ? Map.of() : json.read(behavior.getTitleJson()))
                    .sortOrder(service.getSortOrder())
                    .build());
        }
        services.sort(Comparator
                .comparing((StaffCarePackageItemView item) -> item.getBehaviorId() == null ? Long.MAX_VALUE : item.getBehaviorId())
                .thenComparing(item -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                .thenComparing(item -> item.getOfferedServiceId() == null ? 0L : item.getOfferedServiceId()));
        return StaffCarePackageView.builder()
                .id(row.getId())
                .branchId(row.getBranch() == null ? null : row.getBranch().getId())
                .name(row.getName())
                .price(row.getPrice())
                .currency(row.getCurrency())
                .active(row.getActive())
                .services(services)
                .build();
    }

    private BranchCarePackage requirePackage(Long packageId, Long branchId) {
        BranchCarePackage row = packageRepository.findById(packageId == null ? -1L : packageId)
                .orElseThrow(() -> new ResourceNotFoundException("package not found"));
        Long owner = row.getBranch() == null ? null : row.getBranch().getId();
        if (owner == null || !owner.equals(branchId)) {
            throw new ResourceNotFoundException("package not found");
        }
        return row;
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

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw MissingFieldException.required("name");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX) {
            throw new MissingFieldException("name is too long");
        }
        return trimmed;
    }

    private static int requirePrice(Integer price) {
        if (price == null) {
            throw MissingFieldException.required("price");
        }
        if (price < 0 || price > PRICE_MAX) {
            throw new MissingFieldException("price is invalid");
        }
        return price;
    }

    private static Set<Long> requireServiceIds(List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw MissingFieldException.required("serviceIds");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : serviceIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            throw MissingFieldException.required("serviceIds");
        }
        return ids;
    }
}
