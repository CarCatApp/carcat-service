package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBrandModelsResponse;
import com.carland.carland_service.dto.request.StaffServicedGoodSaveRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchServicedGood;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServicedGoodRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Staff writes only a brand name under a shared heading. The list belongs to one branch.
 */
@Service
@RequiredArgsConstructor
public class BranchServicedGoodsService {

    private static final int NAME_MAX = 80;

    private final BookingStaffAccess bookingStaffAccess;
    private final BranchRepository branchRepository;
    private final BrandModelServiceRepository brandModelServiceRepository;
    private final BranchServicedGoodRepository servicedGoodRepository;
    private final BookingBranchBrandsService bookingBranchBrandsService;

    @Transactional(readOnly = true)
    public BookingBrandModelsResponse list(Long userId, boolean mustChangePassword, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff);
        return bookingBranchBrandsService.groups(branch.getId(), acceptLanguage, true);
    }

    @Transactional
    public BookingBrandModelsResponse add(Long userId, boolean mustChangePassword, Long headingId,
                                          StaffServicedGoodSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff);
        BrandModelService heading = heading(headingId);
        String name = requireName(body);
        rejectDuplicate(branch.getId(), heading.getId(), name, null);
        servicedGoodRepository.save(BranchServicedGood.builder()
                .branch(branch)
                .brandModelService(heading)
                .name(name)
                .price(price(body))
                .build());
        return bookingBranchBrandsService.groups(branch.getId(), acceptLanguage, true);
    }

    @Transactional
    public BookingBrandModelsResponse update(Long userId, boolean mustChangePassword, Long headingId, Long goodId,
                                             StaffServicedGoodSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff);
        BranchServicedGood good = owned(branch, headingId, goodId);
        String name = requireName(body);
        rejectDuplicate(branch.getId(), headingId, name, good.getId());
        good.setName(name);
        good.setPrice(price(body));
        servicedGoodRepository.save(good);
        return bookingBranchBrandsService.groups(branch.getId(), acceptLanguage, true);
    }

    @Transactional
    public BookingBrandModelsResponse delete(Long userId, boolean mustChangePassword, Long headingId, Long goodId,
                                             String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff);
        servicedGoodRepository.delete(owned(branch, headingId, goodId));
        return bookingBranchBrandsService.groups(branch.getId(), acceptLanguage, true);
    }

    private void rejectDuplicate(Long branchId, Long headingId, String name, Long ignoreId) {
        String needle = name.toLowerCase(Locale.ROOT);
        for (BranchServicedGood row : servicedGoodRepository.findByBranch_IdOrderByIdAsc(branchId)) {
            if (row.getBrandModelService() == null || !headingId.equals(row.getBrandModelService().getId())) {
                continue;
            }
            if (ignoreId != null && ignoreId.equals(row.getId())) {
                continue;
            }
            if (row.getName() != null && needle.equals(row.getName().trim().toLowerCase(Locale.ROOT))) {
                throw new ConflictException("Bu ad artıq bu başlıqda var");
            }
        }
    }

    private BranchServicedGood owned(Branch branch, Long headingId, Long goodId) {
        BranchServicedGood good = servicedGoodRepository.findById(goodId)
                .orElseThrow(() -> new ResourceNotFoundException("serviced good not found"));
        if (good.getBranch() == null || !branch.getId().equals(good.getBranch().getId())) {
            throw new ResourceNotFoundException("serviced good not found");
        }
        if (good.getBrandModelService() == null || !headingId.equals(good.getBrandModelService().getId())) {
            throw new ResourceNotFoundException("serviced good not found");
        }
        return good;
    }

    private BrandModelService heading(Long headingId) {
        return brandModelServiceRepository.findById(headingId)
                .orElseThrow(() -> new ResourceNotFoundException("heading not found"));
    }

    /** Null → qiymət yoxdur. Mənfi qəbul edilmir. */
    private java.math.BigDecimal price(StaffServicedGoodSaveRequest body) {
        if (body == null || body.getPrice() == null) {
            return null;
        }
        if (body.getPrice().signum() < 0) {
            throw new MissingFieldException("price must be >= 0");
        }
        return body.getPrice().setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String requireName(StaffServicedGoodSaveRequest body) {
        if (body == null || body.getName() == null || body.getName().isBlank()) {
            throw MissingFieldException.required("name");
        }
        String name = body.getName().trim();
        if (name.length() > NAME_MAX) {
            throw new MissingFieldException("name is too long");
        }
        return name;
    }

    private Branch resolveBranch(BookingStaff staff) {
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
}
