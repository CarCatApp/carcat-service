package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.webhook.PartnerNewServiceVisitResult;
import com.carland.carland_service.dto.webhook.PartnerUpdateServiceVisitResult;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.service.PartnerServiceVisitIngestService;
import com.carland.carland_service.service.PartnerServiceVisitUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Partner webhook REST controller'ı; partnerlerden gelen yeni servis ziyareti ve servis ziyareti güncelleme webhook'larını işler, VIN ile araç varlık kontrolü ve test ucu sunar.
 * en: REST controller for partner webhooks; processes incoming new-service-visit and update-service-visit webhooks from partners, and offers a VIN-based car existence check and a test endpoint.
 */
@RestController
@RequestMapping("/webhook/partner")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private static final String DELIVERY_SOURCE_HEADER = "X-Webhook-Delivery";
    private static final String DELIVERY_RABBIT_REPLAY = "rabbit-replay";

    private final CarRepository carRepository;
    private final PartnerServiceVisitIngestService partnerServiceVisitIngestService;
    private final PartnerServiceVisitUpdateService partnerServiceVisitUpdateService;

    /**
     * tr: Webhook ucunun erişilebilir olduğunu doğrulamak için sabit bir başarı mesajı döner.
     * en: Returns a fixed success message to verify that the webhook endpoint is reachable.
     */
    @GetMapping("/test")
    public String test() {
        return "test uğurlu oldu";
    }

    /**
     * tr: Verilen VIN'e sahip aracın kayıtlı olup olmadığını kontrol eder; vin/partnerId boşsa veya araç bulunamazsa 404, bulunursa 200 döner.
     * en: Checks whether a car with the given VIN exists; returns 404 if vin/partnerId is missing or the car is not found, 200 otherwise.
     */
    @GetMapping("/car/find")
    public ResponseEntity<Void> findCarByVin(@RequestParam String vin, @RequestParam Long partnerId) {
        if (vin == null || vin.isBlank() || partnerId == null || carRepository.findByVin(vin.trim()) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * tr: Partnerden gelen yeni servis ziyareti webhook'unu işler; gövdedeki HyperVehicleByVinResponse verisini ingest servisine iletir, kayıt oluşturulduysa 200, hiçbir şey oluşmadıysa 409 döner.
     * en: Processes the partner's new-service-visit webhook; passes the HyperVehicleByVinResponse body to the ingest service, returns 200 if any records were created, 409 if nothing was created.
     */
    @PostMapping("/new-service-visit")
    public ResponseEntity<PartnerNewServiceVisitResult> newServiceVisit(HttpServletRequest httpRequest,
                                                                        @RequestBody HyperVehicleByVinResponse request) {
        logRabbitReplayIfNeeded(httpRequest);
        PartnerNewServiceVisitResult result = partnerServiceVisitIngestService.ingest(request);
        return ResponseEntity.status(resolveIngestStatus(result)).body(result);
    }

    /**
     * tr: Partnerden gelen servis ziyareti güncelleme webhook'unu işler; herhangi bir alan/satır/parça güncellendiyse 200, hiçbir şey değişmediyse 409 döner.
     * en: Processes the partner's update-service-visit webhook; returns 200 if any visit fields/lines/parts were updated, 409 if nothing changed.
     */
    @PutMapping("/edit/service-visit")
    public ResponseEntity<PartnerUpdateServiceVisitResult> updateServiceVisit(
            HttpServletRequest httpRequest,
            @RequestBody HyperVehicleByVinResponse request) {
        logRabbitReplayIfNeeded(httpRequest);
        PartnerUpdateServiceVisitResult result = partnerServiceVisitUpdateService.update(request);
        return ResponseEntity.status(resolveUpdateStatus(result)).body(result);
    }

    private void logRabbitReplayIfNeeded(HttpServletRequest request) {
        if (DELIVERY_RABBIT_REPLAY.equals(request.getHeader(DELIVERY_SOURCE_HEADER))) {
            log.info("yuxudan oyandim rabbit istek gonderdi");
        }
    }

    private HttpStatus resolveUpdateStatus(PartnerUpdateServiceVisitResult result) {
        if (result.getVisitFieldsUpdated() > 0 || result.getLinesUpdated() > 0 || result.getPartsUpdated() > 0) {
            return HttpStatus.OK;
        }
        return HttpStatus.CONFLICT;
    }

    private HttpStatus resolveIngestStatus(PartnerNewServiceVisitResult result) {
        if (result.getVisitsCreated() > 0 || result.getLinesCreated() > 0 || result.getPartsCreated() > 0) {
            return HttpStatus.OK;
        }
        return HttpStatus.CONFLICT;
    }
}
