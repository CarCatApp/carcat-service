package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * tr: RangeController/percentage akışında bir aracın servis yüzdesi kaydını (son/sonraki servis tarihi ve km) oluşturmak veya güncellemek için kullanılan istek DTO'su.
 * en: Request DTO used in the RangeController/percentage flow to create or update a car's service percentage record (last/next service date and km).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PercentageRequest {
    Long carId;
    Long percentageId;
    LocalDate lastServiceDate;
    Integer lastServiceKm;
    LocalDate nextServiceDate;
    Integer nextServiceKm;
}
