package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelRequest {
    /** Siyahıdan seçilən şablon. reason ilə birlikdə göndərilməz. */
    Long reasonId;
    /** Əl ilə yazılan mətn. reasonId ilə birlikdə göndərilməz. */
    String reason;
}
