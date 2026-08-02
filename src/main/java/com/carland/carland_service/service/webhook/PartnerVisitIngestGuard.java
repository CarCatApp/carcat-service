package com.carland.carland_service.service.webhook;

import com.carland.carland_service.dto.response.VisitServiceLineResponse;
import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.entity.Log;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.repository.LogRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.repository.VisitServiceLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * tr: Partner webhook'undan gelen ziyaretlerin mükerrer olmadığını garanti eden koruma bileşeni; aynı recordId'li ziyaret veya payload içinde tekrarlanan serviceCode varsa isteği reddeder, araçta zaten var olan serviceCode'ları ise sadece Log tablosuna kaydeder.
 * en: Guard component ensuring visits arriving from the partner webhook are not duplicates; rejects the request when a visit with the same recordId exists or a serviceCode repeats within the payload, while serviceCodes already present on the car are only recorded in the Log table.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerVisitIngestGuard {

    private final VisitRepository visitRepository;
    private final VisitServiceLineRepository visitServiceLineRepository;
    private final LogRepository logRepository;

    /**
     * tr: Ziyaretin yeni olduğunu doğrular: aynı carId + recordId ile ziyaret zaten varsa veya payload'daki servis satırlarında serviceCode tekrar ediyorsa AlreadyExistsException fırlatır. recordId null ise kontrol yapılmaz; araçta zaten kayıtlı serviceCode tespit edilirse sadece loglanır (exception fırlatılmaz).
     * en: Asserts the visit is new: throws AlreadyExistsException if a visit with the same carId + recordId already exists or a serviceCode repeats among the payload's service lines. Skips checks when recordId is null; serviceCodes already stored for the car are only logged (no exception).
     */
    public void assertNewVisit(Long carId, VisitHistoryItemResponse item) {
        Long recordId = item.getPartnerRecordId();
        if (recordId == null) {
            return;
        }

        if (visitRepository.findWithDetailsByCarIdAndHyperRecordId(carId, recordId).isPresent()) {
            throw new AlreadyExistsException("Visit already exists for recordId=" + recordId);
        }

        List<VisitServiceLineResponse> lines = item.getServices();
        if (lines == null || lines.isEmpty()) {
            return;
        }

        Set<Integer> seenInPayload = new HashSet<>();
        for (VisitServiceLineResponse line : lines) {
            if (line.getServiceCode() == null) {
                continue;
            }
            if (!seenInPayload.add(line.getServiceCode())) {
                throw new AlreadyExistsException("Duplicate serviceCode in request: " + line.getServiceCode());
            }
            if (visitServiceLineRepository.existsByVisit_Car_CarIdAndServiceCode(carId, line.getServiceCode())) {
                log.info("dublicate service code");
                Log log = Log.builder()
                        .userId("webhook post")
                        .log("dublicate service code " + recordId)
                        .build();
                logRepository.save(log);
            }
        }
    }
}
