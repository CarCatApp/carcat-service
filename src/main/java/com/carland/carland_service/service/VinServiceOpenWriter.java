package com.carland.carland_service.service;

import com.carland.carland_service.entity.VinServiceOpen;
import com.carland.carland_service.repository.VinServiceOpenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * tr: Açılış bayrağını kendi transaction'ında yazar. Liste okuması read-only kalsa da satır kalır.
 * en: Writes the open flag in its own transaction so a read-only list call still keeps the row.
 */
@Service
@RequiredArgsConstructor
public class VinServiceOpenWriter {

    private final VinServiceOpenRepository vinServiceOpenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfAbsent(String vin, String reason, Long carId) {
        if (vin == null || vin.isBlank() || vinServiceOpenRepository.existsByVin(vin)) {
            return;
        }
        vinServiceOpenRepository.saveAndFlush(VinServiceOpen.builder()
                .vin(vin)
                .reason(reason)
                .openedAt(OffsetDateTime.now(StaffSlotWindows.ZONE))
                .carId(carId)
                .build());
    }
}
