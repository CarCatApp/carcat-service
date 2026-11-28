package com.carland.carland_service.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminBannerSaveRequest {
    String title;
    String description;
    String context;
    Long promoId;
    String type;
    String lang;
    LocalDate expireDate;
    Long branchId;
    String url;
    Integer sortOrder;
    Boolean showing;
    String status;
}
