package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: İptal olunmuş book. reason bookingə yazılan mətndir.
 * en: Cancelled booking. reason is the text stored on the booking.
 */
@Data
@Builder
public class BookingAppointmentCancelResponse {
    Long bookingId;
    String ref;
    String status;
    String reason;
}
