package com.carland.carland_service.feign;

import com.carland.carland_service.dto.response.AuthUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "authUsers", url = "https://digital-innovation.agency/auth/server/api/v1/users")
public interface AuthUsersFeign {

    /**
     * carland_auth'dan kullanıcı listesi. from/to (ISO yyyy-MM-dd) opsiyonel,
     * verilirse createdAt aralığına göre filtreler (to günü dahil).
     */
    @GetMapping("/list")
    List<AuthUser> getUserList(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    );
}
