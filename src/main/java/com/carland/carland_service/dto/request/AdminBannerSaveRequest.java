package com.carland.carland_service.dto.request;

import lombok.Data;

@Data
public class AdminBannerSaveRequest {
    String title;
    String description;
    String type;
    Long branchId;
    String url;
    Integer sortOrder;
    Boolean showing;
    String status;
}
