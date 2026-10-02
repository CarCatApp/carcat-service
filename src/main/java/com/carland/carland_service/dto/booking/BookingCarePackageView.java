package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCarePackageView {
    Long id;
    String name;
    Integer price;
    String currency;
    /** Pakette açık olan xidmət sayısı. */
    Integer count;
    List<BookingCarePackageServiceView> services;
}
