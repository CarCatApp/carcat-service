package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingPackageLine;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.repository.BookingPackageLineRepository;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPercentageSnapshotTest {

    @Mock ServiceEntityRepository serviceEntityRepository;
    @Mock BranchCarePackageItemRepository carePackageItemRepository;
    @Mock BookingPackageLineRepository packageLineRepository;

    BookingPercentageSnapshot snapshot;
    MaintenanceTemplate template;
    Car car;

    @BeforeEach
    void setUp() {
        snapshot = new BookingPercentageSnapshot(serviceEntityRepository, carePackageItemRepository, packageLineRepository);
        template = MaintenanceTemplate.builder().id(3L).name("benzin").build();
        car = Car.builder().carId(5L).maintenanceTemplate(template).build();
    }

    @Test
    void resolveMapsEngineOilToTheTemplateService() {
        when(serviceEntityRepository.findAllByMaintenanceTemplate(template)).thenReturn(List.of(
                ServiceEntity.builder().id(9L).nameEn("Engine oil & filter").build(),
                ServiceEntity.builder().id(10L).nameEn("Brake pads").build()));

        assertEquals(9L, snapshot.resolve(car, "ENGINE_OIL"));
        assertEquals(10L, snapshot.resolve(car, "brake_pads"));
        assertNull(snapshot.resolve(car, "NOT_A_CODE"));
        assertNull(snapshot.resolve(car, " "));
        assertNull(snapshot.resolve(null, "ENGINE_OIL"));
    }

    @Test
    void snapshotPackageKeepsOnlyPeriodicLines() {
        when(serviceEntityRepository.findAllByMaintenanceTemplate(template)).thenReturn(List.of(
                ServiceEntity.builder().id(9L).nameEn("Engine oil & filter").build()));
        OfferedService oil = OfferedService.builder().id(1L).periodic(true).active(true)
                .maintenanceCode("ENGINE_OIL").titleJson("{\"az\":\"Yağ\"}").build();
        OfferedService scan = OfferedService.builder().id(2L).periodic(false).active(true).build();
        when(carePackageItemRepository.findByCarePackage_IdOrderByIdAsc(8L)).thenReturn(List.of(
                BranchCarePackageItem.builder().enabled(true).offeredService(oil).build(),
                BranchCarePackageItem.builder().enabled(true).offeredService(scan).build()));
        Booking booking = Booking.builder().id(4L).carePackageId(8L).build();

        snapshot.snapshotPackage(booking, car);

        ArgumentCaptor<BookingPackageLine> saved = ArgumentCaptor.forClass(BookingPackageLine.class);
        verify(packageLineRepository).save(saved.capture());
        assertEquals(1L, saved.getValue().getOfferedServiceId());
        assertEquals(9L, saved.getValue().getPercentageServiceId());
        assertEquals("{\"az\":\"Yağ\"}", saved.getValue().getTitleJson());
    }

    @Test
    void snapshotPackageSkipsABookingWithoutAPackage() {
        snapshot.snapshotPackage(Booking.builder().id(4L).build(), car);
        verify(carePackageItemRepository, never()).findByCarePackage_IdOrderByIdAsc(org.mockito.ArgumentMatchers.any());
    }
}
