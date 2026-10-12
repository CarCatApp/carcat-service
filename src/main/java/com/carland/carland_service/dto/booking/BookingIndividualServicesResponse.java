package com.carland.carland_service.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

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
    /**
     * Java null: alan JSON'da yok (STANDARD çağrı).
     * Optional.empty: JSON null (CAR_SERVICE, eşleşme yok).
     * Optional.of: eşleşen individual_services.id.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Optional<Long> preselectedServiceId;
}
