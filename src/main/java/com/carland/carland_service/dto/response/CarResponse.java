package com.carland.carland_service.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * tr: CarController araç sorgularında dönen yanıt DTO'su. Aracın kimlik, teknik özellik, kilometre ve toplam masraf bilgilerini içerir.
 * en: Response DTO returned by CarController car queries. Contains the car's identity, technical specs, mileage and all-time cost info.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponse {
    private Long carId;
    private Long customerId;
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
    private LocalDateTime createdAt;
    private String bodyType;
    private String message;
    private List<String> vinProvidedFields;
    /** Partner ids for badge logo fetch ({@code GET /photo/for/partner/badge-logo/get/{partnerId}}). */
    private List<String> servicedPartnerIds;
    private String resource;
    private BigDecimal allTimeCost;
    private Long engineTypeId;
}
