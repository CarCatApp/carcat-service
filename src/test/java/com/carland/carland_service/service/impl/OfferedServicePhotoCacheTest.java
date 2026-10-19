package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.OfferedServicePhoto;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.OfferedServicePhotoRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
import com.carland.carland_service.service.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferedServicePhotoCacheTest {

    @Mock OfferedServicePhotoRepository offeredServicePhotoRepository;
    @Mock OfferedServiceRepository offeredServiceRepository;
    @Mock RedisCacheService redisCacheService;

    @InjectMocks PhotoServiceImpl photoService;

    @Test
    void redisHitSkipsOfferedServiceCount() {
        byte[] bytes = new byte[] {1, 2, 3};
        when(redisCacheService.getOfferedServicePhoto(5L))
                .thenReturn(ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes));

        ResponseEntity<byte[]> res = photoService.getOfferedServicePhoto(5L);

        assertArrayEquals(bytes, res.getBody());
        verify(offeredServiceRepository, never()).existsById(any());
        verify(offeredServicePhotoRepository, never()).findByOfferedServiceId(any());
    }

    @Test
    void missStillRejectsUnknownService() {
        when(redisCacheService.getOfferedServicePhoto(9L)).thenReturn(null);
        when(offeredServiceRepository.existsById(9L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> photoService.getOfferedServicePhoto(9L));
        verify(offeredServicePhotoRepository, never()).findByOfferedServiceId(any());
    }

    @Test
    void missLoadsPhotoAndPutsRedis() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47};
        when(redisCacheService.getOfferedServicePhoto(5L)).thenReturn(null);
        when(offeredServiceRepository.existsById(5L)).thenReturn(true);
        when(offeredServicePhotoRepository.findByOfferedServiceId(5L)).thenReturn(
                OfferedServicePhoto.builder()
                        .offeredServiceId(5L)
                        .fileType("image/png")
                        .imageData(png)
                        .build());

        ResponseEntity<byte[]> res = photoService.getOfferedServicePhoto(5L);

        assertArrayEquals(png, res.getBody());
        verify(redisCacheService).putOfferedServicePhoto(eq(5L), eq(MediaType.IMAGE_PNG), eq(png));
    }
}
