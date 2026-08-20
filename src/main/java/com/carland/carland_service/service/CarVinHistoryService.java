package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.CarVinServiceHistoryResponse;

/**
 * tr: Bir aracın VIN numarasına göre servis geçmişini getiren servis sözleşmesidir.
 *     HTTP v1: GET /api/v1/car/{vin}/service-history (service_histories).
 * en: Service contract that returns a car's service history by VIN.
 *     HTTP v1: GET /api/v1/car/{vin}/service-history (service_histories).
 */
public interface CarVinHistoryService {
    /**
     * tr: VIN'e göre servis geçmişini döner; müşteri ve araç sahipliği doğrulanır.
     * en: Returns the service history by VIN; customer and car ownership are validated.
     */
    CarVinServiceHistoryResponse getServiceHistoryByVin(String vin, String phoneNumber, String userIdHeader, String acceptLanguage);


}
