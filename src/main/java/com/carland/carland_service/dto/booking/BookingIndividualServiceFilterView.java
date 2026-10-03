package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Filter chip satırı. All burada yok.
 * en: Filter chip row. All is not included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingIndividualServiceFilterView {
    Long id;
    String name;
}
