package com.carland.carland_service.service.commission;

import com.carland.carland_service.dto.response.hyper.HyperServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.service.impl.HyperTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: VIN'i Hyper'da yalnız okur. Ziyaret yazmaz, cache'e dokunmaz. 5 saniyede cevap yoksa başarısız.
 * en: Reads a VIN from Hyper only. Does not write visits or touch cache. No answer in 5 seconds is a failure.
 */
@Slf4j
@Service
public class HyperVinLookup {

    private final HyperTokenService hyperTokenService;
    private final RestTemplate http;
    private final String hyperBaseUrl;

    public HyperVinLookup(HyperTokenService hyperTokenService,
                          @Value("${hyper.auth.base-url}") String hyperBaseUrl) {
        this.hyperTokenService = hyperTokenService;
        this.hyperBaseUrl = hyperBaseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        this.http = new RestTemplate(factory);
    }

    public HyperVinSnapshot lookup(String vin) {
        String token = hyperTokenService.getToken();
        if (token == null || token.isBlank()) {
            return HyperVinSnapshot.failed("no_token");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String url = hyperBaseUrl + "/partner/v1/vehicles/by-vin/" + vin;
        try {
            ResponseEntity<HyperVehicleByVinResponse> response = http.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), HyperVehicleByVinResponse.class);
            return fromBody(response.getBody());
        } catch (HttpClientErrorException.NotFound notFound) {
            String body = notFound.getResponseBodyAsString();
            if (body != null && body.contains("vehicle_not_found")) {
                return HyperVinSnapshot.notFound("vehicle_not_found");
            }
            log.warn("commission hyper vin lookup not found | vin={} status=404", vin);
            return HyperVinSnapshot.failed("not_found");
        } catch (RuntimeException ex) {
            log.warn("commission hyper vin lookup failed | vin={} err={}", vin, ex.getClass().getSimpleName());
            return HyperVinSnapshot.failed(ex.getClass().getSimpleName());
        }
    }

    private static HyperVinSnapshot fromBody(HyperVehicleByVinResponse body) {
        if (body == null || body.getServiceHistory() == null || body.getServiceHistory().isEmpty()) {
            return HyperVinSnapshot.notFound("empty");
        }
        List<LocalDate> dates = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (HyperServiceHistoryItemResponse item : body.getServiceHistory()) {
            if (item == null) {
                continue;
            }
            dates.add(item.getLastServiceDate());
            if (text.length() > 1500) {
                continue;
            }
            if (text.length() > 0) {
                text.append("; ");
            }
            text.append(item.getRecordId() == null ? "?" : item.getRecordId());
            text.append(' ');
            text.append(item.getLastServiceDate());
        }
        if (text.length() > 2000) {
            text.setLength(2000);
        }
        return HyperVinSnapshot.found(dates, text.toString());
    }
}
