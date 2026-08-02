package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: CalendarController sorgularında dönen, bir günün randevu zaman aralıklarını (RangeResponse listesi) içeren yanıt DTO'su.
 * en: Response DTO returned by CalendarController queries, containing a day's appointment time slots (list of RangeResponse).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponse {
    List<RangeResponse> timeRanges;
    String message;
}
