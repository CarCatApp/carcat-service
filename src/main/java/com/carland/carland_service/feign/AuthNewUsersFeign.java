package com.carland.carland_service.feign;

import com.carland.carland_service.dto.response.AdminAuthLoginResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "authNewUsersLogin", url = "${carland.admin.auth-base-url}")
public interface AuthNewUsersFeign {

    @PostMapping("/login")
    AdminAuthLoginResponse login(@RequestBody Map<String, String> body,
                       @RequestHeader("Accept-Language") String acceptLanguage);
}
