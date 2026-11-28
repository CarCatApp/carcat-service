package com.carland.carland_service.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class AdminPromoSaveRequest {
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
}
