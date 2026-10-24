package com.carland.carland_service.feign;

import com.carland.carland_service.dto.response.NameSurname;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * tr: carland_auth iç adresinden ad-soyad çeker. Public gateway konteyner içinden reset yediği için
 *     carland.auth.base-url (auth-service:9090) kullanılır.
 * en: Loads name and surname from carland_auth on the internal address. The public gateway resets
 *     from inside the container, so this uses carland.auth.base-url (auth-service:9090).
 */
@FeignClient(name = "nameSurname", url = "${carland.auth.base-url}/api/v1/users")

public interface NameSurnameFeign {
    /**
     * tr: Verilen userId'ye ait ad-soyad bilgisini carland_auth'dan getirir.
     * en: Fetches the name and surname for the given userId from carland_auth.
     */
    @GetMapping("/getNameSurname")
    NameSurname getNameSurname(@RequestParam Long userId);
}
