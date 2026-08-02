package com.carland.carland_service.feign;

import com.carland.carland_service.dto.response.hyper.HyperTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * tr: Hyper kimlik doğrulama servisinden Bearer token alan Feign istemcisi; token ömrü 1 saattir ve uç saatte 20 istekle sınırlı olduğu için alınan token cache'lenip tekrar kullanılmalıdır.
 * en: Retrieves Bearer token from Hyper authentication service.
 * Token lifetime is 1 hour and the endpoint is limited to 20 requests per hour.
 * The received token should be cached and reused for subsequent Hyper API calls.
 */
@FeignClient(name = "hyperAuthClient", url = "${hyper.auth.base-url}")
public interface HyperAuthClient {

    /**
     * tr: Form-urlencoded kimlik bilgileriyle Hyper token ucuna POST atar ve HyperTokenResponse içinde Bearer token'ı döner.
     * en: Posts the form-urlencoded credentials to the Hyper token endpoint and returns the Bearer token inside a HyperTokenResponse.
     */
    @PostMapping(value = "${hyper.auth.token-url}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    HyperTokenResponse getToken(@RequestBody MultiValueMap<String, String> form);

}
