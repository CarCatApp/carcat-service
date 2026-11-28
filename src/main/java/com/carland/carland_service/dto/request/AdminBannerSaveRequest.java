package com.carland.carland_service.dto.request;

import lombok.Data;

@Data
public class AdminBannerSaveRequest {
    String title;
    String description;
    String context;
    String type;
    Long branchId;
    String url;
    Integer sortOrder;
    Boolean showing;
    String status;
}
