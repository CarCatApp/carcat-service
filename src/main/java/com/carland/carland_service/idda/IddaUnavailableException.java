package com.carland.carland_service.idda;

/**
 * tr: IDDA'ya ulaşılamadı (timeout veya bozuk cevap). Bu çağrı hakka ve bakiyeye yazılmaz.
 * en: IDDA could not be reached (timeout or unreadable body). This call does not count and is not a charged success.
 */
public class IddaUnavailableException extends RuntimeException {

    public IddaUnavailableException(String message) {
        super(message);
    }
}
