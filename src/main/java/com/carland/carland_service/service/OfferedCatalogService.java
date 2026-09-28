package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffOfferedServiceListResponse;
import com.carland.carland_service.dto.booking.StaffOfferedServiceView;
import com.carland.carland_service.dto.booking.StaffServiceBehaviorGroupView;
import com.carland.carland_service.dto.request.AdminOfferedServiceSaveRequest;
import com.carland.carland_service.dto.request.AdminServiceBehaviorSaveRequest;
import com.carland.carland_service.dto.response.AdminOfferedServiceRow;
import com.carland.carland_service.dto.response.AdminServiceBehaviorRow;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.ServiceBehavior;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.OfferedServiceRepository;
import com.carland.carland_service.repository.ServiceBehaviorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * tr: Dövri qulluq xidmət grupları ve satırları. Liste bütün şubeler için ortaktır.
 * en: Routine-care service groups and lines. The list is shared by every branch.
 */
@Service
@RequiredArgsConstructor
public class OfferedCatalogService {

    private static final Pattern CODE = Pattern.compile("^[a-z0-9_]+$");

    private final ServiceBehaviorRepository behaviorRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final BookingStaffAccess bookingStaffAccess;
    private final ServiceCategoryJson json;

    /**
     * tr: Aktif gruplar ve altındaki aktif xidmətlər. Sıra sort_order, sonra id.
     * en: Active groups and the active services under them. Ordered by sort_order, then id.
     */
    @Transactional(readOnly = true)
    public StaffOfferedServiceListResponse listForStaff(Long userId, boolean mustChangePassword, String acceptLanguage) {
        bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        List<StaffServiceBehaviorGroupView> groups = new ArrayList<>();
        for (ServiceBehavior behavior : behaviorRepository.findByActiveTrueOrderBySortOrderAscIdAsc()) {
            List<StaffOfferedServiceView> services = new ArrayList<>();
            for (OfferedService row : offeredServiceRepository
                    .findByBehavior_IdAndActiveTrueOrderBySortOrderAscIdAsc(behavior.getId())) {
                services.add(StaffOfferedServiceView.builder()
                        .id(row.getId())
                        .title(json.read(row.getTitleJson()))
                        .sortOrder(row.getSortOrder())
                        .build());
            }
            groups.add(StaffServiceBehaviorGroupView.builder()
                    .id(behavior.getId())
                    .code(behavior.getCode())
                    .title(json.read(behavior.getTitleJson()))
                    .sortOrder(behavior.getSortOrder())
                    .services(services)
                    .build());
        }
        return StaffOfferedServiceListResponse.builder().groups(groups).build();
    }

    /**
     * tr: Admin grup satırları. Pasif gruplar da gelir.
     * en: Admin group rows. Inactive groups are included.
     */
    @Transactional(readOnly = true)
    public List<AdminServiceBehaviorRow> behaviorRows() {
        List<AdminServiceBehaviorRow> rows = new ArrayList<>();
        for (ServiceBehavior behavior : behaviorRepository.findAllByOrderBySortOrderAscIdAsc()) {
            rows.add(toBehaviorRow(behavior));
        }
        return rows;
    }

    /**
     * tr: id null ise grup oluşturur. id varsa code değişmez.
     * en: Null id creates a group. An existing id does not change code.
     */
    @Transactional
    public AdminServiceBehaviorRow saveBehavior(AdminServiceBehaviorSaveRequest body) {
        if (body == null || isBlank(body.getTitleAz())) {
            throw MissingFieldException.required("titleAz");
        }
        ServiceBehavior behavior;
        if (body.getId() == null) {
            String code = normalizeCode(body.getCode());
            if (behaviorRepository.existsByCode(code)) {
                throw new ConflictException("code already exists");
            }
            behavior = ServiceBehavior.builder().code(code).build();
        } else {
            behavior = behaviorRepository.findById(body.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("behavior not found"));
        }
        behavior.setTitleJson(json.write(body.getTitleAz().trim(), trimToEmpty(body.getTitleEn()), trimToEmpty(body.getTitleRu())));
        behavior.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        behavior.setActive(body.getActive() == null ? Boolean.TRUE : body.getActive());
        return toBehaviorRow(behaviorRepository.save(behavior));
    }

    /**
     * tr: Admin xidmət satırları. Pasif satırlar da gelir.
     * en: Admin service rows. Inactive rows are included.
     */
    @Transactional(readOnly = true)
    public List<AdminOfferedServiceRow> offeredRows() {
        List<AdminOfferedServiceRow> rows = new ArrayList<>();
        for (OfferedService row : offeredServiceRepository.findAllByOrderBySortOrderAscIdAsc()) {
            rows.add(toOfferedRow(row));
        }
        return rows;
    }

    /**
     * tr: Xidmət oluşturur veya günceller. behaviorId seçilen grubun id'sidir.
     * en: Creates or updates a service. behaviorId is the id of the selected group.
     */
    @Transactional
    public AdminOfferedServiceRow saveOffered(AdminOfferedServiceSaveRequest body) {
        if (body == null || isBlank(body.getTitleAz())) {
            throw MissingFieldException.required("titleAz");
        }
        if (body.getBehaviorId() == null) {
            throw MissingFieldException.required("behaviorId");
        }
        ServiceBehavior behavior = behaviorRepository.findById(body.getBehaviorId())
                .orElseThrow(() -> new ResourceNotFoundException("behavior not found"));
        OfferedService row;
        if (body.getId() == null) {
            row = OfferedService.builder().behavior(behavior).build();
        } else {
            row = offeredServiceRepository.findById(body.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("service not found"));
            row.setBehavior(behavior);
        }
        row.setTitleJson(json.write(body.getTitleAz().trim(), trimToEmpty(body.getTitleEn()), trimToEmpty(body.getTitleRu())));
        row.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        row.setActive(body.getActive() == null ? Boolean.TRUE : body.getActive());
        return toOfferedRow(offeredServiceRepository.save(row));
    }

    private AdminServiceBehaviorRow toBehaviorRow(ServiceBehavior behavior) {
        Map<String, String> title = json.read(behavior.getTitleJson());
        return AdminServiceBehaviorRow.builder()
                .id(behavior.getId())
                .code(behavior.getCode())
                .titleAz(title.get("az"))
                .titleEn(title.get("en"))
                .titleRu(title.get("ru"))
                .sortOrder(behavior.getSortOrder())
                .active(behavior.getActive())
                .build();
    }

    private AdminOfferedServiceRow toOfferedRow(OfferedService row) {
        Map<String, String> title = json.read(row.getTitleJson());
        ServiceBehavior behavior = row.getBehavior();
        Map<String, String> behaviorTitle = behavior == null ? Map.of() : json.read(behavior.getTitleJson());
        return AdminOfferedServiceRow.builder()
                .id(row.getId())
                .behaviorId(behavior == null ? null : behavior.getId())
                .behaviorCode(behavior == null ? null : behavior.getCode())
                .behaviorTitleAz(behaviorTitle.get("az"))
                .titleAz(title.get("az"))
                .titleEn(title.get("en"))
                .titleRu(title.get("ru"))
                .sortOrder(row.getSortOrder())
                .active(row.getActive())
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

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
