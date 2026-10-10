package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminBrandModelHeadingSaveRequest;
import com.carland.carland_service.dto.response.AdminBrandModelHeadingRow;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandModelHeadingServiceTest {

    @Mock BrandModelServiceRepository brandModelServiceRepository;
    @Mock BrandModelRepository brandModelRepository;

    BrandModelHeadingService service;

    @BeforeEach
    void setUp() {
        service = new BrandModelHeadingService(
                brandModelServiceRepository,
                brandModelRepository,
                new ServiceCategoryJson(new ObjectMapper()));
    }

    @Test
    void saveRequiresThreeLanguages() {
        assertThrows(MissingFieldException.class, () -> service.save(AdminBrandModelHeadingSaveRequest.builder()
                .titleAz("Yağlar")
                .titleEn("Oils")
                .build()));
        verify(brandModelServiceRepository, never()).save(any());
    }

    @Test
    void saveWritesOilAndTitles() {
        when(brandModelServiceRepository.save(any())).thenAnswer(call -> {
            BrandModelService saved = call.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        AdminBrandModelHeadingRow row = service.save(AdminBrandModelHeadingSaveRequest.builder()
                .titleAz("Yağlar")
                .titleEn("Oils")
                .titleRu("Масла")
                .oil(true)
                .sortOrder(1)
                .build());

        ArgumentCaptor<BrandModelService> captor = ArgumentCaptor.forClass(BrandModelService.class);
        verify(brandModelServiceRepository).save(captor.capture());
        assertTrue(captor.getValue().getTitleJson().contains("\"en\":\"Oils\""));
        assertEquals(Boolean.TRUE, captor.getValue().getOil());
        assertEquals("Yağlar", row.getTitleAz());
        assertEquals(11L, row.getId());
    }

    @Test
    void deleteRemovesBranchProductsThenHeading() {
        BrandModelService heading = BrandModelService.builder().id(4L).titleJson("{}").oil(false).sortOrder(3).build();
        when(brandModelServiceRepository.findById(4L)).thenReturn(Optional.of(heading));

        service.delete(4L);

        verify(brandModelRepository).deleteByBrandModelService_Id(4L);
        verify(brandModelServiceRepository).delete(heading);
    }
}
