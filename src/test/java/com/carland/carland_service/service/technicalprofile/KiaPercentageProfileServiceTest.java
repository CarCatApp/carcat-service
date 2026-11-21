package com.carland.carland_service.service.technicalprofile;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.enums.CarTransmissionVariant;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        KiaPercentageProfileService service = new KiaPercentageProfileService(
                percentageRepository, serviceEntityRepository, customerServiceRecordRepository);

        Long recordId = service.applyTransmissionVariant(car, CarTransmissionVariant.AT6_8);

        assertEquals(77L, recordId);
        assertEquals(PercentageStatus.DEACTIVATED.name(), gateway.getStatus());
        assertEquals(PercentageStatus.CREATED.name(), at6.getStatus());
        assertEquals(PercentageStatus.DEACTIVATED.name(), at4.getStatus());
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
