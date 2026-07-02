package com.carland.carland_service.service.mapper;

import com.carland.carland_service.dto.response.hyper.HyperCostResponse;
import com.carland.carland_service.dto.response.hyper.HyperServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.hyper.HyperServiceLineResponse;
import com.carland.carland_service.dto.response.hyper.HyperServicePartResponse;
import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.dto.response.v2.CarVinServiceHistoryV2Response;
import com.carland.carland_service.dto.response.v2.MoneyResponse;
import com.carland.carland_service.dto.response.v2.ServiceHistoryLineV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryPartV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryVisitV2Response;

import java.util.Collections;
import java.util.List;

public final class HyperWebhookIngestMapper {

    private HyperWebhookIngestMapper() {
    }

    public static ServiceHistoryVisitV2Response toSingleVisitItem(HyperVehicleByVinResponse hyper, Partner partner) {
        List<HyperServiceHistoryItemResponse> history = hyper.getServiceHistory();
        return toVisitItem(history.get(0), partner);
    }

    public static CarVinServiceHistoryV2Response toIngestRequest(HyperVehicleByVinResponse hyper, Partner partner) {
        List<HyperServiceHistoryItemResponse> history = hyper.getServiceHistory() == null
                ? Collections.emptyList()
                : hyper.getServiceHistory();

        return CarVinServiceHistoryV2Response.builder()
                .vin(hyper.getVin())
                .source(partner.getSource() != null ? partner.getSource() : "hyper")
                .items(history.stream().map(item -> toVisitItem(item, partner)).toList())
                .build();
    }

    private static ServiceHistoryVisitV2Response toVisitItem(HyperServiceHistoryItemResponse item, Partner partner) {
        HyperCostResponse finalCost = item.getFinalCost() != null ? item.getFinalCost() : item.getCost();

        return ServiceHistoryVisitV2Response.builder()
                .partnerRecordId(item.getRecordId())
                .type(item.getServiceType())
                .date(item.getLastServiceDate())
                .mileage(item.getLastServiceMileage())
                .dealer(item.getDealer())
                .serviceGroups(item.getServiceGroups())
                .invoiceNumber(item.getInvoiceNumber())
                .serviceCenterId(partner.getId())
                .serviceCenterName(partner.getName())
                .cost(toMoney(item.getCost()))
                .amount(toMoney(finalCost))
                .services(mapLines(item.getServices()))
                .parts(mapParts(item.getParts()))
                .build();
    }

    private static List<ServiceHistoryLineV2Response> mapLines(List<HyperServiceLineResponse> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        return lines.stream()
                .map(line -> ServiceHistoryLineV2Response.builder()
                        .serviceCode(line.getServiceCode())
                        .serviceName(line.getServiceName())
                        .universalServiceId(line.getUniversalServiceId())
                        .serviceGroups(line.getServiceGroups())
                        .cost(toMoney(line.getCost()))
                        .nextServiceDate(line.getNextServiceDate())
                        .nextServiceMileage(line.getNextServiceMileage())
                        .build())
                .toList();
    }

    private static List<ServiceHistoryPartV2Response> mapParts(List<HyperServicePartResponse> parts) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        return parts.stream()
                .map(part -> ServiceHistoryPartV2Response.builder()
                        .name(part.getName())
                        .qty(part.getQty())
                        .unit(part.getUnit())
                        .build())
                .toList();
    }

    private static MoneyResponse toMoney(HyperCostResponse cost) {
        if (cost == null || (cost.getAmount() == null && cost.getCurrency() == null)) {
            return null;
        }
        return MoneyResponse.builder()
                .amount(cost.getAmount())
                .currency(cost.getCurrency() != null ? cost.getCurrency() : "AZN")
                .build();
    }
}
