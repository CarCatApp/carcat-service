package com.carland.carland_service.service.commission;

import com.carland.carland_service.dto.response.hyper.HyperServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.service.impl.HyperTokenService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * tr: VIN-i Hyper-da yalnız oxuyur. Ziyarət yazmır, cache-ə toxunmur. 5 saniyədə cavab yoxdursa uğursuz.
 * en: Reads a VIN from Hyper only. Does not write visits or touch cache. No answer in 5 seconds is a failure.
 */
@Slf4j
@Service
public class HyperVinLookup {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

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
            ResponseEntity<String> response = http.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return HyperVinSnapshot.notFound("empty", clip(body));
            }
            try {
                return fromBody(JSON.readValue(body, HyperVehicleByVinResponse.class), body);
            } catch (Exception ex) {
                return HyperVinSnapshot.failed("unreadable", clip(body));
            }
        } catch (HttpClientErrorException.NotFound notFound) {
            String body = notFound.getResponseBodyAsString();
            if (body != null && body.contains("vehicle_not_found")) {
                return HyperVinSnapshot.notFound("vehicle_not_found", clip(body));
            }
            log.warn("commission hyper vin lookup not found | vin={} status=404", vin);
            return HyperVinSnapshot.failed("not_found", clip(body));
        } catch (RuntimeException ex) {
            log.warn("commission hyper vin lookup failed | vin={} err={}", vin, ex.getClass().getSimpleName());
            return HyperVinSnapshot.failed(ex.getClass().getSimpleName());
        }
    }

    private static HyperVinSnapshot fromBody(HyperVehicleByVinResponse body, String raw) {
        if (body == null || body.getServiceHistory() == null || body.getServiceHistory().isEmpty()) {
            return HyperVinSnapshot.notFound("empty", clip(raw));
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
        return HyperVinSnapshot.found(dates, text.toString(), clip(raw));
    }

    private static String clip(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 4000 ? value : value.substring(0, 4000);
    }
}
