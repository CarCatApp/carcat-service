package com.carland.carland_service.digitalhistory;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * tr: CC-il-10 simvol. 0, O, 1 və I yoxdur. Ardıcıl deyil.
 * en: CC-year-10 characters. 0, O, 1 and I are left out. Not sequential.
 */
@Component
public class DigitalHistoryCodes {

    static final Pattern CODE = Pattern.compile("CC-\\d{4}-[A-HJ-NP-Z2-9]{10}");

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String next(int year) {
        char[] body = new char[10];
        for (int i = 0; i < body.length; i++) {
            body[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return "CC-" + year + "-" + new String(body);
    }

    public static boolean matches(String code) {
        return code != null && CODE.matcher(code).matches();
    }
}
