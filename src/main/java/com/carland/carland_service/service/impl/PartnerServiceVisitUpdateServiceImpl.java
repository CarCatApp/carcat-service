package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.webhook.LineUpdateDetail;
import com.carland.carland_service.dto.response.MoneyResponse;
import com.carland.carland_service.dto.webhook.PartUpdateDetail;
import com.carland.carland_service.dto.webhook.PartnerUpdateServiceVisitResult;
import com.carland.carland_service.dto.response.VisitServiceLineResponse;
import com.carland.carland_service.entity.VisitPart;
import com.carland.carland_service.dto.response.VisitPartResponse;
import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.HyperPercentageSyncService;
import com.carland.carland_service.service.PartnerLookupService;
import com.carland.carland_service.service.PartnerServiceVisitUpdateService;
import com.carland.carland_service.service.webhook.HyperWebhookIngestMapper;
import com.carland.carland_service.service.webhook.HyperServiceVisitValidator;
import com.carland.carland_service.service.webhook.HyperWebhookCarMetadataApplier;
import com.carland.carland_service.service.webhook.VisitWebhookSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * tr: Partner webhook'undan gelen MEVCUT servis ziyareti güncellemelerini işleyen servistir; ziyareti
 *     partnerRecordId ile bulur, alan-alan karşılaştırarak sadece değişenleri yazar, satır ve parçaları
 *     merge eder, değişiklik olduysa toplam maliyet ve percentage senkronunu tetikler.
 * en: Service handling EXISTING service visit updates arriving from the partner webhook; finds the visit
 *     by partnerRecordId, writes only changed fields via field-by-field comparison, merges lines and parts,
 *     and triggers total-cost plus percentage sync when anything changed.
 */
@Service
@RequiredArgsConstructor
public class PartnerServiceVisitUpdateServiceImpl implements PartnerServiceVisitUpdateService {

    private final CarRepository carRepository;
    private final VisitRepository visitRepository;
    private final PartnerLookupService partnerLookupService;
    private final HyperWebhookCarMetadataApplier hyperWebhookCarMetadataApplier;
    private final HyperPercentageSyncService hyperPercentageSyncService;
    private final VisitWebhookSupport visitWebhookSupport;

