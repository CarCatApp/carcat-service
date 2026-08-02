package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.webhook.PartnerNewServiceVisitResult;

/**
 * tr: Partner webhook'undan gelen yeni servis ziyareti (new-service-visit) verisini sisteme işleyen sözleşmedir.
 * en: Contract for ingesting new service visit data coming from the partner webhook.
 */
public interface PartnerServiceVisitIngestService {

    /**
     * tr: Hyper'dan gelen araç/ziyaret verisini doğrulayıp ilgili araca yeni ziyaret olarak kaydeder ve sonucu döner.
     * en: Validates the vehicle/visit payload from Hyper, persists it as a new visit for the matching car, and returns the result.
     */
    PartnerNewServiceVisitResult ingest(HyperVehicleByVinResponse request);
}
