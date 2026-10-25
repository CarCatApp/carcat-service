package com.carland.carland_service.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * tr: Response JSON'daki plateNumber alanını görüntü formatında yazar.
 * en: Writes a response plateNumber field in display format.
 */
public class PlateNumberSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(PlateNumbers.format(value));
    }
}