    /**
     * tr: Güncelleme isteğini işler: payload'ı doğrular, aktif partner'ı ve VIN ile aracı bulur,
     *     ziyareti partnerRecordId ile getirir (yoksa veya başka partner'a aitse ResourceNotFoundException),
     *     ziyaret alanlarını/satırları/parçaları merge eder, değişiklik varsa kaydedip senkronları çalıştırır.
     * en: Processes the update request: validates the payload, resolves the active partner and the car by
     *     VIN, loads the visit by partnerRecordId (ResourceNotFoundException when missing or owned by
     *     another partner), merges visit fields/lines/parts, persists and runs syncs when anything changed.
     */
    @Override
    @Transactional
    public PartnerUpdateServiceVisitResult update(HyperVehicleByVinResponse request) {
        HyperServiceVisitValidator.validateSingleVisit(request);

        Partner partner = partnerLookupService.requireActivePartner(request.getPartnerId());

        String vin = request.getVin().trim();
        Car car = carRepository.findByVin(vin);
        if (car == null) {
            throw new ResourceNotFoundException("Car not found for vin: " + vin);
        }

        hyperWebhookCarMetadataApplier.apply(car, request);

        VisitHistoryItemResponse item = HyperWebhookIngestMapper.toSingleVisitItem(request, partner);
        Long recordId = item.getPartnerRecordId();

        Visit visit = visitRepository.findWithDetailsByCarIdAndHyperRecordId(car.getCarId(), recordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Visit not found for recordId=" + recordId + " and vin=" + vin));
        if (visit.getServiceCenterId() != null && !Objects.equals(visit.getServiceCenterId(), partner.getId())) {
            throw new ResourceNotFoundException(
                    "Visit not found for recordId=" + recordId + " and vin=" + vin);
        }
        visit.getParts().size();

        PartnerUpdateServiceVisitResult result = PartnerUpdateServiceVisitResult.builder()
                .vin(vin)
                .partnerRecordId(recordId)
                .visitId(visit.getId())
                .lines(new ArrayList<>())
                .parts(new ArrayList<>())
                .build();

        visitWebhookSupport.fillPartnerIfMissing(visit, partner);

        int visitFieldsUpdated = applyVisitSnapshot(visit, item);
        int linesUpdated = mergeLines(visit, item, result);
        int partsUpdated = mergeParts(visit, item, result);

        result.setVisitFieldsUpdated(visitFieldsUpdated);
        result.setLinesUpdated(linesUpdated);
        result.setPartsUpdated(partsUpdated);

        boolean changed = visitFieldsUpdated > 0 || linesUpdated > 0 || partsUpdated > 0;
        visit.setUpdatedAt(LocalDateTime.now());
        visitWebhookSupport.appendEditEvent(visit, "PUT", changed, partner);
        visitRepository.saveAndFlush(visit);

        if (changed) {
            visitWebhookSupport.recalculateAllTimeCost(car);
            visitRepository.findWithDetailsByCarIdAndHyperRecordId(car.getCarId(), recordId)
                    .ifPresent(freshVisit -> hyperPercentageSyncService.syncFromVisit(car, freshVisit));
            result.setMessage("Visit and service lines updated");
        } else {
            result.setMessage("Visit and service lines already up to date");
        }

        return result;
    }

    /**
     * tr: Ziyaret üst alanlarını (tip, tarih, km, dealer, fatura no, gruplar, maliyetler) DTO ile
     *     karşılaştırıp farklı olanları günceller; değişen alan sayısını döner.
     * en: Compares the visit's top-level fields (type, date, mileage, dealer, invoice no, groups, costs)
     *     with the DTO and updates differing ones; returns the changed field count.
     */
    private int applyVisitSnapshot(Visit visit, VisitHistoryItemResponse item) {
        int changed = 0;

        if (!Objects.equals(visit.getServiceType(), item.getType())) {
            visit.setServiceType(item.getType());
            changed++;
        }
        if (!Objects.equals(visit.getLastServiceDate(), item.getDate())) {
            visit.setLastServiceDate(item.getDate());
            changed++;
        }
        if (!Objects.equals(visit.getLastServiceMileage(), item.getMileage())) {
            visit.setLastServiceMileage(item.getMileage());
            changed++;
        }
        if (!Objects.equals(visit.getDealer(), item.getDealer())) {
            visit.setDealer(item.getDealer());
            changed++;
        }
        if (!Objects.equals(visit.getInvoiceNumber(), item.getInvoiceNumber())) {
            visit.setInvoiceNumber(item.getInvoiceNumber());
            changed++;
        }

        List<String> serviceGroups = item.getServiceGroups() != null ? new ArrayList<>(item.getServiceGroups()) : new ArrayList<>();
        if (!Objects.equals(visit.getServiceGroups(), serviceGroups)) {
            visit.setServiceGroups(serviceGroups);
            changed++;
        }

        MoneyResponse cost = item.getCost();
        BigDecimal costAmount = cost != null ? cost.getAmount() : null;
        String costCurrency = cost != null ? cost.getCurrency() : null;
        if (!visitWebhookSupport.decimalEquals(visit.getCostAmount(), costAmount)) {
            visit.setCostAmount(costAmount);
            changed++;
        }
        if (!Objects.equals(visit.getCostCurrency(), costCurrency)) {
            visit.setCostCurrency(costCurrency);
            changed++;
        }

        MoneyResponse amount = item.getAmount();
        BigDecimal finalCostAmount = amount != null ? amount.getAmount() : null;
        String finalCostCurrency = amount != null ? amount.getCurrency() : null;
        if (!visitWebhookSupport.decimalEquals(visit.getFinalCostAmount(), finalCostAmount)) {
            visit.setFinalCostAmount(finalCostAmount);
            changed++;
        }
        if (!Objects.equals(visit.getFinalCostCurrency(), finalCostCurrency)) {
            visit.setFinalCostCurrency(finalCostCurrency);
            changed++;
        }

        return changed;
    }

    /**
     * tr: DTO'daki servis satırlarını mevcut ziyaretle merge eder: serviceCode eşleşmesi yoksa yeni satır
     *     ekler, varsa alanları karşılaştırıp günceller; güncellenen satır sayısını döner.
     * en: Merges the DTO's service lines into the existing visit: adds a new line when no serviceCode
     *     match exists, otherwise compares and updates fields; returns the updated line count.
     */
    private int mergeLines(Visit visit, VisitHistoryItemResponse item, PartnerUpdateServiceVisitResult result) {
        int updated = 0;
        for (VisitServiceLineResponse lineRequest : item.getServices()) {
            VisitServiceLine existingLine = findLineByServiceCode(visit, lineRequest.getServiceCode());
            if (existingLine == null) {
                VisitServiceLine createdLine = visitWebhookSupport.mapLineToEntity(lineRequest);
                visit.addService(createdLine);
                updated++;
                result.getLines().add(LineUpdateDetail.builder()
                        .serviceCode(createdLine.getServiceCode())
                        .lineId(createdLine.getId())
                        .updated(true)
                        .build());
                continue;
            }

            boolean lineChanged = applyLineSnapshot(existingLine, lineRequest);
            if (lineChanged) {
                updated++;
            }
            result.getLines().add(LineUpdateDetail.builder()
                    .serviceCode(existingLine.getServiceCode())
                    .lineId(existingLine.getId())
                    .updated(lineChanged)
                    .build());
        }
        return updated;
    }

    /**
     * tr: DTO'daki parçaları mevcut ziyaretle merge eder: ad+miktar+birim anahtarına göre eşleşme yoksa
     *     yeni parça ekler, varsa günceller; güncellenen parça sayısını döner.
     * en: Merges the DTO's parts into the existing visit: adds a new part when no name+qty+unit key match
     *     exists, updates otherwise; returns the updated part count.
     */
    private int mergeParts(Visit visit, VisitHistoryItemResponse item, PartnerUpdateServiceVisitResult result) {
        if (item.getParts() == null || item.getParts().isEmpty()) {
            return 0;
        }

        int updated = 0;
        for (VisitPartResponse partRequest : item.getParts()) {
            VisitPart existingPart = findPartByIdentity(visit, partRequest);
            if (existingPart == null) {
                VisitPart createdPart = visitWebhookSupport.mapPartToEntity(partRequest);
                visit.addPart(createdPart);
                updated++;
                result.getParts().add(PartUpdateDetail.builder()
                        .name(createdPart.getName())
                        .qty(createdPart.getQty())
                        .unit(createdPart.getUnit())
                        .partId(createdPart.getId())
                        .updated(true)
                        .build());
                continue;
            }

            boolean partChanged = applyPartSnapshot(existingPart, partRequest);
            if (partChanged) {
                updated++;
            }
            result.getParts().add(PartUpdateDetail.builder()
                    .name(existingPart.getName())
                    .qty(existingPart.getQty())
                    .unit(existingPart.getUnit())
                    .partId(existingPart.getId())
                    .updated(partChanged)
                    .build());
        }
        return updated;
    }

    /**
     * tr: Tek servis satırının alanlarını DTO ile karşılaştırıp farklı olanları günceller;
     *     en az bir alan değiştiyse true döner.
     * en: Compares a single service line's fields with the DTO and updates differing ones;
     *     returns true when at least one field changed.
     */
    private boolean applyLineSnapshot(VisitServiceLine target, VisitServiceLineResponse source) {
        int changed = 0;

        if (!Objects.equals(target.getServiceName(), source.getServiceName())) {
            target.setServiceName(source.getServiceName());
            changed++;
        }

        String normalizedUniversalServiceId = visitWebhookSupport.normalizeUniversalServiceId(source.getUniversalServiceId());
        if (!Objects.equals(target.getUniversalServiceId(), normalizedUniversalServiceId)) {
            target.setUniversalServiceId(normalizedUniversalServiceId);
            changed++;
        }

        List<String> serviceGroups = source.getServiceGroups() != null
                ? new ArrayList<>(source.getServiceGroups())
                : new ArrayList<>();
        if (!Objects.equals(target.getServiceGroups(), serviceGroups)) {
            target.setServiceGroups(serviceGroups);
            changed++;
        }

        MoneyResponse cost = source.getCost();
        BigDecimal costAmount = cost != null ? cost.getAmount() : null;
        String costCurrency = cost != null ? cost.getCurrency() : null;
        if (!visitWebhookSupport.decimalEquals(target.getCostAmount(), costAmount)) {
            target.setCostAmount(costAmount);
            changed++;
        }
        if (!Objects.equals(target.getCostCurrency(), costCurrency)) {
            target.setCostCurrency(costCurrency);
            changed++;
        }
        if (!Objects.equals(target.getNextServiceDate(), source.getNextServiceDate())) {
            target.setNextServiceDate(source.getNextServiceDate());
            changed++;
        }
        if (!Objects.equals(target.getNextServiceMileage(), source.getNextServiceMileage())) {
            target.setNextServiceMileage(source.getNextServiceMileage());
            changed++;
        }

        return changed > 0;
    }

    /**
     * tr: Tek parçanın alanlarını (ad, miktar, birim) DTO ile karşılaştırıp günceller;
     *     en az bir alan değiştiyse true döner.
     * en: Compares and updates a single part's fields (name, qty, unit) against the DTO;
     *     returns true when at least one field changed.
     */
    private boolean applyPartSnapshot(VisitPart target, VisitPartResponse source) {
        int changed = 0;

        if (!Objects.equals(target.getName(), source.getName())) {
            target.setName(source.getName());
            changed++;
        }
        if (!visitWebhookSupport.decimalEquals(target.getQty(), source.getQty())) {
            target.setQty(source.getQty());
            changed++;
        }
        if (!Objects.equals(target.getUnit(), source.getUnit())) {
            target.setUnit(source.getUnit());
            changed++;
        }

        return changed > 0;
    }

    /**
     * tr: Ziyaretin satırları arasında verilen serviceCode'a sahip olanı bulur; yoksa null döner.
     * en: Finds the visit line with the given serviceCode; returns null when absent.
     */
    private VisitServiceLine findLineByServiceCode(Visit visit, Integer serviceCode) {
        return visit.getServices().stream()
                .filter(line -> Objects.equals(line.getServiceCode(), serviceCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * tr: Ziyaretin parçaları arasında ad+miktar+birim anahtarı eşleşenini bulur; yoksa null döner.
     * en: Finds the visit part matching by the name+qty+unit key; returns null when absent.
     */
    private VisitPart findPartByIdentity(Visit visit, VisitPartResponse partRequest) {
        String key = partKey(partRequest);
        return visit.getParts().stream()
                .filter(part -> partKey(part).equals(key))
                .findFirst()
                .orElse(null);
    }

    /**
     * tr: Parça DTO'sundan karşılaştırma anahtarı üretir (ad|miktar|birim).
     * en: Builds the comparison key (name|qty|unit) from the part DTO.
     */
    private String partKey(VisitPartResponse part) {
        return Objects.toString(part.getName(), "")
                + "|" + normalizeQty(part.getQty())
                + "|" + Objects.toString(part.getUnit(), "");
    }

    /**
     * tr: Parça entity'sinden karşılaştırma anahtarı üretir (ad|miktar|birim).
     * en: Builds the comparison key (name|qty|unit) from the part entity.
     */
    private String partKey(VisitPart part) {
        return Objects.toString(part.getName(), "")
                + "|" + normalizeQty(part.getQty())
                + "|" + Objects.toString(part.getUnit(), "");
    }

    /**
     * tr: Miktarı anahtar üretimi için normalize eder: null ise boş string, değilse sondaki sıfırlar atılmış hali.
     * en: Normalizes the quantity for key building: empty string for null, trailing-zero-stripped plain string otherwise.
     */
    private String normalizeQty(BigDecimal qty) {
        if (qty == null) {
            return "";
        }
        return qty.stripTrailingZeros().toPlainString();
    }
}
