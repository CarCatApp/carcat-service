package com.carland.carland_service.test_sima_idda.controller;

import com.carland.carland_service.test_sima_idda.dto.response.IddaCarListResponse;
import com.carland.carland_service.test_sima_idda.service.IddaCarListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IDDA car-list demo API (separate from SIMA verify).
 * Placeholder Feign path until real IDDA contract is provided.
 */
@RestController
@RequestMapping("/api/v1/idda")
@RequiredArgsConstructor
public class IddaController {

    private final IddaCarListService iddaCarListService;

    /**
     * Example: get vehicle list by FIN, compare VIN with local customer cars (log only).
     */
    @GetMapping("/cars")
    public IddaCarListResponse getCarsByFin(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("fin") String fin
    ) {
        return iddaCarListService.getCarsByFin(userIdHeader, fin);
    }
}
