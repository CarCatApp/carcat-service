package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Şube profili. canUploadBranchPhoto partner adminde false.
 * en: Branch profile. canUploadBranchPhoto is false for a partner admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffBranchProfileView {
    Long branchId;
    String name;
    String instagram;
    Boolean hasPhoto;
    Boolean canUploadBranchPhoto;
    String staffName;
    String staffSurname;
    String staffRole;
    Boolean hasStaffPhoto;
    List<StaffBranchGoodView> goods;
    List<StaffBrandModelServiceView> brandModelServices;
}
