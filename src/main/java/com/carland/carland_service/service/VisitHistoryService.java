package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.VisitHistoryResponse;

/**
 * tr: Bir aracın VIN numarasına göre servis ziyareti geçmişini (visit tabanlı model) getiren servis sözleşmesidir;
 *     önce lokal cache'e (visits tablosu) bakar, yoksa Hyper API'den çeker. (Eski adı: CarVinHistoryServiceV2)
 * en: Service contract that returns a car's service visit history (visit-based model) by VIN;
 *     reads from local cache (visits table) first, otherwise fetches from the Hyper API.
 *     (Former name: CarVinHistoryServiceV2)
 */
public interface VisitHistoryService {

    /**
     * tr: VIN'e göre servis geçmişini döner. Müşteri-araç sahipliğini doğrular; cache varsa cache'den,
     *     yoksa Hyper'dan canlı çekip kaydederek döner. Eksik parametrede MissingFieldException,
     *     müşteri bulunamazsa UserNotFoundException, araç bulunamazsa ResourceNotFoundException fırlatır.
     * en: Returns service history by VIN. Validates customer-car ownership; serves from cache when present,
     *     otherwise fetches live from Hyper and persists. Throws MissingFieldException on missing params,
     *     UserNotFoundException when the customer is not found, ResourceNotFoundException when the car is not found.
     */
    VisitHistoryResponse getServiceHistoryByVin(String vin, String phoneNumber, String userIdHeader, String acceptLanguage);
}
