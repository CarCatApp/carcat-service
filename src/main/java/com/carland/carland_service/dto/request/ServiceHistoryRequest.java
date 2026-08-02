package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * tr: Bir araca ait servis geçmişi kaydı (servis adı, tarih, km, tutar) eklemek için kullanılan istek DTO'su.
 * en: Request DTO used to add a service history entry (service name, date, km, amount) for a car.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceHistoryRequest {


    Long carId;
    String vin;
    String serviceName;
    LocalDate doneDate;
    Integer doneKm;
    BigDecimal serviceAmount;

}
