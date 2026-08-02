package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * tr: CalendarController üzerinden randevu takvimi (gün, saat aralığı, çalışan sayısı) oluşturma isteklerinde kullanılan DTO.
 * en: DTO used in CalendarController requests to create an appointment calendar (day, time range, worker count).
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
    Long autoServiceId;

}
