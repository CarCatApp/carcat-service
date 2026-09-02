package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.VisitHistoryResponse;

/**
 * tr: Bir aracın VIN numarasına göre servis ziyareti geçmişini (visit tabanlı model) getiren servis sözleşmesidir;
 *     Redis miss'te Hyper API'den çeker, mevcut recordId'leri günceller (upsert). (Eski adı: CarVinHistoryServiceV2)
 * en: Service contract that returns a car's service visit history (visit-based model) by VIN;
 *     on Redis miss fetches from Hyper and upserts existing recordIds. (Former name: CarVinHistoryServiceV2)
 */
public interface VisitHistoryService {

    /**
     * tr: VIN'e göre servis geçmişini döner. Müşteri-araç sahipliğini doğrular; Redis hit ise oradan,
     *     miss ise Hyper'dan çekip visits'e upsert eder. Eksik parametrede MissingFieldException,
     *     müşteri bulunamazsa UserNotFoundException, araç bulunamazsa ResourceNotFoundException fırlatır.
     * en: Returns service history by VIN. Validates customer-car ownership; Redis hit returns cache,
     *     miss fetches Hyper and upserts visits. Throws MissingFieldException on missing params,
     *     UserNotFoundException when the customer is not found, ResourceNotFoundException when the car is not found.
     */
    VisitHistoryResponse getServiceHistoryByVin(String vin, String phoneNumber, String userIdHeader, String acceptLanguage);
}
