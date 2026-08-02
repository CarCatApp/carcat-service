package com.carland.carland_service.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * tr: Request body'sini belleğe kopyalayan sarmalayıcıdır; body'nin hem HMAC imza doğrulamasında
 *     hem de controller'da tekrar okunabilmesini sağlar.
 * en: Request wrapper that copies the body into memory; allows the body to be re-read both by
 *     HMAC signature validation and by the controller.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    /**
     * tr: Orijinal isteğin input stream'ini tamamen okuyup byte dizisi olarak saklar.
     * en: Fully reads the original request's input stream and stores it as a byte array.
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /**
     * tr: Cache'lenmiş ham body byte'larını döner.
     * en: Returns the cached raw body bytes.
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    /**
     * tr: Cache'lenmiş body üzerinden yeni bir ServletInputStream döner; her çağrıda baştan okunabilir.
     * en: Returns a fresh ServletInputStream over the cached body; can be re-read on every call.
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    /**
     * tr: Cache'lenmiş body'yi UTF-8 olarak okuyan BufferedReader döner.
     * en: Returns a BufferedReader reading the cached body as UTF-8.
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
