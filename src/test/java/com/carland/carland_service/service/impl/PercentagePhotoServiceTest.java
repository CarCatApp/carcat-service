package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.PercentageEmptyPhoto;
import com.carland.carland_service.entity.PercentagePhoto;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.PercentageEmptyPhotoRepository;
import com.carland.carland_service.repository.PercentagePhotoRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.service.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PercentagePhotoServiceTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Mock PercentagePhotoRepository percentagePhotoRepository;
    @Mock PercentageEmptyPhotoRepository percentageEmptyPhotoRepository;
    @Mock ServiceEntityRepository serviceEntityRepository;
    @Mock RedisCacheService redisCacheService;

    @InjectMocks PhotoServiceImpl photoService;

    @Test
    void getRequiresServiceId() {
        assertThrows(MissingFieldException.class, () -> photoService.getPercentagePhoto(null));
    }

    @Test
    void getUnknownServiceIs404() {
        when(serviceEntityRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> photoService.getPercentagePhoto(99L));
    }

    @Test
    void getReturnsOwnPhotoAndCachesUnderServiceId() {
        when(serviceEntityRepository.existsById(10L)).thenReturn(true);
        when(redisCacheService.getPercentagePhoto(10L)).thenReturn(null);
        PercentagePhoto row = PercentagePhoto.builder()
                .serviceId(10L)
                .fileType("png")
                .imageData(PNG)
                .build();
        when(percentagePhotoRepository.findByServiceId(10L)).thenReturn(row);

        ResponseEntity<byte[]> res = photoService.getPercentagePhoto(10L);

        assertEquals(MediaType.IMAGE_PNG, res.getHeaders().getContentType());
        assertArrayEquals(PNG, res.getBody());
        verify(redisCacheService).putPercentagePhoto(eq(10L), eq(MediaType.IMAGE_PNG), eq(PNG));
        verify(redisCacheService, never()).putPercentageEmptyPhoto(any(), any());
    }

    @Test
    void getFallsBackToEmptyWithoutCachingUnderServiceId() {
        when(serviceEntityRepository.existsById(10L)).thenReturn(true);
        when(redisCacheService.getPercentagePhoto(10L)).thenReturn(null);
        when(percentagePhotoRepository.findByServiceId(10L)).thenReturn(null);
        when(redisCacheService.getPercentageEmptyPhoto()).thenReturn(null);
        PercentageEmptyPhoto empty = PercentageEmptyPhoto.builder()
                .fileType("png")
                .imageData(PNG)
                .build();
        when(percentageEmptyPhotoRepository.findFirstByOrderByImageIdAsc()).thenReturn(Optional.of(empty));

        ResponseEntity<byte[]> res = photoService.getPercentagePhoto(10L);

        assertArrayEquals(PNG, res.getBody());
        verify(redisCacheService, never()).putPercentagePhoto(any(), any(), any());
        verify(redisCacheService).putPercentageEmptyPhoto(eq(MediaType.IMAGE_PNG), eq(PNG));
    }

    @Test
    void get404WhenOwnAndEmptyMissing() {
        when(serviceEntityRepository.existsById(10L)).thenReturn(true);
        when(redisCacheService.getPercentagePhoto(10L)).thenReturn(null);
        when(percentagePhotoRepository.findByServiceId(10L)).thenReturn(null);
        when(redisCacheService.getPercentageEmptyPhoto()).thenReturn(null);
        when(percentageEmptyPhotoRepository.findFirstByOrderByImageIdAsc()).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> photoService.getPercentagePhoto(10L));
    }

    @Test
    void uploadReplacesExistingAndEvicts() throws Exception {
        when(serviceEntityRepository.existsById(10L)).thenReturn(true);
        PercentagePhoto old = PercentagePhoto.builder().serviceId(10L).imageData(new byte[] {1}).build();
        when(percentagePhotoRepository.findByServiceId(10L)).thenReturn(old);
        MockMultipartFile file = new MockMultipartFile("file", "oil.png", "image/png", PNG);

        photoService.uploadPercentagePhoto(file, 10L);

        verify(percentagePhotoRepository).delete(old);
        verify(percentagePhotoRepository).save(any(PercentagePhoto.class));
        verify(redisCacheService).evictPercentagePhotoAfterCommit(10L);
    }
}
