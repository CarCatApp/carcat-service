package com.carland.carland_service.dto.response.hyper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

/**
 * tr: Hyper API'nin VIN ile araç sorgusu endpoint'inden dönen yanıtı (araç bilgileri ve servis geçmişi) eşleyen DTO.
 * en: DTO mapping the response of the Hyper API vehicle-by-VIN lookup endpoint (vehicle info and service history).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "partnerId",
        "plate",
        "vin",
        "brand",
        "model",
        "year",
        "engineVolume",
        "engineType",
        "bodyType",
        "trim",
        "currentMileage",
        "serviceHistory"
})
public class HyperVehicleByVinResponse {
    private Long partnerId;
    private String plate;
    private String vin;
    private String brand;
    private String model;
    private Integer year;
    private Double engineVolume;
    private String engineType;
    private String bodyType;
    private String trim;
    private Integer currentMileage;
    private List<HyperServiceHistoryItemResponse> serviceHistory;
}
