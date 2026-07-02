package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.response.v2.CarVinServiceHistoryV2Response;
import com.carland.carland_service.dto.response.v2.LineIngestDetail;
import com.carland.carland_service.dto.response.v2.MoneyResponse;
import com.carland.carland_service.dto.response.v2.PartnerNewServiceVisitResult;
import com.carland.carland_service.dto.response.v2.ServiceHistoryLineV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryPartV2;
import com.carland.carland_service.dto.response.v2.ServiceHistoryPartV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryVisitV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryV2;
import com.carland.carland_service.dto.response.v2.Visit;
import com.carland.carland_service.dto.response.v2.VisitIngestDetail;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.EnumPartnerId;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.HyperPercentageSyncService;
import com.carland.carland_service.service.PartnerLookupService;
import com.carland.carland_service.service.PartnerServiceVisitIngestService;
import com.carland.carland_service.service.mapper.HyperWebhookIngestMapper;
import com.carland.carland_service.service.validation.HyperServiceVisitValidator;
import com.carland.carland_service.service.webhook.HyperWebhookCarMetadataApplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerServiceVisitIngestServiceImpl implements PartnerServiceVisitIngestService {

    private static final EnumPartnerId DEFAULT_PARTNER = EnumPartnerId.HYPER;

    private final CarRepository carRepository;
    private final VisitRepository visitRepository;
    private final PartnerLookupService partnerLookupService;
    private final HyperWebhookCarMetadataApplier hyperWebhookCarMetadataApplier;
    private final HyperPercentageSyncService hyperPercentageSyncService;

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

    private PartnerNewServiceVisitResult ingestVisits(Car car, CarVinServiceHistoryV2Response request, Long partnerId) {
        String vin = request.getVin().trim();

        PartnerNewServiceVisitResult result = PartnerNewServiceVisitResult.builder()
                .vin(vin)
                .visits(new ArrayList<>())
                .build();

        List<Visit> touchedVisits = new ArrayList<>();

        for (ServiceHistoryVisitV2Response item : request.getItems()) {
            Optional<Visit> existing = visitRepository.findWithDetailsByCarIdAndHyperRecordId(
                    car.getCarId(), item.getPartnerRecordId());
            if (existing.isPresent()) {
                Visit visit = existing.get();
                assertVisitBelongsToPartner(visit, partnerId, item.getPartnerRecordId(), vin);
                visit.getParts().size();
                VisitIngestDetail detail = appendMissingLinesAndParts(visit, item, result);
                touchedVisits.add(visit);
                result.getVisits().add(detail);
                result.setVisitsSkipped(result.getVisitsSkipped() + 1);
                continue;
            }

            Visit created = mapItemToVisit(car, item);
            visitRepository.saveAndFlush(created);
            touchedVisits.add(created);
            result.getVisits().add(buildCreatedVisitDetail(created, item.getPartnerRecordId()));
            result.setVisitsCreated(result.getVisitsCreated() + 1);
            result.setLinesCreated(result.getLinesCreated() + sizeOf(item.getServices()));
            result.setPartsCreated(result.getPartsCreated() + sizeOf(item.getParts()));
        }

        if (result.getVisitsCreated() > 0 || result.getLinesCreated() > 0 || result.getPartsCreated() > 0) {
            recalculateAllTimeCost(car);
            refreshServicedPartnerIds(car);
            refreshPercentagesFromTouchedVisits(car, touchedVisits);
        }

        result.setMessage(resolveMessage(result));
        return result;
    }

    private void assertVisitBelongsToPartner(Visit visit, Long partnerId, Long recordId, String vin) {
        if (visit.getServiceCenterId() != null && !Objects.equals(visit.getServiceCenterId(), partnerId)) {
            throw new ResourceNotFoundException(
                    "Visit not found for recordId=" + recordId + " and vin=" + vin);
        }
    }

    private void refreshPercentagesFromTouchedVisits(Car car, List<Visit> touchedVisits) {
        for (Visit visit : touchedVisits) {
            if (visit.getHyperRecordId() == null) {
                continue;
            }
            visitRepository.findWithDetailsByCarIdAndHyperRecordId(car.getCarId(), visit.getHyperRecordId())
                    .ifPresent(freshVisit -> hyperPercentageSyncService.syncFromVisit(car, freshVisit));
        }
    }

    private VisitIngestDetail buildCreatedVisitDetail(Visit visit, Long partnerRecordId) {
        List<LineIngestDetail> lines = new ArrayList<>();
        if (visit.getServices() != null) {
            for (ServiceHistoryV2 line : visit.getServices()) {
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

    private VisitIngestDetail appendMissingLinesAndParts(Visit visit, ServiceHistoryVisitV2Response item, PartnerNewServiceVisitResult result) {
        Set<String> existingLineKeys = new LinkedHashSet<>();
        for (ServiceHistoryV2 line : visit.getServices()) {
            existingLineKeys.add(lineKey(line));
        }

        if (item.getServices() != null) {
            for (ServiceHistoryLineV2Response line : item.getServices()) {
                if (findMatchingLine(visit, line) != null) {
                    result.setLinesSkipped(result.getLinesSkipped() + 1);
                    continue;
                }
                visit.addService(mapLineToEntity(line));
                result.setLinesCreated(result.getLinesCreated() + 1);
            }
        }
        if (item.getParts() != null) {
            for (ServiceHistoryPartV2Response part : item.getParts()) {
                if (partExists(visit, part)) {
                    result.setPartsSkipped(result.getPartsSkipped() + 1);
                    continue;
                }
                visit.addPart(mapPartToEntity(part));
                result.setPartsCreated(result.getPartsCreated() + 1);
            }
        }
        visitRepository.saveAndFlush(visit);

        List<LineIngestDetail> lineDetails = new ArrayList<>();
        if (item.getServices() != null) {
            for (ServiceHistoryLineV2Response line : item.getServices()) {
                ServiceHistoryV2 matched = findMatchingLine(visit, line);
                if (matched == null) {
                    continue;
                }
                lineDetails.add(LineIngestDetail.builder()
                        .serviceCode(matched.getServiceCode())
                        .lineId(matched.getId())
                        .created(!existingLineKeys.contains(lineKey(line)))
                        .build());
            }
        }

        return VisitIngestDetail.builder()
                .partnerRecordId(item.getPartnerRecordId())
                .visitId(visit.getId())
                .visitCreated(false)
                .lines(lineDetails)
                .build();
    }

    private ServiceHistoryV2 findMatchingLine(Visit visit, ServiceHistoryLineV2Response line) {
        String key = lineKey(line);
        return visit.getServices().stream()
                .filter(existing -> lineKey(existing).equals(key))
                .findFirst()
                .orElse(null);
    }

    private String resolveMessage(PartnerNewServiceVisitResult result) {
        if (result.getVisitsCreated() > 0 || result.getLinesCreated() > 0 || result.getPartsCreated() > 0) {
            if (result.getVisitsSkipped() > 0 || result.getLinesSkipped() > 0 || result.getPartsSkipped() > 0) {
                return "Visit updated with new service lines or parts";
            }
            return "Visit and service lines created";
        }
        return "Visit and service lines already exist";
    }

    private Visit mapItemToVisit(Car car, ServiceHistoryVisitV2Response item) {
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
            for (ServiceHistoryLineV2Response line : item.getServices()) {
                visit.addService(mapLineToEntity(line));
            }
        }
        if (item.getParts() != null) {
            for (ServiceHistoryPartV2Response part : item.getParts()) {
                visit.addPart(mapPartToEntity(part));
            }
        }
        return visit;
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

    private boolean partExists(Visit visit, ServiceHistoryPartV2Response part) {
        String key = partKey(part);
        return visit.getParts().stream().anyMatch(existing -> partKey(existing).equals(key));
    }

    private String lineKey(ServiceHistoryLineV2Response line) {
        return Objects.toString(line.getServiceCode(), "")
                + "|" + normalizeUniversalServiceId(line.getUniversalServiceId())
                + "|" + Objects.toString(line.getServiceName(), "");
    }

    private String lineKey(ServiceHistoryV2 line) {
        return Objects.toString(line.getServiceCode(), "")
                + "|" + normalizeUniversalServiceId(line.getUniversalServiceId())
                + "|" + Objects.toString(line.getServiceName(), "");
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

    private String resolvePartnerName(ServiceHistoryVisitV2Response item, Long partnerId) {
        if (item.getServiceCenterName() != null && !item.getServiceCenterName().isBlank()) {
            return item.getServiceCenterName();
        }
        return partnerLookupService.find(EnumPartnerId.fromId(partnerId).orElse(DEFAULT_PARTNER))
                .map(Partner::getName)
                .orElse(DEFAULT_PARTNER.getDefaultName());
    }

    private void recalculateAllTimeCost(Car car) {
        BigDecimal total = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car).stream()
                .map(this::resolveVisitFinalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        car.setAllTimeCost(total);
        carRepository.save(car);
    }

    private BigDecimal resolveVisitFinalCost(Visit visit) {
        if (visit.getFinalCostAmount() != null) {
            return visit.getFinalCostAmount();
        }
        if (visit.getCostAmount() != null) {
            return visit.getCostAmount();
        }
        return BigDecimal.ZERO;
    }

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

    private String normalizeUniversalServiceId(String raw) {
        if (raw == null || raw.isBlank() || "other".equalsIgnoreCase(raw.trim())) {
            return "";
        }
        return raw.trim();
    }

    private int sizeOf(List<?> items) {
        return items == null ? 0 : items.size();
    }
}
