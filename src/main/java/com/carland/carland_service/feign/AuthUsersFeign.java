package com.carland.carland_service.feign;

import com.carland.carland_service.dto.response.AuthUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * tr: carland_auth kullanıcı listesi. Public gateway konteyner içinden TLS el sıkışmasını kestiği için
 *     carland.auth.base-url (auth-service:9090) kullanılır.
 * en: carland_auth user list. The public gateway drops the TLS handshake from inside the container,
 *     so this uses carland.auth.base-url (auth-service:9090).
 */
@FeignClient(name = "authUsers", url = "${carland.auth.base-url}/api/v1/users")
public interface AuthUsersFeign {

    /**
     * tr: carland_auth'dan kullanıcı listesi. from/to (ISO yyyy-MM-dd) opsiyonel,
     * verilirse createdAt aralığına göre filtreler (to günü dahil).
     * en: Fetches the user list from carland_auth. from/to (ISO yyyy-MM-dd) are optional;
     * when provided, filters by the createdAt range (the "to" day is inclusive).
     */
    @GetMapping("/list")
    List<AuthUser> getUserList(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    );
}
