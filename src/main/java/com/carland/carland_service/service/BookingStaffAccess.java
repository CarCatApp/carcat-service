package com.carland.carland_service.service;

import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingStaffRepository;
import com.carland.carland_service.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * tr: Takvim/geçmiş yazımı için aktif BookingStaff + şube yetkisi.
 * en: Active BookingStaff + branch permission for calendar/history writes.
 */
@Service
@RequiredArgsConstructor
public class BookingStaffAccess {

    private final BookingStaffRepository staffRepository;
    private final BranchRepository branchRepository;

    public BookingStaff requireActive(Long userId, String acceptLanguage) {
        return staffRepository.findByUserId(userId).stream()
                .filter(row -> BookingStaffStatus.ACTIVE.name().equals(row.getStatus()))
                .findFirst()
                .orElseThrow(() -> new InvalidStatusException(
                        MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage)));
    }

    public boolean isActiveStaff(Long userId) {
        if (userId == null) {
            return false;
        }
        return staffRepository.findByUserId(userId).stream()
                .anyMatch(row -> BookingStaffStatus.ACTIVE.name().equals(row.getStatus()));
    }

    public Branch requireWritableBranch(BookingStaff staff, Long branchId, String acceptLanguage) {
        if (branchId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessagesLangValues.AUTO_SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage)));
        if (!branch.getPartner().getId().equals(staff.getPartner().getId())) {
            throw new ConflictException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }
        String role = staff.getRole();
        if (BookingStaffRole.PARTNER_ADMIN.name().equals(role)) {
            return branch;
        }
        if (BookingStaffRole.BRANCH_ADMIN.name().equals(role)) {
            if (staff.getBranch() == null || !staff.getBranch().getId().equals(branchId)) {
                throw new InvalidStatusException(
                        MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
            }
            return branch;
        }
        throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
    }
}
