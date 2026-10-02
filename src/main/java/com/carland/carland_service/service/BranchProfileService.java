package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffBranchGoodView;
import com.carland.carland_service.dto.booking.StaffBranchProfileView;
import com.carland.carland_service.dto.booking.StaffBrandModelServiceView;
import com.carland.carland_service.dto.booking.StaffBrandModelView;
import com.carland.carland_service.dto.request.StaffBranchGoodSaveRequest;
import com.carland.carland_service.dto.request.StaffBranchProfileSaveRequest;
import com.carland.carland_service.dto.request.StaffBrandModelSaveRequest;
import com.carland.carland_service.dto.request.StaffBrandModelServiceSaveRequest;
import com.carland.carland_service.dto.request.StaffNameSaveRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchGood;
import com.carland.carland_service.entity.BrandModel;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingStaffRepository;
import com.carland.carland_service.repository.BranchGoodRepository;
import com.carland.carland_service.repository.BranchPhotoRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.carland.carland_service.repository.StaffPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * tr: Şube profili, mallar və marka siyahısı. Fərdi xidmət kataloğuna bağlı deyil.
 * en: Branch profile, goods, and brand lists. Not linked to the individual-service catalog.
 */
@Service
@RequiredArgsConstructor
public class BranchProfileService {

    private static final Set<String> UNITS = Set.of("eded", "litr", "kq", "metr", "dest", "servis");

    private final BookingStaffAccess bookingStaffAccess;
    private final BookingStaffRepository bookingStaffRepository;
    private final BranchRepository branchRepository;
    private final BranchGoodRepository branchGoodRepository;
    private final BrandModelServiceRepository brandModelServiceRepository;
    private final BrandModelRepository brandModelRepository;
    private final BranchPhotoRepository branchPhotoRepository;
    private final StaffPhotoRepository staffPhotoRepository;

    @Transactional(readOnly = true)
    public StaffBranchProfileView get(Long userId, boolean mustChangePassword, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        return view(staff, resolveBranch(staff, acceptLanguage));
    }

    @Transactional
    public StaffBranchProfileView updateProfile(Long userId, boolean mustChangePassword,
                                                 StaffBranchProfileSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        branch.setName(required(body.getName(), "name", 120));
        branch.setInstagram(optional(body.getInstagram(), 64));
        branchRepository.save(branch);
        return view(staff, branch);
    }

    /**
     * tr: Şube e-postasını yazar. Ad və Instagram dəyişmir.
     * en: Writes the branch email. Name and Instagram stay as they are.
     */
    @Transactional
    public StaffBranchProfileView updateContactEmail(Long userId, boolean mustChangePassword,
                                                      StaffBranchProfileSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        branch.setContactEmail(email(body == null ? null : body.getContactEmail()));
        branchRepository.save(branch);
        return view(staff, branch);
    }

    @Transactional
    public StaffBranchProfileView updateStaffName(Long userId, boolean mustChangePassword,
                                                   StaffNameSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        String name = required(body.getName(), "name", 80);
        String surname = required(body.getSurname(), "surname", 80);
        for (BookingStaff row : bookingStaffRepository.findByUserId(userId)) {
            row.setName(name);
            row.setSurname(surname);
        }
        staff.setName(name);
        staff.setSurname(surname);
        return view(staff, resolveBranch(staff, acceptLanguage));
    }

    @Transactional
    public StaffBranchProfileView addGood(Long userId, boolean mustChangePassword,
                                           StaffBranchGoodSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        String name = required(body == null ? null : body.getName(), "name", 80);
        int sort = branchGoodRepository.findByBranch_IdOrderBySortOrderAscIdAsc(branch.getId()).size();
        branchGoodRepository.save(BranchGood.builder()
                .branch(branch)
                .name(name)
                .sortOrder(sort)
                .build());
        return view(staff, branch);
    }

    @Transactional
    public StaffBranchProfileView deleteGood(Long userId, boolean mustChangePassword,
                                              Long goodId, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BranchGood good = branchGoodRepository.findById(goodId)
                .orElseThrow(() -> new ResourceNotFoundException("good not found"));
        if (good.getBranch() == null || !branch.getId().equals(good.getBranch().getId())) {
            throw new ResourceNotFoundException("good not found");
        }
        branchGoodRepository.delete(good);
        return view(staff, branch);
    }

    @Transactional
    public StaffBranchProfileView addBrandService(Long userId, boolean mustChangePassword,
                                                   StaffBrandModelServiceSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        if (body == null) {
            throw MissingFieldException.required("title");
        }
        int sort = brandModelServiceRepository.findByBranch_IdOrderBySortOrderAscIdAsc(branch.getId()).size();
        brandModelServiceRepository.save(BrandModelService.builder()
                .branch(branch)
                .title(required(body.getTitle(), "title", 120))
                .oil(Boolean.TRUE.equals(body.getOil()))
                .sortOrder(sort)
                .build());
        return view(staff, branch);
    }

    @Transactional
    public StaffBranchProfileView deleteBrandService(Long userId, boolean mustChangePassword,
                                                      Long serviceId, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BrandModelService heading = ownedHeading(branch, serviceId);
        brandModelRepository.deleteByBrandModelService_Id(heading.getId());
        brandModelServiceRepository.delete(heading);
        return view(staff, branch);
    }

