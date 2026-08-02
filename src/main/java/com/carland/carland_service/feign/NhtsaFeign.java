package com.carland.carland_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * tr: ABD NHTSA vPIC API'sine bağlanan Feign istemcisi; VIN kodundan araç bilgilerini çözmek için kullanılır.
 * en: Feign client connecting to the US NHTSA vPIC API; used to decode vehicle information from a VIN code.
 */
@FeignClient(name = "nhtsaClient", url = "https://vpic.nhtsa.dot.gov/api/vehicles")

public interface NhtsaFeign {
    /**
     * tr: Verilen VIN'i NHTSA servisinde çözümleyip sonucu istenen formatta (örn. json) ham Map olarak döner.
     * en: Decodes the given VIN via the NHTSA service and returns the result in the requested format (e.g. json) as a raw Map.
     */
    @GetMapping("/decodevin/{vin}")
    Map<String, Object> decodeVin(@PathVariable("vin") String vin,
                                  @RequestParam("format") String format);
}
