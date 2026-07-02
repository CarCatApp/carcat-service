package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.response.v2.LineUpdateDetail;
import com.carland.carland_service.dto.response.v2.MoneyResponse;
import com.carland.carland_service.dto.response.v2.PartUpdateDetail;
import com.carland.carland_service.dto.response.v2.PartnerUpdateServiceVisitResult;
import com.carland.carland_service.dto.response.v2.ServiceHistoryLineV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryPartV2;
import com.carland.carland_service.dto.response.v2.ServiceHistoryPartV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryVisitV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryV2;
import com.carland.carland_service.dto.response.v2.Visit;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.HyperPercentageSyncService;
import com.carland.carland_service.service.PartnerLookupService;
import com.carland.carland_service.service.PartnerServiceVisitUpdateService;
import com.carland.carland_service.service.mapper.HyperWebhookIngestMapper;
import com.carland.carland_service.service.validation.HyperServiceVisitValidator;
import com.carland.carland_service.service.webhook.HyperWebhookCarMetadataApplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PartnerServiceVisitUpdateServiceImpl implements PartnerServiceVisitUpdateService {

    private final CarRepository carRepository;
    private final VisitRepository visitRepository;
    private final PartnerLookupService partnerLookupService;
    private final HyperWebhookCarMetadataApplier hyperWebhookCarMetadataApplier;
    private final HyperPercentageSyncService hyperPercentageSyncService;

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

        ServiceHistoryVisitV2Response item = HyperWebhookIngestMapper.toSingleVisitItem(request, partner);
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

        int visitFieldsUpdated = applyVisitSnapshot(visit, item);
        int linesUpdated = mergeLines(visit, item, result);
        int partsUpdated = mergeParts(visit, item, result);

        result.setVisitFieldsUpdated(visitFieldsUpdated);
        result.setLinesUpdated(linesUpdated);
        result.setPartsUpdated(partsUpdated);

        boolean changed = visitFieldsUpdated > 0 || linesUpdated > 0 || partsUpdated > 0;
        if (changed) {
            visitRepository.saveAndFlush(visit);
            recalculateAllTimeCost(car);
            visitRepository.findWithDetailsByCarIdAndHyperRecordId(car.getCarId(), recordId)
                    .ifPresent(freshVisit -> hyperPercentageSyncService.syncFromVisit(car, freshVisit));
            result.setMessage("Visit and service lines updated");
        } else {
            result.setMessage("Visit and service lines already up to date");
        }

        return result;
    }

    private int applyVisitSnapshot(Visit visit, ServiceHistoryVisitV2Response item) {
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
        if (!decimalEquals(visit.getCostAmount(), costAmount)) {
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
        if (!decimalEquals(visit.getFinalCostAmount(), finalCostAmount)) {
            visit.setFinalCostAmount(finalCostAmount);
            changed++;
        }
        if (!Objects.equals(visit.getFinalCostCurrency(), finalCostCurrency)) {
            visit.setFinalCostCurrency(finalCostCurrency);
            changed++;
        }

        return changed;
    }

    private int mergeLines(Visit visit, ServiceHistoryVisitV2Response item, PartnerUpdateServiceVisitResult result) {
        int updated = 0;
        for (ServiceHistoryLineV2Response lineRequest : item.getServices()) {
            ServiceHistoryV2 existingLine = findLineByServiceCode(visit, lineRequest.getServiceCode());
            if (existingLine == null) {
                ServiceHistoryV2 createdLine = mapLineToEntity(lineRequest);
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

    private int mergeParts(Visit visit, ServiceHistoryVisitV2Response item, PartnerUpdateServiceVisitResult result) {
        if (item.getParts() == null || item.getParts().isEmpty()) {
            return 0;
        }

        int updated = 0;
        for (ServiceHistoryPartV2Response partRequest : item.getParts()) {
            ServiceHistoryPartV2 existingPart = findPartByIdentity(visit, partRequest);
            if (existingPart == null) {
                ServiceHistoryPartV2 createdPart = mapPartToEntity(partRequest);
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

    private boolean applyLineSnapshot(ServiceHistoryV2 target, ServiceHistoryLineV2Response source) {
        int changed = 0;

        if (!Objects.equals(target.getServiceName(), source.getServiceName())) {
            target.setServiceName(source.getServiceName());
            changed++;
        }

        String normalizedUniversalServiceId = normalizeUniversalServiceId(source.getUniversalServiceId());
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
        if (!decimalEquals(target.getCostAmount(), costAmount)) {
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

    private boolean applyPartSnapshot(ServiceHistoryPartV2 target, ServiceHistoryPartV2Response source) {
        int changed = 0;

        if (!Objects.equals(target.getName(), source.getName())) {
            target.setName(source.getName());
            changed++;
        }
        if (!decimalEquals(target.getQty(), source.getQty())) {
            target.setQty(source.getQty());
            changed++;
        }
        if (!Objects.equals(target.getUnit(), source.getUnit())) {
            target.setUnit(source.getUnit());
            changed++;
        }

        return changed > 0;
    }

    private ServiceHistoryV2 findLineByServiceCode(Visit visit, Integer serviceCode) {
        return visit.getServices().stream()
                .filter(line -> Objects.equals(line.getServiceCode(), serviceCode))
                .findFirst()
                .orElse(null);
    }

    private ServiceHistoryPartV2 findPartByIdentity(Visit visit, ServiceHistoryPartV2Response partRequest) {
        String key = partKey(partRequest);
        return visit.getParts().stream()
                .filter(part -> partKey(part).equals(key))
                .findFirst()
                .orElse(null);
    }

    private ServiceHistoryV2 mapLineToEntity(ServiceHistoryLineV2Response line) {
        MoneyResponse cost = line.getCost();
        return ServiceHistoryV2.builder()
                .serviceCode(line.getServiceCode())
                .serviceName(line.getServiceName())
                .universalServiceId(normalizeUniversalServiceId(line.getUniversalServiceId()))
                .serviceGroups(line.getServiceGroups() != null ? new ArrayList<>(line.getServiceGroups()) : new ArrayList<>())
                .costAmount(cost != null ? cost.getAmount() : null)
                .costCurrency(cost != null ? cost.getCurrency() : null)
                .nextServiceDate(line.getNextServiceDate())
                .nextServiceMileage(line.getNextServiceMileage())
                .build();
    }

    private ServiceHistoryPartV2 mapPartToEntity(ServiceHistoryPartV2Response part) {
        return ServiceHistoryPartV2.builder()
                .name(part.getName())
                .qty(part.getQty())
                .unit(part.getUnit())
                .build();
    }

    private String partKey(ServiceHistoryPartV2Response part) {
        return Objects.toString(part.getName(), "")
                + "|" + normalizeQty(part.getQty())
                + "|" + Objects.toString(part.getUnit(), "");
    }

    private String partKey(ServiceHistoryPartV2 part) {
        return Objects.toString(part.getName(), "")
                + "|" + normalizeQty(part.getQty())
                + "|" + Objects.toString(part.getUnit(), "");
    }

    private String normalizeQty(BigDecimal qty) {
        if (qty == null) {
            return "";
        }
        return qty.stripTrailingZeros().toPlainString();
    }

    private String normalizeUniversalServiceId(String raw) {
        if (raw == null || raw.isBlank() || "other".equalsIgnoreCase(raw.trim())) {
            return "";
        }
        return raw.trim();
    }

    private boolean decimalEquals(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private void recalculateAllTimeCost(Car car) {
        BigDecimal total = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car).stream()
                .map(visit -> visit.getFinalCostAmount() != null ? visit.getFinalCostAmount()
                        : visit.getCostAmount() != null ? visit.getCostAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        car.setAllTimeCost(total);
        carRepository.save(car);
    }
}
