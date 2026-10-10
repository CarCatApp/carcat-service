package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffBranchProfileView;
import com.carland.carland_service.dto.booking.StaffBrandModelServiceView;
import com.carland.carland_service.dto.booking.StaffBrandModelView;
import com.carland.carland_service.dto.request.StaffBranchProfileSaveRequest;
import com.carland.carland_service.dto.request.StaffBranchWorkingHoursRequest;
import com.carland.carland_service.dto.request.StaffBrandModelSaveRequest;
import com.carland.carland_service.dto.request.StaffNameSaveRequest;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BrandModel;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingStaffRepository;
import com.carland.carland_service.repository.BranchPhotoRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.carland.carland_service.repository.StaffPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * tr: Şube profili və ortaq başlıqların altındaki marka siyahısı. Başlıq əlavə etmək yoxdur.
 * en: Branch profile and brands under the shared headings. Branches cannot add headings.
 */
@Service
@RequiredArgsConstructor
public class BranchProfileService {

    private static final Set<String> UNITS = Set.of("eded", "litr", "kq", "metr", "dest", "servis");

    private final BookingStaffAccess bookingStaffAccess;
    private final BookingStaffRepository bookingStaffRepository;
    private final BranchRepository branchRepository;
    private final BrandModelServiceRepository brandModelServiceRepository;
    private final BrandModelRepository brandModelRepository;
    private final ServiceCategoryJson serviceCategoryJson;
    private final BranchPhotoRepository branchPhotoRepository;
    private final StaffPhotoRepository staffPhotoRepository;

    @Transactional(readOnly = true)
    public StaffBranchProfileView get(Long userId, boolean mustChangePassword, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        return view(staff, resolveBranch(staff, acceptLanguage), acceptLanguage);
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
        branch.setContactEmail(email(body.getContactEmail()));
        branchRepository.save(branch);
        return view(staff, branch, acceptLanguage);
    }

    /**
     * tr: Həftə içi, şənbə və bazar saatını yazır. Boş cüt həmin günü bağlayır.
     * en: Writes weekday, Saturday and Sunday hours. An empty pair closes that day.
     */
    @Transactional
    public StaffBranchProfileView updateWorkingHours(Long userId, boolean mustChangePassword,
                                                      StaffBranchWorkingHoursRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        StaffBranchWorkingHoursRequest hours = body == null ? new StaffBranchWorkingHoursRequest() : body;
        BranchWorkingHours.write(branch,
                hours.getWeekdayStart(), hours.getWeekdayEnd(),
                hours.getSaturdayStart(), hours.getSaturdayEnd(),
                hours.getSundayStart(), hours.getSundayEnd());
        branchRepository.save(branch);
        return view(staff, branch, acceptLanguage);
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
        return view(staff, branch, acceptLanguage);
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
        return view(staff, resolveBranch(staff, acceptLanguage), acceptLanguage);
    }

    @Transactional
    public StaffBranchProfileView addBrandModel(Long userId, boolean mustChangePassword, Long serviceId,
                                                 StaffBrandModelSaveRequest body, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BrandModelService heading = heading(serviceId);
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        String series = required(body.getSeries(), "series", 80);
        String viscosity = null;
        if (Boolean.TRUE.equals(heading.getOil())) {
            viscosity = required(body.getViscosity(), "viscosity", 40);
        }
        String unit = unit(body.getUnit());
        brandModelRepository.save(BrandModel.builder()
                .brandModelService(heading)
                .branch(branch)
                .name(required(body.getName(), "name", 80))
                .series(series)
                .viscosity(viscosity)
                .unit(unit)
                .build());
        return view(staff, branch, acceptLanguage);
    }

    @Transactional
    public StaffBranchProfileView updateBrandModel(Long userId, boolean mustChangePassword, Long serviceId,
                                                    Long modelId, StaffBrandModelSaveRequest body,
                                                    String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BrandModelService heading = heading(serviceId);
        BrandModel model = ownedModel(branch, heading, modelId);
        if (body == null) {
            throw MissingFieldException.required("name");
        }
        model.setName(required(body.getName(), "name", 80));
        model.setSeries(required(body.getSeries(), "series", 80));
        if (Boolean.TRUE.equals(heading.getOil())) {
            model.setViscosity(required(body.getViscosity(), "viscosity", 40));
        } else {
            model.setViscosity(null);
        }
        model.setUnit(unit(body.getUnit()));
        brandModelRepository.save(model);
        return view(staff, branch, acceptLanguage);
    }

