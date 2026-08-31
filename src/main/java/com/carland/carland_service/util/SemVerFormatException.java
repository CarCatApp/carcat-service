package com.carland.carland_service.util;

/**
 * tr: SemVer parse edilemeyen string.
 * en: Thrown when a string is not a parseable SemVer.
 */
public class SemVerFormatException extends IllegalArgumentException {

    public SemVerFormatException(String message) {
        super(message);
    }
}
