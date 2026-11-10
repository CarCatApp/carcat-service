package com.carland.carland_service.idda;

/**
 * tr: IDDA teknik hata (timeout, host, HTTP, bozuk cevap). Bu çağrı hakka ve bakiyeye yazılmaz.
 * en: IDDA technical failure (timeout, host, HTTP, unreadable body). This call does not count and is not a charged success.
 */
public class IddaUnavailableException extends RuntimeException {

    public IddaUnavailableException(String message) {
        super(message);
    }
}