    @Transactional
    public StaffBranchProfileView deleteBrandModel(Long userId, boolean mustChangePassword, Long serviceId,
                                                    Long modelId, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = resolveBranch(staff, acceptLanguage);
        BrandModelService heading = heading(serviceId);
        brandModelRepository.delete(ownedModel(branch, heading, modelId));
        return view(staff, branch, acceptLanguage);
    }

    private StaffBranchProfileView view(BookingStaff staff, Branch branch, String acceptLanguage) {
        Map<Long, List<StaffBrandModelView>> modelsByHeading = new LinkedHashMap<>();
        for (BrandModel model : brandModelRepository.findByBranch_IdOrderByIdAsc(branch.getId())) {
            if (model.getBrandModelService() == null || model.getBrandModelService().getId() == null) {
                continue;
            }
            modelsByHeading
                    .computeIfAbsent(model.getBrandModelService().getId(), id -> new ArrayList<>())
                    .add(StaffBrandModelView.builder()
                            .id(model.getId())
                            .name(model.getName())
                            .series(model.getSeries())
                            .viscosity(model.getViscosity())
                            .unit(model.getUnit())
                            .build());
        }
        String lang = BookingMineService.langOf(acceptLanguage);
        List<StaffBrandModelServiceView> headings = new ArrayList<>();
        for (BrandModelService heading : brandModelServiceRepository.findAllByOrderBySortOrderAscIdAsc()) {
            List<StaffBrandModelView> models = modelsByHeading.getOrDefault(heading.getId(), List.of());
            String title = BookingMineService.catalogText(serviceCategoryJson.read(heading.getTitleJson()), lang);
            headings.add(StaffBrandModelServiceView.builder()
                    .id(heading.getId())
                    .title(title == null ? "" : title)
                    .oil(Boolean.TRUE.equals(heading.getOil()))
                    .models(models)
                    .build());
        }
        BookingStaff named = staffOnBranch(staff.getUserId(), branch, staff);
        boolean partnerAdmin = BookingStaffRole.PARTNER_ADMIN.name().equals(staff.getRole());
        return StaffBranchProfileView.builder()
                .branchId(branch.getId())
                .name(branch.getName())
                .instagram(branch.getInstagram())
                .contactEmail(branch.getContactEmail())
                .workingHoursWeekday(BranchWorkingHours.weekdayOf(branch))
                .workingHoursSaturday(BranchWorkingHours.saturdayOf(branch))
                .workingHoursSunday(BranchWorkingHours.sundayOf(branch))
                .hasPhoto(branchPhotoRepository.existsByBranchId(branch.getId()))
                .canUploadBranchPhoto(!partnerAdmin)
                .staffName(named.getName())
                .staffSurname(named.getSurname())
                .staffRole(named.getRole())
                .hasStaffPhoto(staff.getUserId() != null && staffPhotoRepository.existsByUserId(staff.getUserId()))
                .brandModelServices(headings)
                .build();
    }

    private BookingStaff staffOnBranch(Long userId, Branch branch, BookingStaff fallback) {
        if (userId == null || branch == null || branch.getId() == null) {
            return fallback;
        }
        return bookingStaffRepository.findByUserId(userId).stream()
                .filter(row -> row.getBranch() != null && branch.getId().equals(row.getBranch().getId()))
                .findFirst()
                .orElse(fallback);
    }

    private BrandModelService heading(Long serviceId) {
        return brandModelServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("brand model service not found"));
    }

    private BrandModel ownedModel(Branch branch, BrandModelService heading, Long modelId) {
        BrandModel model = brandModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("brand model not found"));
        if (model.getBrandModelService() == null || !heading.getId().equals(model.getBrandModelService().getId())) {
            throw new ResourceNotFoundException("brand model not found");
        }
        if (model.getBranch() == null || branch.getId() == null || !branch.getId().equals(model.getBranch().getId())) {
            throw new ResourceNotFoundException("brand model not found");
        }
        return model;
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
