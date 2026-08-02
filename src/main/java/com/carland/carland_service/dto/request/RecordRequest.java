package com.carland.carland_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * tr: Müşterinin araç servis kaydını (yapılan servis, tarih, km) oluşturmak veya güncellemek için kullanılan istek DTO'su.
 * en: Request DTO used to create or update a customer's car service record (performed service, date, km).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecordRequest {
    Long carId;
    String vin;
    Long recordId;
    Long serviceId;
    String serviceName;
    String actionType;
    LocalDate doneDate;
    Integer doneKm;
    String servicedStatus;
}
