package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * tr: code Accept-Language cümləsidir. title eyni cümləni tək dildə saxlayan xəritədir. note yazılan mətndir.
 * en: code is the Accept-Language sentence. title is that same sentence under one language key. note is the written text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCanceledReasonView {
    String code;
    Map<String, String> title;
    String note;
}
