package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HyperPercentageSyncServiceTest {

    @Mock PercentageRepository percentageRepository;
    @Mock VisitRepository visitRepository;
    @Mock ServiceEntityRepository serviceEntityRepository;

    HyperPercentageSyncService service;
    Car car;

    @BeforeEach
    void setUp() {
        service = new HyperPercentageSyncService(percentageRepository, visitRepository, serviceEntityRepository);
        car = Car.builder().carId(5L).mileage(42_580L).build();
    }

    @Test
    void forcedReloadDoesNotRollANewerPanelWriteBackToAnOlderVisit() {
        Percentage percentage = panelPercentage(LocalDate.of(2026, 10, 20), 42_580);
        when(percentageRepository.findAllByCarId(5L)).thenReturn(List.of(percentage));
        Visit older = hyperVisit(1L, 50L, LocalDate.of(2026, 1, 1), 10_000);
        when(visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car)).thenReturn(List.of(older));

        service.resyncFromVisits(car, List.of(older));

        verify(percentageRepository, never()).save(any());
        assertEquals(LocalDate.of(2026, 10, 20), percentage.getLastServiceDate());
        assertEquals(42_580, percentage.getLastServiceKm());
    }

    @Test
    void forcedReloadStillAppliesANewerVisit() {
        Percentage percentage = panelPercentage(LocalDate.of(2026, 1, 1), 10_000);
        when(percentageRepository.findAllByCarId(5L)).thenReturn(List.of(percentage));
        Visit newer = hyperVisit(2L, 80L, LocalDate.of(2026, 10, 20), 42_580);
        when(visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car)).thenReturn(List.of(newer));

        service.resyncFromVisits(car, List.of(newer));

        verify(percentageRepository).save(percentage);
        assertEquals(LocalDate.of(2026, 10, 20), percentage.getLastServiceDate());
        assertEquals(42_580, percentage.getLastServiceKm());
    }

    @Test
    void forcedReloadStillRefreshesTheSameHyperRecord() {
        Percentage percentage = panelPercentage(LocalDate.of(2026, 10, 20), 42_580);
        percentage.setPartnerRecordId("50");
        percentage.setNextServiceKm(null);
        when(percentageRepository.findAllByCarId(5L)).thenReturn(List.of(percentage));
        Visit same = hyperVisit(1L, 50L, LocalDate.of(2026, 10, 20), 42_580);
        same.getServices().get(0).setNextServiceMileage(60_000);
        when(visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car)).thenReturn(List.of(same));

        service.resyncFromVisits(car, List.of(same));

        verify(percentageRepository).save(percentage);
        assertEquals(60_000, percentage.getNextServiceKm());
    }

    @Test
    void plainSyncDoesNotRewriteTheSameRecord() {
        Percentage percentage = panelPercentage(LocalDate.of(2026, 10, 20), 42_580);
        percentage.setPartnerRecordId("50");
        when(percentageRepository.findAllByCarId(5L)).thenReturn(List.of(percentage));
        Visit same = hyperVisit(1L, 50L, LocalDate.of(2026, 10, 20), 42_580);

        service.syncFromVisits(car, List.of(same));

        verify(percentageRepository, never()).save(any());
    }

    private Percentage panelPercentage(LocalDate lastDate, int lastKm) {
        return Percentage.builder()
                .id(3L)
                .carId(5L)
                .serviceNameEn("Engine oil & filter")
                .intervalKm(10_000L)
                .intervalMonth(12)
                .lastServiceDate(lastDate)
                .lastServiceKm(lastKm)
                .status(PercentageStatus.EDITED_BY_PARTNER.name())
                .partnerRecordId("visit:100")
                .build();
    }

    private Visit hyperVisit(Long id, Long recordId, LocalDate date, int km) {
        Visit visit = Visit.builder()
                .id(id)
                .hyperRecordId(recordId)
                .car(car)
                .lastServiceDate(date)
                .lastServiceMileage(km)
                .build();
        visit.addService(VisitServiceLine.builder()
                .universalServiceId("ENGINE_OIL")
                .build());
        return visit;
    }
}
