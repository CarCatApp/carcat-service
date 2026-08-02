package com.carland.carland_service.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * tr: Müşteri-araç eşleştirme (link) akışında kullanılan sadeleştirilmiş araç yanıt DTO'su; CustomerWithCarResponse içinde döner.
 * en: Simplified car response DTO used in the customer-car linking flow; returned inside CustomerWithCarResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponseForLink {
    private Long carId;
    private String vin;
    private String plateNumber;
    private String brand;
    private String model;
    private Integer modelYear;
    private String color;
    private String engineType;
    private Integer engineVolume;
    private String transmissionType;
    private Long mileage;
    private LocalDateTime updatedAt;
    private String bodyType;
}
