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
        assertEquals("Price terms", en.getTitle());
        assertEquals("The price includes the service fee only.", en.getSubtitle());
        assertEquals(
                "Spare parts and consumables (oil, filters, etc.) are not included in the total and will be calculated separately.",
                en.getDescription());

        BookingPackagePriceInfoResponse az = service.info(5L, "az");
        assertEquals("Qiymət şərtləri", az.getTitle());
        assertEquals("Qiymətə yalnız xidmət haqqı daxildir.", az.getSubtitle());
        assertEquals(
                "Ehtiyat hissələri və sərfiyyat materialları (yağ, filtr və s.) ümumi məbləğə daxil deyil, onlar ayrıca hesablanacaq.",
                az.getDescription());

        BookingPackagePriceInfoResponse ru = service.info(5L, "ru");
        assertEquals("Условия цены", ru.getTitle());
        assertEquals("В цену включена только стоимость услуги.", ru.getSubtitle());
        assertEquals(
                "Запчасти и расходные материалы (масло, фильтры и т. д.) не входят в общую сумму и будут рассчитаны отдельно.",
                ru.getDescription());
    }

    @Test
    void missingPackageIsNotFound() {
        when(carePackageRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.info(9L, "az"));
    }
}
