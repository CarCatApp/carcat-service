package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.webhook.PartnerUpdateServiceVisitResult;

/**
 * tr: Partner webhook'undan gelen servis ziyareti güncelleme (update-service-visit) verisini işleyen sözleşmedir.
 * en: Contract for processing service visit update data coming from the partner webhook.
 */
public interface PartnerServiceVisitUpdateService {
    /**
     * tr: Hyper'dan gelen güncelleme verisini doğrulayıp mevcut ziyareti günceller ve sonucu döner.
     * en: Validates the update payload from Hyper, updates the existing visit, and returns the result.
     */
    PartnerUpdateServiceVisitResult update(HyperVehicleByVinResponse request);
}
