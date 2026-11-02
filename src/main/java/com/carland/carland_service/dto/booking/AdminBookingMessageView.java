package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Admin sohbet satırı. Saat 24 saat, HH:mm.
 * en: Admin chat row. Clock is 24-hour, HH:mm.
 */
@Data
@Builder
public class AdminBookingMessageView {
    Long id;
    /** customer | branch */
    String fromRole;
    String body;
    String createdAt;
}
