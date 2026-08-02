package com.carland.carland_service.dto.response;

import com.carland.carland_service.entity.Car;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDate;

/**
 * tr: Müşterinin araç servis kaydı işlemlerinden dönen yanıt DTO'su (servis adı, tarih, km, durum).
 * en: Response DTO returned by customer car service record operations (service name, date, km, status).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordResponse {

    Long id;
    Long serviceId;
    String serviceName;
    String serviceNameAz;
    String serviceNameRu;
    String serviceNameEn;
    String actionType;
    LocalDate doneDate;
    Integer doneKm;
    String message;
    String servicedStatus;
}
