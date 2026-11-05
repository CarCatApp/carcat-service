package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Mobilin logo QR'a çevirdiği adres. {@code expiresInSeconds} şu an 300.
 * en: The URL mobile turns into a logo QR. {@code expiresInSeconds} is 300 for now.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalHistoryLinkResponse {
    private String url;
    private long expiresInSeconds;
}
