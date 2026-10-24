package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.dto.response.VisitHistoryResponse;
import com.carland.carland_service.entity.Car;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    /**
     * tr: Aracın DB'deki ziyaretlerini döner. Hyper API çağırmaz, Redis yazmaz.
     *     Partner 1 ve boş branch_id, cevapta şube 10 olur.
     * en: Returns the car's visits stored in the DB. Does not call Hyper and does not write Redis.
     *     Partner 1 with an empty branch_id is returned as branch 10.
     */
    List<VisitHistoryItemResponse> listStoredByCar(Car car, String acceptLanguage);

    /**
     * tr: Partner 1 ve şube 10 (boş branch_id dahil) en az bir ziyareti olan araç id'leri. Hyper çağırmaz.
     * en: Car ids with at least one visit for partner 1 and branch 10, including a null branch_id. Does not call Hyper.
     */
    Set<Long> carIdsWithHyperBranchVisit(Collection<Long> carIds);
}
