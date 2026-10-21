package com.carland.carland_service.service;

/**
 * tr: Config'teki mağaza linkleri. Boş veya https olmayan link CTA sayılmaz.
 * en: Store links from config. A blank or non-https link does not count as a CTA.
 */
public record StoreLinks(String ios, String android) {
}
