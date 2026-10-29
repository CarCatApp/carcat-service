package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingPackageLineRepository;
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetailedPercentageServiceTest {

    @Mock BookingPercentageSnapshot snapshot;
    @Mock BookingIndividualLineRepository individualLineRepository;
    @Mock BookingPackageLineRepository packageLineRepository;
    @Mock BookingSelectedServiceRepository selectedServiceRepository;
    @Mock IndividualServiceRepository individualServiceRepository;
    @Mock OfferedServiceRepository offeredServiceRepository;
    @Mock PercentageRepository percentageRepository;
    @Mock ServiceEntityRepository serviceEntityRepository;

    DetailedPercentageService service;
    MaintenanceTemplate template;
    Car car;
    Booking booking;

    @BeforeEach
    void setUp() {
        service = new DetailedPercentageService(snapshot, individualLineRepository, packageLineRepository,
                selectedServiceRepository, individualServiceRepository, offeredServiceRepository,
                percentageRepository, serviceEntityRepository);
        template = MaintenanceTemplate.builder().id(3L).name("benzin").build();
        car = Car.builder().carId(5L).mileage(42580L).maintenanceTemplate(template).build();
        booking = Booking.builder().id(50L).carId(5L).build();
        when(packageLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of());
        when(selectedServiceRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of());
    }

    @Test
    void copiedServiceIdResetsThePercentage() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of(
                BookingIndividualLine.builder().individualServiceId(20L).percentageServiceId(9L).build()));
        ServiceEntity oil = ServiceEntity.builder().id(9L).serviceName("Yağ").nameEn("Engine oil & filter")
                .intervalKm(10_000L).intervalMonth(12).maintenanceTemplate(template).build();
        when(serviceEntityRepository.findById(9L)).thenReturn(Optional.of(oil));
        when(percentageRepository.findByServiceIdAndCarId(9L, 5L)).thenReturn(null);
        when(percentageRepository.save(any(Percentage.class))).thenAnswer(inv -> inv.getArgument(0));

        Visit visit = visit();
        VisitServiceLine line = VisitServiceLine.builder()
                .kind("individual")
                .individualServiceId(20L)
                .doneDate(LocalDate.of(2026, 10, 20))
                .doneKm(42580)
                .nextServiceMileage(50000)
                .build();
        visit.addService(line);

        service.applyFromVisit(car, booking, visit);

        assertEquals("ENGINE_OIL", line.getUniversalServiceId());

        ArgumentCaptor<Percentage> saved = ArgumentCaptor.forClass(Percentage.class);
        verify(percentageRepository).save(saved.capture());
        Percentage percentage = saved.getValue();
        assertEquals(9L, percentage.getServiceId());
        assertEquals(42580, percentage.getLastServiceKm());
        assertEquals(LocalDate.of(2026, 10, 20), percentage.getLastServiceDate());
        assertEquals(50000, percentage.getNextServiceKm());
        assertEquals(LocalDate.of(2027, 10, 20), percentage.getNextServiceDate());
        assertEquals(PercentageStatus.EDITED_BY_PARTNER.name(), percentage.getStatus());
        assertEquals("visit:100", percentage.getPartnerRecordId());
        assertEquals(100, percentage.getKmPercentage());
        verify(snapshot, never()).resolve(any(), any());
    }

    @Test
    void blankCopyResolvesFromTheCatalogAndUsesTheInterval() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of(
                BookingIndividualLine.builder().individualServiceId(20L).build()));
        when(individualServiceRepository.findById(20L)).thenReturn(Optional.of(
                IndividualService.builder().id(20L).maintenanceCode("ENGINE_OIL").build()));
        when(snapshot.resolve(car, "ENGINE_OIL")).thenReturn(9L);
        Percentage existing = Percentage.builder().serviceId(9L).carId(5L).intervalKm(10_000L).intervalMonth(12)
                .status(PercentageStatus.CREATED.name()).build();
        when(percentageRepository.findByServiceIdAndCarId(9L, 5L)).thenReturn(existing);
        when(percentageRepository.save(any(Percentage.class))).thenAnswer(inv -> inv.getArgument(0));

        Visit visit = visit();
        visit.addService(VisitServiceLine.builder()
                .kind("individual")
                .individualServiceId(20L)
                .doneDate(LocalDate.of(2026, 10, 20))
                .doneKm(42580)
                .build());

        service.applyFromVisit(car, booking, visit);

        assertEquals(52580, existing.getNextServiceKm());
        assertEquals(PercentageStatus.EDITED_BY_PARTNER.name(), existing.getStatus());
    }

    @Test
    void olderCompletionDoesNotMoveThePercentageBackwards() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of(
                BookingIndividualLine.builder().individualServiceId(20L).percentageServiceId(9L).build()));
        Percentage existing = Percentage.builder().serviceId(9L).carId(5L)
                .lastServiceDate(LocalDate.of(2026, 11, 1)).lastServiceKm(50000)
                .status(PercentageStatus.EDITED_BY_PARTNER.name()).build();
        when(percentageRepository.findByServiceIdAndCarId(9L, 5L)).thenReturn(existing);

        Visit visit = visit();
        visit.addService(VisitServiceLine.builder()
                .kind("package")
                .individualServiceId(20L)
                .doneDate(LocalDate.of(2026, 10, 1))
                .doneKm(40000)
                .build());

        VisitServiceLine line = visit.getServices().get(0);
        service.applyFromVisit(car, booking, visit);

        verify(percentageRepository, never()).save(any());
        assertEquals(50000, existing.getLastServiceKm());
        assertEquals(null, line.getUniversalServiceId());
    }

    @Test
    void oneTimeLineDoesNotTouchAPercentage() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of());
        Visit visit = visit();
        visit.addService(VisitServiceLine.builder().kind("one_time").serviceName("Qapı").doneKm(42580).build());

        service.applyFromVisit(car, booking, visit);

        verify(percentageRepository, never()).findByServiceIdAndCarId(any(), any());
    }

    @Test
    void lineHyperIdResolvesWhenTheCatalogCodeIsBlank() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of(
                BookingIndividualLine.builder().individualServiceId(20L).build()));
        when(individualServiceRepository.findById(20L)).thenReturn(Optional.of(
                IndividualService.builder().id(20L).build()));
        when(snapshot.resolve(car, "ENGINE_OIL")).thenReturn(9L);
        Percentage existing = Percentage.builder().serviceId(9L).carId(5L).intervalKm(10_000L).intervalMonth(12)
                .serviceNameEn("Engine oil & filter").status(PercentageStatus.CREATED.name()).build();
        when(percentageRepository.findByServiceIdAndCarId(9L, 5L)).thenReturn(existing);
        when(percentageRepository.save(any(Percentage.class))).thenAnswer(inv -> inv.getArgument(0));

        Visit visit = visit();
        visit.addService(VisitServiceLine.builder()
                .kind("individual")
                .individualServiceId(20L)
                .universalServiceId("ENGINE_OIL")
                .doneDate(LocalDate.of(2026, 10, 20))
                .doneKm(42580)
                .build());

        service.applyFromVisit(car, booking, visit);

        assertEquals(52580, existing.getNextServiceKm());
        assertEquals(PercentageStatus.EDITED_BY_PARTNER.name(), existing.getStatus());
    }

    @Test
    void catalogCodeIsUsedWhenTheCopiedIdIsNotOnThisCar() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of(
                BookingIndividualLine.builder().individualServiceId(20L).percentageServiceId(99L).build()));
        when(percentageRepository.findByServiceIdAndCarId(99L, 5L)).thenReturn(null);
        when(serviceEntityRepository.findById(99L)).thenReturn(Optional.empty());
        when(individualServiceRepository.findById(20L)).thenReturn(Optional.of(
                IndividualService.builder().id(20L).maintenanceCode("ENGINE_OIL").build()));
        when(snapshot.resolve(car, "ENGINE_OIL")).thenReturn(9L);
        Percentage existing = Percentage.builder().serviceId(9L).carId(5L).intervalKm(10_000L).intervalMonth(12)
                .status(PercentageStatus.CREATED.name()).build();
        when(percentageRepository.findByServiceIdAndCarId(9L, 5L)).thenReturn(existing);
        when(percentageRepository.save(any(Percentage.class))).thenAnswer(inv -> inv.getArgument(0));

        Visit visit = visit();
        visit.addService(VisitServiceLine.builder()
                .kind("individual")
                .individualServiceId(20L)
                .doneDate(LocalDate.of(2026, 10, 20))
                .doneKm(42580)
                .build());

        service.applyFromVisit(car, booking, visit);

        assertEquals(52580, existing.getNextServiceKm());
        assertEquals(9L, existing.getServiceId());
    }

    @Test
    void missingIntervalIsTakenFromTheTemplate() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of(
                BookingIndividualLine.builder().individualServiceId(20L).percentageServiceId(9L).build()));
        ServiceEntity oil = ServiceEntity.builder().id(9L).nameEn("Engine oil & filter")
                .intervalKm(8_000L).intervalMonth(6).maintenanceTemplate(template).build();
        when(serviceEntityRepository.findById(9L)).thenReturn(Optional.of(oil));
        Percentage existing = Percentage.builder().serviceId(9L).carId(5L)
                .status(PercentageStatus.CREATED.name()).build();
        when(percentageRepository.findByServiceIdAndCarId(9L, 5L)).thenReturn(existing);
        when(percentageRepository.save(any(Percentage.class))).thenAnswer(inv -> inv.getArgument(0));

        Visit visit = visit();
        VisitServiceLine line = VisitServiceLine.builder()
                .kind("individual")
                .individualServiceId(20L)
                .doneDate(LocalDate.of(2026, 10, 20))
                .doneKm(42580)
                .build();
        visit.addService(line);

        service.applyFromVisit(car, booking, visit);

        assertEquals(8_000L, existing.getIntervalKm());
        assertEquals(6, existing.getIntervalMonth());
        assertEquals(50580, existing.getNextServiceKm());
        assertEquals(LocalDate.of(2027, 4, 20), existing.getNextServiceDate());
        assertEquals("ENGINE_OIL", line.getUniversalServiceId());
    }

    @Test
    void otherItemsKmLifeFollowsTheCarMileage() {
        when(individualLineRepository.findByBooking_IdOrderByIdAsc(50L)).thenReturn(List.of(
                BookingIndividualLine.builder().individualServiceId(20L).percentageServiceId(9L).build()));
        ServiceEntity oil = ServiceEntity.builder().id(9L).nameEn("Engine oil & filter")
                .intervalKm(10_000L).intervalMonth(12).maintenanceTemplate(template).build();
        when(serviceEntityRepository.findById(9L)).thenReturn(Optional.of(oil));
        Percentage oilRow = Percentage.builder().serviceId(9L).carId(5L).intervalKm(10_000L).intervalMonth(12)
                .status(PercentageStatus.CREATED.name()).build();
        Percentage brakes = Percentage.builder().serviceId(11L).carId(5L)
                .lastServiceKm(40_000).nextServiceKm(50_000).kmPercentage(10).remainingKm(9_999).build();
        when(percentageRepository.findByServiceIdAndCarId(9L, 5L)).thenReturn(oilRow);
        when(percentageRepository.findAllByCarId(5L)).thenReturn(List.of(oilRow, brakes));
        when(percentageRepository.save(any(Percentage.class))).thenAnswer(inv -> inv.getArgument(0));

        Visit visit = visit();
        visit.addService(VisitServiceLine.builder()
                .kind("individual")
                .individualServiceId(20L)
                .doneDate(LocalDate.of(2026, 10, 20))
                .doneKm(42580)
                .build());

        service.applyFromVisit(car, booking, visit);

        assertEquals(74, brakes.getKmPercentage());
        assertEquals(7420, brakes.getRemainingKm());
    }

    private Visit visit() {
        return Visit.builder().id(100L).car(car).lastServiceDate(LocalDate.of(2026, 10, 20)).lastServiceMileage(42580).build();
    }
}
