package com.carland.carland_service.feign;

import com.carland.carland_service.dto.response.NameSurname;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * tr: carland_auth servisinden kullanıcının ad-soyad bilgisini çeken Feign istemcisi.
 * en: Feign client fetching a user's name and surname from the carland_auth service.
 */
@FeignClient(name = "nameSurname", url = "https://digital-innovation.agency/auth/server/api/v1/users")

public interface NameSurnameFeign {
    /**
     * tr: Verilen userId'ye ait ad-soyad bilgisini carland_auth'dan getirir.
     * en: Fetches the name and surname for the given userId from carland_auth.
     */
    @GetMapping("/getNameSurname")
    NameSurname getNameSurname(@RequestParam Long userId);
}
