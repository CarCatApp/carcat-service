package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.repository.ServiceHistoryRepository;
import com.carland.carland_service.service.HyperPercentageSyncService.PartnerLineSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PercentageLifeServiceTest {

    @Mock ServiceEntityRepository serviceEntityRepository;
    @Mock PercentageRepository percentageRepository;
    @Mock CustomerServiceRecordRepository customerServiceRecordRepository;
    @Mock ServiceHistoryRepository serviceHistoryRepository;
    @Mock HyperPercentageSyncService hyperPercentageSyncService;

    PercentageLifeService service;
    MaintenanceTemplate template;
    Car car;
    ServiceEntity oil;

    @BeforeEach
    void setUp() {
        service = new PercentageLifeService(
                serviceEntityRepository,
                percentageRepository,
                customerServiceRecordRepository,
                serviceHistoryRepository,
                hyperPercentageSyncService);
        template = MaintenanceTemplate.builder().id(4L).build();
        car = Car.builder().carId(1091L).mileage(15000L).maintenanceTemplate(template)
                .createdAt(LocalDateTime.of(2020, 1, 1, 0, 0)).build();
        oil = ServiceEntity.builder().id(152L).serviceName("Oil").actionType("Change")
                .nameEn("Engine oil").intervalKm(10000L).intervalMonth(12).maintenanceTemplate(template).build();
    }

    @Test
    void editedRowUsesStoredNextKmAndDate() {
        LocalDate next = LocalDate.now().plusDays(40);
        when(serviceEntityRepository.findAllByMaintenanceTemplate(template)).thenReturn(List.of(oil));
        when(customerServiceRecordRepository.findAllByCar(car)).thenReturn(List.of());
        when(serviceHistoryRepository.findAllByCar(car)).thenReturn(List.of());
        when(percentageRepository.findByServiceIdAndCarId(152L, 1091L)).thenReturn(Percentage.builder()
                .serviceId(152L).carId(1091L).status(PercentageStatus.EDITED_BY_CUSTOMER.name())
                .lastServiceKm(10000).nextServiceKm(20000)
                .lastServiceDate(LocalDate.now().minusDays(10)).nextServiceDate(next)
                .build());

        PercentageLifeService.Snapshot life = service.byServiceId(car).get(152L);

        assertEquals(5000, life.remainingKm());
        assertEquals(40, life.remainingDays());
        assertEquals(50, life.kmPercentage());
        assertEquals(80, life.monthPercentage());
        assertEquals(50, life.shownPercentage());
    }

    @Test
    void createdRowUsesThePartnerVisitInsteadOfTheStoredRow() {
        when(serviceEntityRepository.findAllByMaintenanceTemplate(template)).thenReturn(List.of(oil));
        when(customerServiceRecordRepository.findAllByCar(car)).thenReturn(List.of());
        when(serviceHistoryRepository.findAllByCar(car)).thenReturn(List.of());
        when(percentageRepository.findByServiceIdAndCarId(152L, 1091L)).thenReturn(Percentage.builder()
                .serviceId(152L).carId(1091L).status(PercentageStatus.CREATED.name())
                .lastServiceKm(1).nextServiceKm(2)
                .lastServiceDate(LocalDate.now().minusDays(1)).nextServiceDate(LocalDate.now().plusDays(1))
                .build());
        when(hyperPercentageSyncService.findBestPartnerLineForService(car, "Engine oil", 10000L, 12))
                .thenReturn(Optional.of(new PartnerLineSnapshot(
                        LocalDate.now().minusDays(5), 12000,
                        LocalDate.now().plusDays(15), 22000)));

        PercentageLifeService.Snapshot life = service.byServiceId(car).get(152L);

        assertEquals(7000, life.remainingKm());
        assertEquals(15, life.remainingDays());
        assertEquals(70, life.kmPercentage());
        assertEquals(75, life.monthPercentage());
    }

    @Test
    void createdRowWithoutAPartnerUsesLastServicePlusTheTemplateInterval() {
        when(serviceEntityRepository.findAllByMaintenanceTemplate(template)).thenReturn(List.of(oil));
        when(customerServiceRecordRepository.findAllByCar(car)).thenReturn(List.of(
                CustomerServiceRecord.builder().serviceId(152L).doneKm(10000)
                        .doneDate(LocalDate.now().minusMonths(1)).build()
        ));
        when(serviceHistoryRepository.findAllByCar(car)).thenReturn(List.of());
        when(percentageRepository.findByServiceIdAndCarId(152L, 1091L)).thenReturn(Percentage.builder()
                .serviceId(152L).carId(1091L).status(PercentageStatus.CREATED.name())
                .lastServiceKm(1).nextServiceKm(2).build());
        when(hyperPercentageSyncService.findBestPartnerLineForService(car, "Engine oil", 10000L, 12))
                .thenReturn(Optional.empty());

        PercentageLifeService.Snapshot life = service.byServiceId(car).get(152L);

        assertEquals(5000, life.remainingKm());
        LocalDate last = LocalDate.now().minusMonths(1);
        LocalDate next = last.plusMonths(12);
        assertEquals((int) (next.toEpochDay() - LocalDate.now().toEpochDay()), life.remainingDays());
        assertEquals(50, life.kmPercentage());
        long totalDays = next.toEpochDay() - last.toEpochDay();
        long remainingDays = next.toEpochDay() - LocalDate.now().toEpochDay();
        assertEquals((int) Math.round((remainingDays * 100.0) / totalDays), life.monthPercentage());
    }

    @Test
    void kmPercentageMatchesThePercentageScreen() {
        assertEquals(62, PercentageLifeService.kmPercentage(0, 50000, 19020L));
        assertEquals(0, PercentageLifeService.kmPercentage(10, 10, 10L));
        assertNull(PercentageLifeService.kmPercentage(null, 10, 10L));
    }

    @Test
    void missingMileageLeavesRemainingKmNull() {
        car.setMileage(null);
        when(serviceEntityRepository.findAllByMaintenanceTemplate(template)).thenReturn(List.of(oil));
        when(customerServiceRecordRepository.findAllByCar(car)).thenReturn(List.of());
        when(serviceHistoryRepository.findAllByCar(car)).thenReturn(List.of());
        when(percentageRepository.findByServiceIdAndCarId(152L, 1091L)).thenReturn(null);
        when(hyperPercentageSyncService.findBestPartnerLineForService(car, "Engine oil", 10000L, 12))
                .thenReturn(Optional.empty());

        PercentageLifeService.Snapshot life = service.byServiceId(car).get(152L);

        assertNull(life.remainingKm());
    }
}
