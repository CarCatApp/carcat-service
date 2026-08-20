package com.carland.carland_service.test_sima_idda.feign;

import com.carland.carland_service.test_sima_idda.dto.idda.IddaCarItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Placeholder IDDA Feign — path/contract not defined in current HTML docs.
 * Refactor when real IDDA swagger arrives.
 */
@FeignClient(
        name = "testIddaClient",
        url = "${idda.base-url}"
)
public interface IddaFeign {

    @GetMapping("/api/v1/vehicles")
    List<IddaCarItem> getCarsByFin(
            @RequestHeader("X-Partner-Code") String partnerCode,
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestParam("fin") String fin
    );
}
