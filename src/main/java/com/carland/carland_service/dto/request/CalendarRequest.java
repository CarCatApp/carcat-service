package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * tr: Şube randevu takvimi oluşturma/okuma isteği.
 * en: Branch calendar create/read request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CalendarRequest {
    LocalDate day;
    LocalTime start;
    LocalTime end;
    Integer rangeMinutes;
    String serviceCategory;
    Integer workerCount;
    Long branchId;
    /** instant | approval. Default instant. */
    String bookingMode;
    /** * or one catalog key. Default *. */
    String serviceKey;
}
