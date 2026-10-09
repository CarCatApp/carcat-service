package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: code Accept-Language cümləsidir. note yazılan mətndir.
 * en: code is the Accept-Language sentence. note is the written text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCanceledReasonView {
    String code;
    String title;
    String note;
}
