package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.PartnerRequest;
import com.carland.carland_service.dto.response.PartnerResponse;

/**
 * tr: İş ortağı (partner) kayıtlarının oluşturulması ve güncellenmesi için servis sözleşmesidir.
 * en: Service contract for creating and updating partner records.
 */
public interface PartnerService {

    /**
     * tr: Yeni bir partner kaydı oluşturur ve oluşturulan partneri döner.
     * en: Creates a new partner record and returns the created partner.
     */
    PartnerResponse createPartner(PartnerRequest request, String phoneNumber, String acceptLanguage);

    /**
     * tr: Var olan partner kaydını günceller ve güncel partneri döner.
     * en: Updates an existing partner record and returns the updated partner.
     */
    PartnerResponse updatePartner(PartnerRequest request, String phoneNumber, String acceptLanguage);
}
