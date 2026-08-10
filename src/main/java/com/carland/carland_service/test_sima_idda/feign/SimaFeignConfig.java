package com.carland.carland_service.test_sima_idda.feign;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Ensures minified JSON String is sent as raw body bytes (not JSON-string-escaped).
 * Required so HMAC Signature matches the HTTP body.
 */
public class SimaFeignConfig {

    @Bean
    public Encoder simaRawStringEncoder() {
        return new Encoder() {
            @Override
            public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
                if (object instanceof String json) {
                    template.body(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                    return;
                }
                throw new EncodeException("SimaFeign expects minified JSON String body, got: "
                        + (object == null ? "null" : object.getClass()));
            }
        };
    }
}
