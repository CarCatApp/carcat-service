package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Müştərinin iptal səbəbi. Mətn booking.cancel_note sahəsinə yazılır.
 * en: The customer's cancel reason. The text is stored on booking.cancel_note.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingAppointmentCancelRequest {
    String reason;
}
