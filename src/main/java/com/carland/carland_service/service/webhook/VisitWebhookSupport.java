package com.carland.carland_service.service.webhook;

import com.carland.carland_service.dto.response.MoneyResponse;
import com.carland.carland_service.dto.response.VisitPartResponse;
import com.carland.carland_service.dto.response.VisitServiceLineResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitPart;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * tr: Webhook ingest ve update servislerinin ortak yardımcılarıdır; daha önce
 *     PartnerServiceVisitIngestServiceImpl ve PartnerServiceVisitUpdateServiceImpl içinde birebir
 *     tekrarlanan mapping, normalize ve toplam maliyet hesaplama metodlarını tek yerde toplar.
 * en: Shared helpers of the webhook ingest and update services; consolidates the mapping, normalization
 *     and total-cost recalculation methods that were previously duplicated verbatim in
 *     PartnerServiceVisitIngestServiceImpl and PartnerServiceVisitUpdateServiceImpl.
 */
@Component
@RequiredArgsConstructor
public class VisitWebhookSupport {

    private final CarRepository carRepository;
    private final VisitRepository visitRepository;

    /**
     * tr: Webhook'tan gelen servis satırı DTO'sunu VisitServiceLine entity'sine çevirir;
     *     universalServiceId'yi normalize eder, null cost alanlarını güvenli okur.
     * en: Maps a webhook service line DTO to a VisitServiceLine entity; normalizes the
     *     universalServiceId and reads nullable cost fields safely.
     */
    public VisitServiceLine mapLineToEntity(VisitServiceLineResponse line) {
        MoneyResponse cost = line.getCost();
        return VisitServiceLine.builder()
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

    /**
     * tr: Webhook'tan gelen parça DTO'sunu VisitPart entity'sine çevirir.
     * en: Maps a webhook part DTO to a VisitPart entity.
     */
    public VisitPart mapPartToEntity(VisitPartResponse part) {
        return VisitPart.builder()
                .name(part.getName())
                .qty(part.getQty())
                .unit(part.getUnit())
                .build();
    }

    /**
     * tr: universalServiceId'yi normalize eder: boş veya "other" ise boş string, aksi halde trim'lenmiş değer.
     *     (Dikkat: VisitHistoryServiceImpl'deki normalize null döner — webhook davranışı bilinçli olarak farklıdır.)
     * en: Normalizes the universalServiceId: empty string when blank or "other", trimmed value otherwise.
     *     (Note: the normalize in VisitHistoryServiceImpl returns null — the webhook behavior differs intentionally.)
     */
    public String normalizeUniversalServiceId(String raw) {
        if (raw == null || raw.isBlank() || "other".equalsIgnoreCase(raw.trim())) {
            return "";
        }
        return raw.trim();
    }

    /**
     * tr: Aracın tüm ziyaretlerini toplayarak allTimeCost alanını yeniden hesaplar ve kaydeder;
     *     ziyaret başına finalCost öncelikli, yoksa cost, o da yoksa 0 alınır.
     * en: Recalculates and persists the car's allTimeCost by summing all visits;
     *     per visit finalCost takes precedence, falling back to cost, else zero.
     */
    public void recalculateAllTimeCost(Car car) {
        BigDecimal total = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car).stream()
                .map(this::resolveVisitFinalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        car.setAllTimeCost(total);
        carRepository.save(car);
    }

    /**
     * tr: Tek ziyaretin toplam tutarını belirler: finalCost > cost > 0 önceliğiyle.
     * en: Resolves a single visit's total amount with finalCost > cost > zero precedence.
     */
    public BigDecimal resolveVisitFinalCost(Visit visit) {
        if (visit.getFinalCostAmount() != null) {
            return visit.getFinalCostAmount();
        }
        if (visit.getCostAmount() != null) {
            return visit.getCostAmount();
        }
        return BigDecimal.ZERO;
    }

    /**
     * tr: İki BigDecimal'i ölçekten bağımsız karşılaştırır (compareTo); ikisi de null ise eşit sayılır.
     * en: Compares two BigDecimals scale-independently (compareTo); both null counts as equal.
     */
    public boolean decimalEquals(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }
}
