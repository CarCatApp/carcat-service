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
import com.carland.carland_service.service.webhook.PartnerVisitIngestGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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
    private final PartnerVisitIngestGuard partnerVisitIngestGuard;

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
            partnerVisitIngestGuard.assertNewVisit(car.getCarId(), item);

            Visit created = mapItemToVisit(car, item);
            visitRepository.saveAndFlush(created);
            touchedVisits.add(created);
            result.getVisits().add(buildCreatedVisitDetail(created, item.getPartnerRecordId()));
            result.setVisitsCreated(result.getVisitsCreated() + 1);
            result.setLinesCreated(result.getLinesCreated() + sizeOf(item.getServices()));
            result.setPartsCreated(result.getPartsCreated() + sizeOf(item.getParts()));
        }

        recalculateAllTimeCost(car);
        refreshServicedPartnerIds(car);
        refreshPercentagesFromTouchedVisits(car, touchedVisits);

        result.setMessage("Visit and service lines created");
        return result;
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
