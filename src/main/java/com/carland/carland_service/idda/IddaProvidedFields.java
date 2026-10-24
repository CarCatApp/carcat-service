package com.carland.carland_service.idda;

import java.util.List;

/**
 * tr: IDDA'dan gelen ve müşterinin değiştiremeyeceği alan adları. Yakıt, ban, renk ve yürüyüş bu listede yoktur.
 * en: Field names that come from IDDA and the customer cannot change. Fuel, body, color and mileage are not in this list.
 */
public final class IddaProvidedFields {

    public static final List<String> NAMES = List.of(
            "plateNumber",
            "vin",
            "brand",
            "model",
            "modelYear",
            "engineVolume"
    );

    private IddaProvidedFields() {
    }
}
