package com.carland.carland_service.service.technicalprofile;

import com.carland.carland_service.dto.response.CarResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.enums.CarTransmissionVariant;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.enums.TimingBeltChoice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KiaPercentageProfileServiceTest {

    @Mock
    private PercentageRepository percentageRepository;
    @Mock
    private ServiceEntityRepository serviceEntityRepository;
    @Mock
    private CustomerServiceRecordRepository customerServiceRecordRepository;

    @Test
    void applyTransmissionVariantReturnsTheActivatedRecordId() {
        Car car = Car.builder().carId(15L).brand("Kia").build();
        Percentage gateway = percentage(1L, 100L, MaintenanceItemNames.TRANSMISSION_FLUID, PercentageStatus.CREATED);
        Percentage at6 = percentage(2L, 256L, MaintenanceItemNames.TRANSMISSION_AT6_8, PercentageStatus.DEACTIVATED);
        Percentage at4 = percentage(3L, 255L, MaintenanceItemNames.TRANSMISSION_AT4, PercentageStatus.DEACTIVATED);
        CustomerServiceRecord at6Record = CustomerServiceRecord.builder().id(77L).serviceId(256L).car(car).build();

        when(percentageRepository.findAllByCarId(15L)).thenReturn(List.of(gateway, at6, at4));
        when(customerServiceRecordRepository.findByServiceIdAndCar(256L, car)).thenReturn(at6Record);

        Long recordId = service().applyTransmissionVariant(car, CarTransmissionVariant.AT6_8);

        assertEquals(77L, recordId);
        assertEquals(PercentageStatus.DEACTIVATED.name(), gateway.getStatus());
        assertEquals(PercentageStatus.CREATED.name(), at6.getStatus());
        assertEquals(PercentageStatus.DEACTIVATED.name(), at4.getStatus());
    }

    @Test
    void beltChoiceReturnsTheTimingBeltRecordId() {
        Car car = Car.builder().carId(15L).brand("Kia").build();
        Percentage belt = percentage(4L, 152L, MaintenanceItemNames.TIMING_BELT, PercentageStatus.DEACTIVATED);
        CustomerServiceRecord beltRecord = CustomerServiceRecord.builder().id(88L).serviceId(152L).car(car).build();
        when(percentageRepository.findAllByCarId(15L)).thenReturn(List.of(belt));
        when(customerServiceRecordRepository.findByServiceIdAndCar(152L, car)).thenReturn(beltRecord);

        Long recordId = service().applyTimingBeltChoice(car, TimingBeltChoice.BELT);

        assertEquals(88L, recordId);
        assertEquals(PercentageStatus.CREATED.name(), belt.getStatus());
    }

    @Test
    void chainChoiceReturnsNullAndDeactivatesTheBelt() {
        Car car = Car.builder().carId(15L).brand("Kia").build();
        Percentage belt = percentage(4L, 152L, MaintenanceItemNames.TIMING_BELT, PercentageStatus.CREATED);
        when(percentageRepository.findAllByCarId(15L)).thenReturn(List.of(belt));

        Long recordId = service().applyTimingBeltChoice(car, TimingBeltChoice.CHAIN);

        assertNull(recordId);
        assertEquals(PercentageStatus.DEACTIVATED.name(), belt.getStatus());
    }

    @Test
    void chainResponseWritesRecordIdNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CarResponse plain = CarResponse.builder().carId(15L).build();
        assertFalse(mapper.valueToTree(plain).has("recordId"));

        CarResponse chain = CarResponse.builder().carId(15L).build();
        chain.includeRecordId(null);
        JsonNode node = mapper.valueToTree(chain);
        assertTrue(node.has("recordId"));
        assertTrue(node.get("recordId").isNull());

        CarResponse belt = CarResponse.builder().carId(15L).build();
        belt.includeRecordId(88L);
        assertEquals(88L, mapper.valueToTree(belt).get("recordId").asLong());
    }

    private KiaPercentageProfileService service() {
        return new KiaPercentageProfileService(
                percentageRepository, serviceEntityRepository, customerServiceRecordRepository);
    }

    private static Percentage percentage(Long id, Long serviceId, String nameEn, PercentageStatus status) {
        return Percentage.builder()
                .id(id)
                .serviceId(serviceId)
                .serviceNameEn(nameEn)
                .carId(15L)
                .status(status.name())
                .build();
    }
}
