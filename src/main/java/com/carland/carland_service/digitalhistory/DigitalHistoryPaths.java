package com.carland.carland_service.digitalhistory;

/**
 * tr: Tarayıcının açtığı PDF adresi. Query'de {@code jwt} durur, Authorization header'ı yoktur.
 *     Kong JWT eklentisi varsayılan olarak {@code jwt} query parametresini de okur.
 *     Canlı ayarda {@code uri_param_names} boşaltılmışsa bu path 401 verir; listeye {@code jwt} geri konur.
 * en: PDF URL the browser opens. The {@code jwt} is in the query, there is no Authorization header.
 *     Kong's JWT plugin also reads the {@code jwt} query parameter by default.
 *     If the live {@code uri_param_names} list was cleared, this path returns 401 until {@code jwt} is put back.
 */
public final class DigitalHistoryPaths {

    public static final String PDF = "/api/v1/car/digital-history";

    private DigitalHistoryPaths() {
    }

    /**
     * tr: Yalnız PDF GET'i. Paylaşım linkini üreten POST bu path'in altında bitmez.
     * en: The PDF GET only. The POST that mints the share link does not end at this path.
     */
    public static boolean isPdf(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        int query = path.indexOf('?');
        String bare = query >= 0 ? path.substring(0, query) : path;
        if (bare.endsWith("/")) {
            bare = bare.substring(0, bare.length() - 1);
        }
        return PDF.equals(bare) || bare.endsWith(PDF);
    }
}
