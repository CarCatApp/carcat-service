package com.carland.carland_service.feign;

import com.carland.carland_service.dto.response.AuthUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * tr: carland_auth servisinin kullanıcı API'sine bağlanan Feign istemcisi; kullanıcı listesini çekmek için kullanılır.
 * en: Feign client connecting to the carland_auth service's user API; used to fetch the user list.
 */
@FeignClient(name = "authUsers", url = "https://digital-innovation.agency/auth/server/api/v1/users")
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
