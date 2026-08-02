package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.PartnerRequest;
import com.carland.carland_service.dto.response.PartnerResponse;
import com.carland.carland_service.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * tr: Partner REST controller'ı; partner oluşturma ve partner bilgilerini güncelleme uçlarını sunar.
 * en: REST controller for partners; exposes endpoints to create a partner and to update partner information.
 */
@RestController
@RequestMapping("/api/v1/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    /**
     * tr: Gövdedeki PartnerRequest ile yeni bir partner oluşturur; phoneNumber header'ı ile çağıran belirlenir ve oluşturulan partner döner.
     * en: Creates a new partner from the PartnerRequest body; the caller is identified by the phoneNumber header, returns the created partner.
     */
    @PostMapping("/create")
    public PartnerResponse createPartner(@RequestBody PartnerRequest request,
                                         @RequestHeader("Authorization") String token,
                                         @RequestHeader("phoneNumber") String phoneNumber,
                                         @RequestHeader("Accept-Language") String acceptLanguage) {
        return partnerService.createPartner(request, phoneNumber, acceptLanguage);
    }

    /**
     * tr: Gövdedeki PartnerRequest ile mevcut partner bilgilerini günceller ve güncellenmiş partneri döner.
     * en: Updates an existing partner's information from the PartnerRequest body and returns the updated partner.
     */
    @PostMapping("/edit")
    public PartnerResponse updatePartner(@RequestBody PartnerRequest request,
                                         @RequestHeader("Authorization") String token,
                                         @RequestHeader("phoneNumber") String phoneNumber,
                                         @RequestHeader("Accept-Language") String acceptLanguage) {
        return partnerService.updatePartner(request, phoneNumber, acceptLanguage);
    }
}
