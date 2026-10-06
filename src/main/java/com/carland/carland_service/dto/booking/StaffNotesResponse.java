package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StaffNotesResponse {
    String kind;
    List<StaffNoteView> items;
}
