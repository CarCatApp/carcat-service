package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffNoteView {
    Long id;
    String title;
}
