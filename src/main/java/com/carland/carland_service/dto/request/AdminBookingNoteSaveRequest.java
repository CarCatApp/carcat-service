package com.carland.carland_service.dto.request;

import lombok.Data;

@Data
public class AdminBookingNoteSaveRequest {
    Long id;
    String kind;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
}
