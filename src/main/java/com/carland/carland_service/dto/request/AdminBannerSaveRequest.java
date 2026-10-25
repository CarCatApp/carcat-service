package com.carland.carland_service.dto.request;

import lombok.Data;

@Data
public class AdminBannerSaveRequest {
    String title;
    String description;
    Long branchId;
    Boolean showing;
    String status;
}
