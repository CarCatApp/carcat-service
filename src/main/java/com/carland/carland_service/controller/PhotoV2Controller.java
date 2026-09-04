package com.carland.carland_service.controller;

import com.carland.carland_service.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: Araç fotoğrafı v2 GET — gövde byte, X-Photo-Status / X-Photo-Source header. Servis v1 ile aynı.
 * en: Car photo v2 GET — body bytes, X-Photo-Status / X-Photo-Source headers. Same PhotoService as v1.
 */
@RestController
@RequestMapping("/api/v2/photo")
@RequiredArgsConstructor
public class PhotoV2Controller {

    private final PhotoService photoService;

    @GetMapping(value = "/for/car/get", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getCarPhoto(
            @RequestHeader("role") String role,
            @RequestParam("carId") Long carId,
            @RequestHeader("phoneNumber") String phoneNumber,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-Client-Timezone") String timezone,
            @RequestHeader("Accept-Language") String acceptLanguage) {
        return photoService.getCarPhotoV2(role, carId, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }
}
