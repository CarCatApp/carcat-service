package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Her çağrı aynı düz liste. filter=all hepsi, filter={id} yalnız o filter id.
 * en: Every call is the same flat list. filter=all is everything, filter={id} is that filter id only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingIndividualServicesResponse {
    Long branchId;
    List<BookingIndividualServiceView> services;
}
