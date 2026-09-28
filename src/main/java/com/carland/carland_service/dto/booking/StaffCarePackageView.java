package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Şubeye ait bir dövri qulluq paketi.
 * en: One routine-care package that belongs to a branch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCarePackageView {
    Long id;
    Long branchId;
    String name;
    Integer price;
    String currency;
    Boolean active;
    List<StaffCarePackageItemView> services;
}
