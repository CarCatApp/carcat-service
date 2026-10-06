package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminBookingNoteRow {
    Long id;
    String kind;
    String code;
    String titleAz;
    String titleEn;
    String titleRu;
    Integer sortOrder;
}
