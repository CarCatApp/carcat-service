package com.carland.carland_service.service;

import com.carland.carland_service.entity.Brand;

import java.util.List;

/**
 * tr: NHTSA Make değerini add-car kataloğundaki marka adına çevirir.
 *     Birebir eşleşme büyük/küçük harfi yok sayar ve katalog yazımını döner.
 *     MERCEDES-BENZ tek istisnadır: katalogda Mercedes varsa onu döner.
 *     Örtüşmezse null döner; Flutter katalogda olmayan isimle model listesi açmasın.
 * en: Maps an NHTSA Make onto an add-car catalog brand.
 *     An exact match ignores case and returns the catalog spelling.
 *     MERCEDES-BENZ is the one exception and maps to Mercedes when that brand exists.
 *     Anything else that does not match returns null, so the app does not query models for an unknown name.
 */
public final class VinBrandCatalog {

    private static final String NHTSA_MERCEDES_BENZ = "MERCEDES-BENZ";
    private static final String CATALOG_MERCEDES = "Mercedes";

    private VinBrandCatalog() {
    }

    public static String match(String nhtsaMake, List<Brand> brands) {
        if (nhtsaMake == null || brands == null) {
            return null;
        }
        String make = nhtsaMake.trim();
        if (make.isEmpty()) {
            return null;
        }
        if (make.equalsIgnoreCase(NHTSA_MERCEDES_BENZ)) {
            return spelling(brands, CATALOG_MERCEDES);
        }
        return spelling(brands, make);
    }

    private static String spelling(List<Brand> brands, String name) {
        for (Brand brand : brands) {
            if (brand == null || brand.getBrandName() == null) {
                continue;
            }
            if (brand.getBrandName().equalsIgnoreCase(name)) {
                return brand.getBrandName();
            }
        }
        return null;
    }
}
