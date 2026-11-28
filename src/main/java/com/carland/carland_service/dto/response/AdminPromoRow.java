package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AdminPromoRow {
    Long id;
    String name;
    String description;
    String code;
    LocalDate startDate;
    LocalDate endDate;
    String discountType;
    BigDecimal discountValue;
    String status;
    Boolean expired;
    List<Long> branchIds;
    List<AdminBannerBranchOption> branches;
}
