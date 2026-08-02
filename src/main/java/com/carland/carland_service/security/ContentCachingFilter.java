package com.carland.carland_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * tr: Tüm istekleri ContentCachingRequestWrapper ile sarar; böylece request body'si birden fazla kez
 *     okunabilir hale gelir (örn. webhook imza doğrulaması + controller). En yüksek öncelikle çalışır.
 * en: Wraps every request in a ContentCachingRequestWrapper so the request body can be read more than
 *     once (e.g. webhook signature validation + controller). Runs at highest precedence.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentCachingFilter extends OncePerRequestFilter {

    /**
     * tr: İstek zaten sarılı değilse ContentCachingRequestWrapper ile sarıp zinciri devam ettirir.
     * en: Wraps the request in a ContentCachingRequestWrapper when not already wrapped and continues the chain.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest wrapped = request instanceof ContentCachingRequestWrapper
                ? request
                : new ContentCachingRequestWrapper(request);
        filterChain.doFilter(wrapped, response);
    }
}
