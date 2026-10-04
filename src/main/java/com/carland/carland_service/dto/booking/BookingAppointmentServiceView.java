package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Booka yazılan fərdi xidmət. Qiymət qəpik.
 * en: Individual service stored on the booking. Prices are qepik.
 */
@Data
@Builder
public class BookingAppointmentServiceView {
    Long id;
    String name;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;
}
