package com.carland.carland_service.test_sima_idda.feign;

import com.carland.carland_service.test_sima_idda.config.SimaIddaConstants;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * SIMA KYC Feign — HTML AzInTelecom paths.
 * Body must already be minified JSON string; Signature covers those exact bytes.
 */
@FeignClient(
        name = "testSimaKycClient",
        url = SimaIddaConstants.EXAMPLE_SIMA_BASE_URL,
        configuration = SimaFeignConfig.class
)
public interface SimaFeign {

    @PostMapping(value = "/api/v1/kyc/identity/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    SimaApiEnvelope verifyCitizen(
            @RequestHeader("Identifier") String identifier,
            @RequestHeader("Auth-Scheme") String authScheme,
            @RequestHeader("Signature") String signature,
            @RequestHeader(value = "DeviceInfo", required = false) String deviceInfo,
            @RequestBody String minifiedJsonBody
    );

    // PO: passport channel not needed for now.
//    @PostMapping(value = "/api/v1/kyc/identity/verify/passport", consumes = MediaType.APPLICATION_JSON_VALUE)
//    SimaApiEnvelope verifyPassport(
//            @RequestHeader("Identifier") String identifier,
//            @RequestHeader("Auth-Scheme") String authScheme,
//            @RequestHeader("Signature") String signature,
//            @RequestHeader(value = "DeviceInfo", required = false) String deviceInfo,
//            @RequestBody String minifiedJsonBody
//    );

    @PostMapping(value = "/api/v1/kyc/identity/verify/foreign", consumes = MediaType.APPLICATION_JSON_VALUE)
    SimaApiEnvelope verifyForeign(
            @RequestHeader("Identifier") String identifier,
            @RequestHeader("Auth-Scheme") String authScheme,
            @RequestHeader("Signature") String signature,
            @RequestHeader(value = "DeviceInfo", required = false) String deviceInfo,
            @RequestBody String minifiedJsonBody
    );
}
