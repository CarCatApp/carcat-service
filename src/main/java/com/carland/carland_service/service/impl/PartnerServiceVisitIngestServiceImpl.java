package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.response.VisitHistoryResponse;
import com.carland.carland_service.dto.webhook.LineIngestDetail;
import com.carland.carland_service.dto.response.MoneyResponse;
import com.carland.carland_service.dto.webhook.PartnerNewServiceVisitResult;
import com.carland.carland_service.dto.response.VisitServiceLineResponse;
import com.carland.carland_service.dto.response.VisitPartResponse;
import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.dto.webhook.VisitIngestDetail;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.HyperPercentageSyncService;
import com.carland.carland_service.service.PartnerLookupService;
import com.carland.carland_service.service.PartnerServiceVisitIngestService;
import com.carland.carland_service.service.webhook.HyperWebhookIngestMapper;
import com.carland.carland_service.service.webhook.HyperServiceVisitValidator;
import com.carland.carland_service.service.webhook.HyperWebhookCarMetadataApplier;
import com.carland.carland_service.service.webhook.PartnerVisitIngestGuard;
import com.carland.carland_service.service.webhook.VisitWebhookSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * tr: Partner webhook'undan gelen YENİ servis ziyaretlerini işleyen servistir; payload'ı doğrular,
 *     partner ve aracı bulur, ziyaretleri Visit entity'lerine çevirip kaydeder, aracın toplam maliyetini,
 *     partner listesini ve yüzde (percentage) senkronunu günceller.
 * en: Service handling NEW service visits arriving from the partner webhook; validates the payload,
 *     resolves partner and car, maps visits to Visit entities and persists them, then refreshes the car's
 *     total cost, partner list and percentage sync.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerServiceVisitIngestServiceImpl implements PartnerServiceVisitIngestService {

    private static final PartnerId DEFAULT_PARTNER = PartnerId.HYPER;

    private final CarRepository carRepository;
    private final VisitRepository visitRepository;
    private final PartnerLookupService partnerLookupService;
    private final HyperWebhookCarMetadataApplier hyperWebhookCarMetadataApplier;
    private final HyperPercentageSyncService hyperPercentageSyncService;
    private final PartnerVisitIngestGuard partnerVisitIngestGuard;
    private final VisitWebhookSupport visitWebhookSupport;

    /**
     * tr: Webhook isteğini işler: payload'da tek ziyaret olmasını doğrular (validator exception fırlatır),
     *     aktif partner'ı zorunlu kılar, VIN ile aracı bulur (yoksa ResourceNotFoundException),
     *     araç metadata'sını günceller ve ziyaretleri kaydeder. Sonuçta oluşturulan kayıt sayıları döner.
     * en: Processes the webhook request: validates single-visit payload (validator throws),
     *     requires an active partner, finds the car by VIN (ResourceNotFoundException otherwise),
     *     applies car metadata and persists the visits. Returns created record counts as the result.
     */
    @Override
    @Transactional
    public PartnerNewServiceVisitResult ingest(HyperVehicleByVinResponse request) {
        HyperServiceVisitValidator.validateSingleVisit(request);

        Partner partner = partnerLookupService.requireActivePartner(request.getPartnerId());

        String vin = request.getVin().trim();
        Car car = carRepository.findByVin(vin);
        if (car == null) {
            throw new ResourceNotFoundException("Car not found for vin: " + vin);
        }

        hyperWebhookCarMetadataApplier.apply(car, request);
        return ingestVisits(car, HyperWebhookIngestMapper.toIngestRequest(request, partner), partner.getId());
    }

    /**
     * tr: Ziyaret listesini tek tek işler: her ziyaretin yeni olduğunu guard ile doğrular (mükerrer ise
     *     exception), entity'ye çevirip kaydeder; sonra toplam maliyet, partner listesi ve percentage
     *     senkronunu tetikler.
     * en: Processes the visit list one by one: asserts each visit is new via the guard (throws on
     *     duplicates), maps and persists them; then triggers total-cost, partner-list and percentage sync.
     */
    private PartnerNewServiceVisitResult ingestVisits(Car car, VisitHistoryResponse request, Long partnerId) {
        String vin = request.getVin().trim();

        PartnerNewServiceVisitResult result = PartnerNewServiceVisitResult.builder()
                .vin(vin)
                .visits(new ArrayList<>())
                .build();

        List<Visit> touchedVisits = new ArrayList<>();

        for (VisitHistoryItemResponse item : request.getItems()) {
            partnerVisitIngestGuard.assertNewVisit(car.getCarId(), item);

            Visit created = mapItemToVisit(car, item);
            visitRepository.saveAndFlush(created);
            touchedVisits.add(created);
            result.getVisits().add(buildCreatedVisitDetail(created, item.getPartnerRecordId()));
            result.setVisitsCreated(result.getVisitsCreated() + 1);
            result.setLinesCreated(result.getLinesCreated() + sizeOf(item.getServices()));
            result.setPartsCreated(result.getPartsCreated() + sizeOf(item.getParts()));
        }

        visitWebhookSupport.recalculateAllTimeCost(car);
        refreshServicedPartnerIds(car);
        refreshPercentagesFromTouchedVisits(car, touchedVisits);

        result.setMessage("Visit and service lines created");
        return result;
    }

    /**
     * tr: Yeni eklenen ziyaretleri DB'den detaylarıyla tekrar okuyup her biri için percentage senkronu çalıştırır.
     * en: Re-reads the newly added visits with details from the DB and runs percentage sync for each.
     */
    private void refreshPercentagesFromTouchedVisits(Car car, List<Visit> touchedVisits) {
        for (Visit visit : touchedVisits) {
            if (visit.getHyperRecordId() == null) {
                continue;
            }
            visitRepository.findWithDetailsByCarIdAndHyperRecordId(car.getCarId(), visit.getHyperRecordId())
                    .ifPresent(freshVisit -> hyperPercentageSyncService.syncFromVisit(car, freshVisit));
        }
    }

    /**
     * tr: Kaydedilen ziyaret için webhook cevabında dönen detay nesnesini (ziyaret + satır id'leri) kurar.
     * en: Builds the detail object (visit + line ids) returned in the webhook response for the saved visit.
     */
    private VisitIngestDetail buildCreatedVisitDetail(Visit visit, Long partnerRecordId) {
        List<LineIngestDetail> lines = new ArrayList<>();
        if (visit.getServices() != null) {
            for (VisitServiceLine line : visit.getServices()) {
                lines.add(LineIngestDetail.builder()
                        .serviceCode(line.getServiceCode())
                        .lineId(line.getId())
                        .created(true)
                        .build());
            }
        }
        return VisitIngestDetail.builder()
                .partnerRecordId(partnerRecordId)
                .visitId(visit.getId())
                .visitCreated(true)
                .lines(lines)
                .build();
    }

    /**
     * tr: Webhook ziyaret DTO'sunu satır ve parçalarıyla birlikte Visit entity'sine çevirir;
     *     servis merkezi bilgisi yoksa varsayılan partner'ı (HYPER) kullanır.
     * en: Maps the webhook visit DTO, including its lines and parts, to a Visit entity;
     *     falls back to the default partner (HYPER) when service center info is missing.
     */
    private Visit mapItemToVisit(Car car, VisitHistoryItemResponse item) {
        Long partnerId = item.getServiceCenterId() != null ? item.getServiceCenterId() : DEFAULT_PARTNER.getId();
        String partnerName = resolvePartnerName(item, partnerId);

        MoneyResponse amount = item.getAmount();
        MoneyResponse cost = item.getCost();
        Visit visit = Visit.builder()
                .car(car)
                .hyperRecordId(item.getPartnerRecordId())
                .serviceType(item.getType())
                .lastServiceDate(item.getDate())
                .lastServiceMileage(item.getMileage())
                .dealer(item.getDealer())
                .invoiceNumber(item.getInvoiceNumber())
                .serviceCenterId(partnerId)
                .serviceCenterName(partnerName)
                .costAmount(cost != null ? cost.getAmount() : null)
                .costCurrency(cost != null ? cost.getCurrency() : null)
                .finalCostAmount(amount != null ? amount.getAmount() : null)
                .finalCostCurrency(amount != null ? amount.getCurrency() : null)
                .serviceGroups(item.getServiceGroups() != null ? new ArrayList<>(item.getServiceGroups()) : new ArrayList<>())
                .build();

        if (item.getServices() != null) {
            for (VisitServiceLineResponse line : item.getServices()) {
                visit.addService(visitWebhookSupport.mapLineToEntity(line));
            }
        }
        if (item.getParts() != null) {
            for (VisitPartResponse part : item.getParts()) {
                visit.addPart(visitWebhookSupport.mapPartToEntity(part));
            }
        }
        return visit;
    }

    /**
     * tr: Servis merkezi adını belirler: DTO'da doluysa onu, yoksa partner kaydındaki adı,
     *     o da yoksa varsayılan partner adını döner.
     * en: Resolves the service center name: the DTO value when present, otherwise the partner record's
     *     name, falling back to the default partner name.
     */
    private String resolvePartnerName(VisitHistoryItemResponse item, Long partnerId) {
        if (item.getServiceCenterName() != null && !item.getServiceCenterName().isBlank()) {
            return item.getServiceCenterName();
        }
        return partnerLookupService.find(PartnerId.fromId(partnerId).orElse(DEFAULT_PARTNER))
                .map(Partner::getName)
                .orElse(DEFAULT_PARTNER.getDefaultName());
    }

    /**
     * tr: Aracın servicedPartnerIds listesini tüm ziyaretlerden (en yeniden eskiye) yeniden kurar ve kaydeder.
     * en: Rebuilds and persists the car's servicedPartnerIds list from all visits (newest to oldest).
     */
    private void refreshServicedPartnerIds(Car car) {
        List<Visit> allVisits = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car);
        LinkedHashSet<String> orderedPartnerIds = new LinkedHashSet<>();
        for (Visit visit : allVisits) {
            if (visit.getServiceCenterId() != null) {
                orderedPartnerIds.add(String.valueOf(visit.getServiceCenterId()));
            }
        }
        car.setServicedPartnerIds(new ArrayList<>(orderedPartnerIds));
        carRepository.save(car);
    }

    /**
     * tr: Liste null ise 0, değilse eleman sayısını döner.
     * en: Returns 0 for a null list, the element count otherwise.
     */
    private int sizeOf(List<?> items) {
        return items == null ? 0 : items.size();
    }
}
