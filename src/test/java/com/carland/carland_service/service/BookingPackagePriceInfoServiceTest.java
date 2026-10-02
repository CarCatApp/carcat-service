package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingPackagePriceInfoResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPackagePriceInfoServiceTest {

    @Mock BranchCarePackageRepository carePackageRepository;

    BookingPackagePriceInfoService service;

    @BeforeEach
    void setUp() {
        service = new BookingPackagePriceInfoService(carePackageRepository);
    }

    @Test
    void returnsThreeLanguagesWithPackagePrice() {
        Partner partner = Partner.builder().id(1L).active(true).build();
        Branch branch = Branch.builder().id(12L).active(true).partner(partner).build();
        when(carePackageRepository.findById(5L)).thenReturn(Optional.of(BranchCarePackage.builder()
                .id(5L).branch(branch).name("Hyper extra").price(129).currency("AZN").active(true).build()));

        BookingPackagePriceInfoResponse en = service.info(5L, "en");
        assertEquals(129, en.getPrice());
        assertEquals("About the package price", en.getTitle());
        assertEquals("129 ₼ is the service fee only.", en.getSubtitle());
        assertEquals(
                "Parts, fluids and other required materials are not included and may be charged separately",
                en.getDescription());

        BookingPackagePriceInfoResponse az = service.info(5L, "az");
        assertEquals("Paket qiyməti haqqında", az.getTitle());
        assertEquals("129 ₼ yalnız xidmət haqqıdır.", az.getSubtitle());

        BookingPackagePriceInfoResponse ru = service.info(5L, "ru");
        assertEquals("О цене пакета", ru.getTitle());
        assertEquals("129 ₼ — это только стоимость услуги.", ru.getSubtitle());
    }

    @Test
    void missingPackageIsNotFound() {
        when(carePackageRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.info(9L, "az"));
    }
}
