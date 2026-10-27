package com.carland.carland_service.idda;

import com.carland.carland_service.dto.request.CarRequest;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.EngineTypeRepository;
import com.carland.carland_service.repository.MaintenanceTemplateRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.service.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IddaCarWriteServiceTest {

    @Mock IddaPlateCheckRepository iddaPlateCheckRepository;
    @Mock CarRepository carRepository;
    @Mock CustomerRepository customerRepository;
    @Mock ColorRepository colorRepository;
    @Mock EngineTypeRepository engineTypeRepository;
    @Mock MaintenanceTemplateRepository maintenanceTemplateRepository;
    @Mock PercentageRepository percentageRepository;
    @Mock CustomerServiceRecordRepository customerServiceRecordRepository;
    @Mock RedisCacheService redisCacheService;

    IddaCarWriteService service;

    @BeforeEach
    void setUp() {
        service = new IddaCarWriteService(
                iddaPlateCheckRepository,
                carRepository,
                customerRepository,
                colorRepository,
                engineTypeRepository,
                maintenanceTemplateRepository,
                percentageRepository,
                customerServiceRecordRepository,
                redisCacheService);
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(9L, "050", "ACTIVE"))
                .thenReturn(Customer.builder().userId(9L).phoneNumber("050").cars(new ArrayList<>()).build());
        when(carRepository.save(any())).thenAnswer(invocation -> {
            Car car = invocation.getArgument(0);
            if (car.getCarId() == null) {
                car.setCarId(50L);
            }
            return car;
        });
    }

    @Test
    void matchingVinKeepsVinUpdatesPlateAndOrphansThePlateRow() {
        Car vinCar = Car.builder()
                .carId(1L)
                .vin("abc123wrongcase")
                .plateNumber("10-AA-100")
                .engineTypeId(5L)
                .engineType("Petrol (Gasoline)")
                .customer(Customer.builder().userId(3L).build())
                .build();
        Car plateCar = Car.builder()
                .carId(2L)
                .vin("OLDVIN")
                .plateNumber("77-BU-669")
                .customer(Customer.builder().userId(4L).build())
                .build();
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(any(), any()))
                .thenReturn(List.of(snapshot()));
        when(carRepository.findAllByVinIgnoreCase("19XZE4F54NE012640")).thenReturn(List.of(vinCar));
        when(carRepository.findByPlateKey("77BU669")).thenReturn(List.of(plateCar));

        IddaAddResult result = service.add(request(8L), "050", "9", "az");

        assertFalse(result.created());
        assertEquals("abc123wrongcase", vinCar.getVin());
        assertEquals("77BU669", vinCar.getPlateNumber());
        assertEquals(5L, vinCar.getEngineTypeId());
        assertEquals(9L, vinCar.getCustomer().getUserId());
        assertTrue(vinCar.getFromIdda());
        assertNull(plateCar.getCustomer());
        verify(redisCacheService).evictCarListAfterCommit("4");
        verify(redisCacheService).evictCarListAfterCommit("3");
        verify(redisCacheService).evictCarListAfterCommit("9");
    }

    @Test
    void missingVinInsertsANewRowAndOrphansTheSinglePlateRow() {
        Car plateCar = Car.builder()
                .carId(2L)
                .vin("OLDVIN")
                .plateNumber("77BU669")
                .customer(Customer.builder().userId(4L).build())
                .build();
        EngineType engine = EngineType.builder().engineTypeId(6L).engineType("Hybrid").build();
        ServiceEntity serviceEntity = ServiceEntity.builder().id(1L).serviceName("Oil").important(false).build();
        MaintenanceTemplate template = MaintenanceTemplate.builder()
                .id(1L)
                .services(List.of(serviceEntity))
                .build();
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(any(), any()))
                .thenReturn(List.of(snapshot()));
        when(carRepository.findAllByVinIgnoreCase("19XZE4F54NE012640")).thenReturn(List.of());
        when(carRepository.findByPlateKey("77BU669")).thenReturn(List.of(plateCar));
        when(engineTypeRepository.findByEngineTypeId(6L)).thenReturn(engine);
        when(maintenanceTemplateRepository.findByEngineType(engine)).thenReturn(Optional.of(template));

        IddaAddResult result = service.add(request(6L), "050", "9", "az");

        assertTrue(result.created());
        assertEquals("19XZE4F54NE012640", result.car().getVin());
        assertEquals("OLDVIN", plateCar.getVin());
        assertNull(plateCar.getCustomer());
        assertEquals(6L, result.car().getEngineTypeId());
        verify(redisCacheService).evictCarListAfterCommit("4");
    }

    @Test
    void iddaColorReplacesCatalogColor() {
        Car vinCar = Car.builder()
                .carId(1L)
                .vin("19XZE4F54NE012640")
                .plateNumber("10-AA-100")
                .colorId(9L)
                .customer(Customer.builder().userId(3L).build())
                .build();
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(any(), any()))
                .thenReturn(List.of(snapshot()));
        when(carRepository.findAllByVinIgnoreCase("19XZE4F54NE012640")).thenReturn(List.of(vinCar));
        when(carRepository.findByPlateKey("77BU669")).thenReturn(List.of());
        CarRequest body = request(8L);
        body.setIddaColor("  tünd göy  ");

        service.add(body, "050", "9", "az");

        assertNull(vinCar.getColorId());
        assertEquals("tünd göy", vinCar.getIddaColor());
        assertTrue(vinCar.getIddaProvidedFields().contains("iddaColor"));
    }

    @Test
    void snapshotColorIsStoredWhenRequestOmitsIt() {
        when(carRepository.findAllByVinIgnoreCase("19XZE4F54NE012640")).thenReturn(List.of());
        when(carRepository.findByPlateKey("77BU669")).thenReturn(List.of());
        EngineType engine = EngineType.builder().engineTypeId(6L).engineType("Hybrid").build();
        ServiceEntity serviceEntity = ServiceEntity.builder().id(1L).serviceName("Oil").important(false).build();
        MaintenanceTemplate template = MaintenanceTemplate.builder().id(1L).services(List.of(serviceEntity)).build();
        when(engineTypeRepository.findByEngineTypeId(6L)).thenReturn(engine);
        when(maintenanceTemplateRepository.findByEngineType(engine)).thenReturn(java.util.Optional.of(template));
        IddaPlateCheck row = snapshot();
        row.setColorRaw("tünd göy");
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(any(), any()))
                .thenReturn(List.of(row));

        IddaAddResult result = service.add(request(6L), "050", "9", "az");

        assertEquals("tünd göy", result.car().getIddaColor());
        assertTrue(result.car().getIddaProvidedFields().contains("iddaColor"));
    }

    @Test
    void colorIdClearsIddaColor() {
        Car vinCar = Car.builder()
                .carId(1L)
                .vin("19XZE4F54NE012640")
                .plateNumber("10-AA-100")
                .iddaColor("göy")
                .customer(Customer.builder().userId(3L).build())
                .build();
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(any(), any()))
                .thenReturn(List.of(snapshot()));
        when(carRepository.findAllByVinIgnoreCase("19XZE4F54NE012640")).thenReturn(List.of(vinCar));
        when(carRepository.findByPlateKey("77BU669")).thenReturn(List.of());
        when(colorRepository.findByColorId(4L)).thenReturn(Color.builder().colorId(4L).build());
        CarRequest body = request(8L);
        body.setColorId(4L);
        body.setIddaColor("göy");

        service.add(body, "050", "9", "az");

        assertEquals(4L, vinCar.getColorId());
        assertNull(vinCar.getIddaColor());
        assertFalse(vinCar.getIddaProvidedFields().contains("iddaColor"));
    }

    @Test
    void registryColorReplacesTheCatalogColorSentByTheApp() {
        when(carRepository.findAllByVinIgnoreCase("19XZE4F54NE012640")).thenReturn(List.of());
        when(carRepository.findByPlateKey("77BU669")).thenReturn(List.of());
        EngineType engine = EngineType.builder().engineTypeId(6L).engineType("Hybrid").build();
        ServiceEntity serviceEntity = ServiceEntity.builder().id(1L).serviceName("Oil").important(false).build();
        MaintenanceTemplate template = MaintenanceTemplate.builder().id(1L).services(List.of(serviceEntity)).build();
        when(engineTypeRepository.findByEngineTypeId(6L)).thenReturn(engine);
        when(maintenanceTemplateRepository.findByEngineType(engine)).thenReturn(Optional.of(template));
        when(colorRepository.findByColorId(16L)).thenReturn(Color.builder().colorId(16L).build());
        IddaPlateCheck row = snapshot();
        row.setColorRaw("Gümüşü metallik");
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(any(), any()))
                .thenReturn(List.of(row));
        CarRequest body = request(6L);
        body.setColorId(16L);

        IddaAddResult result = service.add(body, "050", "9", "az");

        assertNull(result.car().getColorId());
        assertEquals("Gümüşü metallik", result.car().getIddaColor());
        assertTrue(result.car().getIddaProvidedFields().contains("iddaColor"));
    }

    private static CarRequest request(Long engineTypeId) {
        return CarRequest.builder()
                .resource("idda")
                .plateNumber("77-BU-669")
                .mileage(0L)
                .engineTypeId(engineTypeId)
                .bodyType("Sedan")
                .build();
    }

    private static IddaPlateCheck snapshot() {
        return IddaPlateCheck.builder()
                .plateKey("77BU669")
                .plateNumber("77BU669")
                .vin("19XZE4F54NE012640")
                .brand("Honda")
                .model("Insight")
                .modelYear(2022)
                .engineCapacity(1496)
                .countsTowardLimit(true)
                .build();
    }
}
