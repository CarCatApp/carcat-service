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

    /**
     * tr: QR və klik {@code {public-base-url}/verify/{nömrə}} açır.
     *     Kong {@code /carland/server-carland/verify} yolunu JWT-siz buraxır.
     * en: The QR and the click open {@code {public-base-url}/verify/{number}}.
     *     Kong lets {@code /carland/server-carland/verify} through without JWT.
     */
    public static String verifyUrl(String publicBaseUrl, String code) {
        String number = code == null ? "" : code.trim();
        return base(publicBaseUrl) + "/verify/" + number;
    }

    public static String base(String publicBaseUrl) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("digital-history.public-base-url is empty");
        }
        String base = publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

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

    /**
     * tr: Yoxlama səhifəsi və onun PDF-i. Paylaşım linkini yaradan POST bura düşmür.
     *     {@code /api/v1/sima/verify/*} bura düşməz — {@code contains("/verify/")} SIMA-nı da tuturdu.
     * en: The verify page and its PDF. The POST that mints the share link does not land here.
     *     {@code /api/v1/sima/verify/*} must not match — bare {@code contains("/verify/")} also hit SIMA.
     */
    public static boolean isVerify(String path) {
        String bare = bare(path);
        if (bare.equals("/verify") || bare.startsWith("/verify/")) {
            return true;
        }
        int digitalHistory = bare.indexOf("/digital-history/verify");
        if (digitalHistory >= 0) {
            String after = bare.substring(digitalHistory + "/digital-history/verify".length());
            return after.isEmpty() || after.startsWith("/");
        }
        // /{context}/verify/{code} when the URI still includes the servlet context (one segment only).
        int verifyAt = bare.indexOf("/verify/");
        if (verifyAt > 0) {
            return bare.substring(0, verifyAt).indexOf('/', 1) < 0;
        }
        if (bare.endsWith("/verify")) {
            return bare.substring(0, bare.length() - "/verify".length()).indexOf('/', 1) < 0;
        }
        return false;
    }

    private static String bare(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int query = path.indexOf('?');
        String bare = query >= 0 ? path.substring(0, query) : path;
        if (bare.endsWith("/")) {
            bare = bare.substring(0, bare.length() - 1);
        }
        return bare;
    }
}
