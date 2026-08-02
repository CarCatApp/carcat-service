package com.carland.carland_service.service.webhook;

import com.carland.carland_service.dto.response.hyper.HyperCostResponse;
import com.carland.carland_service.dto.response.hyper.HyperServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.hyper.HyperServiceLineResponse;
import com.carland.carland_service.dto.response.hyper.HyperServicePartResponse;
import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.dto.response.VisitHistoryResponse;
import com.carland.carland_service.dto.response.MoneyResponse;
import com.carland.carland_service.dto.response.VisitServiceLineResponse;
import com.carland.carland_service.dto.response.VisitPartResponse;
import com.carland.carland_service.dto.response.VisitHistoryItemResponse;

import java.util.Collections;
import java.util.List;

/**
 * tr: Hyper webhook DTO'larını (HyperVehicleByVinResponse) iç ziyaret modeline (VisitHistoryResponse / VisitHistoryItemResponse) çeviren yardımcı sınıf (utility); ziyaret, servis satırı, parça ve tutar alanlarını haritalar. Örneklenemez, sadece statik metodlar içerir.
 * en: Utility class converting Hyper webhook DTOs (HyperVehicleByVinResponse) into the internal visit model (VisitHistoryResponse / VisitHistoryItemResponse); maps visit, service line, part and money fields. Non-instantiable, contains only static methods.
 */
public final class HyperWebhookIngestMapper {

    private HyperWebhookIngestMapper() {
    }

    /**
     * tr: serviceHistory listesindeki ilk (ve tek) ziyareti, partner bilgileriyle birlikte VisitHistoryItemResponse'a çevirir. Listenin doğrulanmış (tek elemanlı) olduğu varsayılır.
     * en: Converts the first (and only) visit in the serviceHistory list to a VisitHistoryItemResponse, enriched with partner info. The list is assumed to be pre-validated (single element).
     */
    public static VisitHistoryItemResponse toSingleVisitItem(HyperVehicleByVinResponse hyper, Partner partner) {
        List<HyperServiceHistoryItemResponse> history = hyper.getServiceHistory();
        return toVisitItem(history.get(0), partner);
    }

    /**
     * tr: Hyper webhook cevabının tamamını, VIN ve kaynak (partner source, yoksa "hyper") bilgisiyle birlikte ziyaret listesi içeren VisitHistoryResponse'a çevirir; serviceHistory null ise boş liste kullanır.
     * en: Converts the entire Hyper webhook response into a VisitHistoryResponse containing the visit list, along with the VIN and source (partner source, defaulting to "hyper"); uses an empty list when serviceHistory is null.
     */
    public static VisitHistoryResponse toIngestRequest(HyperVehicleByVinResponse hyper, Partner partner) {
        List<HyperServiceHistoryItemResponse> history = hyper.getServiceHistory() == null
                ? Collections.emptyList()
                : hyper.getServiceHistory();

        return VisitHistoryResponse.builder()
                .vin(hyper.getVin())
                .source(partner.getSource() != null ? partner.getSource() : "hyper")
                .items(history.stream().map(item -> toVisitItem(item, partner)).toList())
                .build();
    }

    private static VisitHistoryItemResponse toVisitItem(HyperServiceHistoryItemResponse item, Partner partner) {
        HyperCostResponse finalCost = item.getFinalCost() != null ? item.getFinalCost() : item.getCost();

        return VisitHistoryItemResponse.builder()
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

    private static List<VisitServiceLineResponse> mapLines(List<HyperServiceLineResponse> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        return lines.stream()
                .map(line -> VisitServiceLineResponse.builder()
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

    private static List<VisitPartResponse> mapParts(List<HyperServicePartResponse> parts) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        return parts.stream()
                .map(part -> VisitPartResponse.builder()
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
