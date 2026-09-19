package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBranchView;
import com.carland.carland_service.dto.booking.StaffDisableRequest;
import com.carland.carland_service.dto.booking.StaffProvisionRequest;
import com.carland.carland_service.dto.booking.StaffProvisionResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStaffStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.feign.AuthStaffFeign;
import com.carland.carland_service.repository.BookingStaffRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.util.PhoneNumbers;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingOrgService {

    private static final String ADMIN_CREATED_SOURCE = "carcat";

    private final PartnerRepository partnerRepository;
    private final BranchRepository branchRepository;
    private final BookingStaffRepository staffRepository;
    private final AuthStaffFeign authStaffFeign;
    private final BookingStaffAuditService staffAuditService;

    @Transactional(readOnly = true)
    public List<Partner> listPartners() {
        return partnerRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Partner getPartner(Long id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner tapılmadı"));
    }

    @Transactional(readOnly = true)
    public List<BookingStaff> listStaff(Long partnerId) {
        return staffRepository.findByPartnerIdWithBranch(partnerId);
    }

    @Transactional(readOnly = true)
    public List<Branch> listBranches(Long partnerId) {
        getPartner(partnerId);
        return branchRepository.findByPartnerIdOrderByIdAsc(partnerId);
    }

    @Transactional
    public Partner createPartner(String name, boolean active, String photo, String contactPhone, String contactEmail) {
        if (name == null || name.isBlank()) {
            throw new MissingFieldException("Partner adı boş ola bilməz");
        }
        String trimmed = name.trim();
        if (partnerRepository.findFirstByNameIgnoreCase(trimmed).isPresent()) {
            throw new ConflictException("Bu adla partner artıq var — mövcud qeydi açın");
        }
        Partner partner = Partner.builder()
                .name(trimmed)
                .active(active)
                .logoUrl(blankToNull(photo))
                .contactPhone(blankToNull(contactPhone))
                .contactEmail(blankToNull(contactEmail))
                .source(ADMIN_CREATED_SOURCE)
                .build();
        return partnerRepository.save(partner);
    }

    @Transactional
    public Branch addBranch(Long partnerId, String name, String address, Double lat, Double lng, boolean active,
                            String contactPhone, String workingHours, String photo) {
        Partner partner = getPartner(partnerId);
        if (name == null || name.isBlank()) {
            throw new MissingFieldException("Branch adı boş ola bilməz");
        }
        Branch branch = Branch.builder()
                .partner(partner)
                .name(name.trim())
                .address(blankToNull(address))
                .lat(lat)
                .lng(lng)
                .active(active)
                .contactPhone(blankToNull(contactPhone))
                .workingHours(blankToNull(workingHours))
                .photo(blankToNull(photo))
                .build();
        return branchRepository.save(branch);
    }

    /**
     * Auth provision first; on local save failure disable the auth user.
     * Returns one-time password once (never logged).
     */
    @Transactional
    public StaffProvisionResponse addStaff(Long partnerId, Long branchId, String roleRaw, String phoneRaw,
                                           String name, String surname, String actor) {
        Partner partner = getPartner(partnerId);
        String phone = PhoneNumbers.normalize(phoneRaw);
        if (phone == null) {
            throw new MissingFieldException("Telefon +994XXXXXXXXX formatında olmalıdır");
        }
        String role = roleRaw == null ? "" : roleRaw.trim().toUpperCase();
        Branch branch = null;
        if (BookingStaffRole.PARTNER_ADMIN.name().equals(role)) {
            if (partner.getHqUserId() != null || staffRepository.existsByPartnerIdAndBranchIsNull(partnerId)) {
                throw new ConflictException("Bu partnerin artıq HQ admini var");
            }
        } else if (BookingStaffRole.BRANCH_ADMIN.name().equals(role)) {
            if (branchId == null) {
                throw new MissingFieldException("Branch admin üçün şöbə seçilməlidir");
            }
            branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Şöbə tapılmadı"));
            if (!branch.getPartner().getId().equals(partnerId)) {
                throw new ConflictException("Şöbə bu partnerə aid deyil");
            }
        } else {
            throw new MissingFieldException("Rol PARTNER_ADMIN və ya BRANCH_ADMIN olmalıdır");
        }

        StaffProvisionResponse provisioned;
        try {
            provisioned = authStaffFeign.provision(StaffProvisionRequest.builder()
                    .phoneNumber(phone)
                    .role(role)
                    .name(blankToNull(name))
                    .surname(blankToNull(surname))
                    .build());
        } catch (FeignException ex) {
            if (ex.status() == 409) {
                throw new ConflictException("Bu nömrə artıq user kimi mövcuddur");
            }
            log.warn("STAFF_PROVISION_FEIGN_FAIL status={}", ex.status());
            throw new ConflictException("Auth-da istifadəçi yaradıla bilmədi");
        }

        Long userId = provisioned.getUserId();
        try {
            if (staffRepository.existsByUserIdAndPartnerIdNot(userId, partnerId)
                    || (staffRepository.existsByUserId(userId)
                    && staffRepository.findByUserId(userId).stream()
                    .anyMatch(row -> !row.getPartner().getId().equals(partnerId)))) {
                authStaffFeign.disable(StaffDisableRequest.builder().userId(userId).build());
                throw new ConflictException("Bu istifadəçi başqa partnerə bağlıdır");
            }
            BookingStaff staff = BookingStaff.builder()
                    .userId(userId)
                    .partner(partner)
                    .branch(branch)
                    .role(role)
                    .status(BookingStaffStatus.INVITED.name())
                    .phoneNumber(phone)
                    .name(blankToNull(name))
                    .surname(blankToNull(surname))
                    .createdAt(LocalDateTime.now())
                    .build();
            staffRepository.save(staff);
            if (BookingStaffRole.PARTNER_ADMIN.name().equals(role)) {
                partner.setHqUserId(userId);
                partnerRepository.save(partner);
            }
            staffAuditService.recordCreate(actor, userId, partnerId,
                    branch == null ? null : branch.getId(), phone, role);
        } catch (ConflictException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            try {
                authStaffFeign.disable(StaffDisableRequest.builder().userId(userId).build());
            } catch (Exception disableEx) {
                log.warn("STAFF_DISABLE_COMPENSATION_FAIL userId={}", userId);
            }
            throw ex;
        }
        log.info("BOOKING_STAFF_ADDED partnerId={} userId={} role={}", partnerId, userId, role);
        return provisioned;
    }

    @Transactional
    public void activateStaff(Long userId) {
        List<BookingStaff> rows = staffRepository.findByUserId(userId);
        for (BookingStaff row : rows) {
            if (BookingStaffStatus.DISABLED.name().equals(row.getStatus())) {
                continue;
            }
            row.setStatus(BookingStaffStatus.ACTIVE.name());
        }
        staffRepository.saveAll(rows);
    }

    @Transactional
    public List<BookingBranchView> visibleBranches(Long userId, boolean mustChangePassword) {
        if (userId == null) {
            throw new ForbiddenException("Staff token required");
        }
        if (mustChangePassword) {
            throw new ForbiddenException("Şifrəni dəyişdirməlisiniz");
        }
        List<BookingStaff> rows = staffRepository.findByUserId(userId).stream()
                .filter(row -> !BookingStaffStatus.DISABLED.name().equals(row.getStatus()))
                .toList();
        if (rows.isEmpty()) {
            throw new ForbiddenException("Booking staff tapılmadı");
        }
        boolean lazyActivate = rows.stream().anyMatch(row ->
                BookingStaffStatus.INVITED.name().equals(row.getStatus()));
        if (lazyActivate) {
            activateStaff(userId);
            rows = staffRepository.findByUserId(userId).stream()
                    .filter(row -> !BookingStaffStatus.DISABLED.name().equals(row.getStatus()))
                    .toList();
        }

        Partner partner = rows.get(0).getPartner();
        if (Boolean.FALSE.equals(partner.getActive())) {
            return List.of();
        }
        boolean hq = rows.stream().anyMatch(row ->
                BookingStaffRole.PARTNER_ADMIN.name().equals(row.getRole()) && row.getBranch() == null);
        List<Branch> branches;
        if (hq) {
            branches = branchRepository.findByPartnerIdOrderByIdAsc(partner.getId());
        } else {
            Map<Long, Branch> unique = new LinkedHashMap<>();
            for (BookingStaff row : rows) {
                if (row.getBranch() != null) {
                    unique.put(row.getBranch().getId(), row.getBranch());
                }
            }
            branches = new ArrayList<>(unique.values());
            branches.sort(Comparator.comparing(Branch::getId));
        }
        return branches.stream()
                .map(branch -> toView(partner, branch))
                .toList();
    }

    private static BookingBranchView toView(Partner partner, Branch branch) {
        return BookingBranchView.builder()
                .id(branch.getId())
                .partnerId(partner.getId())
                .partnerName(partner.getName())
                .name(branch.getName())
                .address(branch.getAddress())
                .lat(branch.getLat())
                .lng(branch.getLng())
                .active(branch.getActive())
                .partnerActive(partner.getActive())
                .contactPhone(branch.getContactPhone())
                .workingHours(branch.getWorkingHours())
                .photo(branch.getPhoto())
                .rating(branch.getRating())
                .ratingCount(branch.getRatingCount())
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
