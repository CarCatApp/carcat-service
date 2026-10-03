package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Paket içindeki bir filtre grubu. Boş grup dönmez.
 * en: One filter group inside a care package. Empty groups are omitted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCarePackageGroupView {
    Long id;
    String name;
    List<BookingCarePackageServiceView> services;
}
