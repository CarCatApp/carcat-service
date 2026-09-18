package com.carland.carland_service.feign;

import com.carland.carland_service.config.InternalTokenFeignConfig;
import com.carland.carland_service.dto.booking.StaffDisableRequest;
import com.carland.carland_service.dto.booking.StaffProvisionRequest;
import com.carland.carland_service.dto.booking.StaffProvisionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "authStaffInternal",
        url = "${carland.auth.base-url}",
        configuration = InternalTokenFeignConfig.class
)
public interface AuthStaffFeign {

    @PostMapping("/api/v1/internal/staff/provision")
    StaffProvisionResponse provision(@RequestBody StaffProvisionRequest request);

    @PostMapping("/api/v1/internal/staff/disable")
    void disable(@RequestBody StaffDisableRequest request);
}
