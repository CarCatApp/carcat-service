package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCarePackageServiceView {
    Long id;
    String name;
    /** Foto yoksa null. Flutter bu yolu GET /api/v1/photo ile açar. */
    String iconUrl;
}
