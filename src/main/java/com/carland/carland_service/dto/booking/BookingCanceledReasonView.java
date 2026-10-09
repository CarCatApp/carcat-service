package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * tr: code müştəri və ya servisin 3 dildəki cümləsidir. note yazılan mətndir.
 * en: code is the customer or service sentence in az/en/ru. note is the written text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCanceledReasonView {
    Map<String, String> code;
    Map<String, String> title;
    String note;
}
