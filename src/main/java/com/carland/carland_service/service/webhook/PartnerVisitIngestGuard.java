package com.carland.carland_service.service.webhook;

import com.carland.carland_service.dto.response.v2.ServiceHistoryLineV2Response;
import com.carland.carland_service.dto.response.v2.ServiceHistoryVisitV2Response;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerVisitIngestGuard {

    private final VisitRepository visitRepository;
    private final VisitServiceLineRepository visitServiceLineRepository;
    private final LogRepository logRepository;

    public void assertNewVisit(Long carId, ServiceHistoryVisitV2Response item) {
        Long recordId = item.getPartnerRecordId();
        if (recordId == null) {
            return;
        }

        if (visitRepository.findWithDetailsByCarIdAndHyperRecordId(carId, recordId).isPresent()) {
            throw new AlreadyExistsException("Visit already exists for recordId=" + recordId);
        }

        List<ServiceHistoryLineV2Response> lines = item.getServices();
        if (lines == null || lines.isEmpty()) {
            return;
        }

        Set<Integer> seenInPayload = new HashSet<>();
        for (ServiceHistoryLineV2Response line : lines) {
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