    @Transactional
    public StaffBranchProfileView addBrandModel(Long userId, boolean mustChangePassword, Long serviceId,
                                                 StaffBrandModelSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BrandModelService heading = ownedHeading(branch, serviceId);
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        String series = null;
        String viscosity = null;
        String unit;
        if (Boolean.TRUE.equals(heading.getOil())) {
            series = required(body.getSeries(), "series", 80);
            viscosity = required(body.getViscosity(), "viscosity", 40);
        }
        unit = unit(body.getUnit());
        brandModelRepository.save(BrandModel.builder()
                .brandModelService(heading)
                .name(required(body.getName(), "name", 80))
                .series(series)
                .viscosity(viscosity)
                .unit(unit)
                .build());
        return view(staff, branch);
    }

    @Transactional
    public StaffBranchProfileView updateBrandModel(Long userId, boolean mustChangePassword, Long serviceId,
                                                    Long modelId, StaffBrandModelSaveRequest body,
                                                    String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BrandModelService heading = ownedHeading(branch, serviceId);
        BrandModel model = brandModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("brand model not found"));
        if (model.getBrandModelService() == null || !heading.getId().equals(model.getBrandModelService().getId())) {
            throw new ResourceNotFoundException("brand model not found");
        }
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        model.setName(required(body.getName(), "name", 80));
        if (Boolean.TRUE.equals(heading.getOil())) {
            model.setSeries(required(body.getSeries(), "series", 80));
            model.setViscosity(required(body.getViscosity(), "viscosity", 40));
        } else {
            model.setSeries(null);
            model.setViscosity(null);
        }
        model.setUnit(unit(body.getUnit()));
        brandModelRepository.save(model);
        return view(staff, branch);
    }

    @Transactional
    public StaffBranchProfileView deleteBrandModel(Long userId, boolean mustChangePassword, Long serviceId,
                                                    Long modelId, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BrandModelService heading = ownedHeading(branch, serviceId);
        BrandModel model = brandModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("brand model not found"));
        if (model.getBrandModelService() == null || !heading.getId().equals(model.getBrandModelService().getId())) {
            throw new ResourceNotFoundException("brand model not found");
        }
        brandModelRepository.delete(model);
        return view(staff, branch);
    }

    private StaffBranchProfileView view(BookingStaff staff, Branch branch) {
        List<StaffBranchGoodView> goods = new ArrayList<>();
        for (BranchGood good : branchGoodRepository.findByBranch_IdOrderBySortOrderAscIdAsc(branch.getId())) {
            goods.add(StaffBranchGoodView.builder().id(good.getId()).name(good.getName()).build());
        }
        List<StaffBrandModelServiceView> headings = new ArrayList<>();
        for (BrandModelService heading : brandModelServiceRepository.findByBranch_IdOrderBySortOrderAscIdAsc(branch.getId())) {
            List<StaffBrandModelView> models = new ArrayList<>();
            for (BrandModel model : brandModelRepository.findByBrandModelService_IdOrderByIdAsc(heading.getId())) {
                models.add(StaffBrandModelView.builder()
                        .id(model.getId())
                        .name(model.getName())
                        .series(model.getSeries())
                        .viscosity(model.getViscosity())
                        .unit(model.getUnit())
                        .build());
            }
            headings.add(StaffBrandModelServiceView.builder()
                    .id(heading.getId())
                    .title(heading.getTitle())
                    .oil(Boolean.TRUE.equals(heading.getOil()))
                    .models(models)
                    .build());
        }
        boolean partnerAdmin = BookingStaffRole.PARTNER_ADMIN.name().equals(staff.getRole());
        return StaffBranchProfileView.builder()
                .branchId(branch.getId())
                .name(branch.getName())
                .instagram(branch.getInstagram())
                .contactEmail(branch.getContactEmail())
                .hasPhoto(branchPhotoRepository.existsByBranchId(branch.getId()))
                .canUploadBranchPhoto(!partnerAdmin)
                .staffName(staff.getName())
                .staffSurname(staff.getSurname())
                .staffRole(staff.getRole())
                .hasStaffPhoto(staff.getUserId() != null && staffPhotoRepository.existsByUserId(staff.getUserId()))
                .goods(goods)
                .brandModelServices(headings)
                .build();
    }

    private BrandModelService ownedHeading(Branch branch, Long serviceId) {
        BrandModelService heading = brandModelServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("brand model service not found"));
        if (heading.getBranch() == null || !branch.getId().equals(heading.getBranch().getId())) {
            throw new ResourceNotFoundException("brand model service not found");
        }
        return heading;
    }

    private Branch resolveBranch(BookingStaff staff, String acceptLanguage) {
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

    private static String required(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw MissingFieldException.required(field);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new MissingFieldException(field + " is too long");
        }
        return trimmed;
    }

    private static String email(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 128 || !trimmed.contains("@")) {
            throw new MissingFieldException("contactEmail is invalid");
        }
        return trimmed;
    }

    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new MissingFieldException("instagram is too long");
        }
        return trimmed;
    }

    private static String unit(String value) {
        if (value == null || value.isBlank()) {
            throw MissingFieldException.required("unit");
        }
        String code = value.trim().toLowerCase();
        if (!UNITS.contains(code)) {
            throw new MissingFieldException("unit is invalid");
        }
        return code;
    }
}
