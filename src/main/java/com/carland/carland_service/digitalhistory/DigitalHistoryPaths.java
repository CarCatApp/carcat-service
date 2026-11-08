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

    /** tr: QR və klik bu ünvana gedir. Kong bu yolu JWT-siz buraxmalıdır. / en: The QR and the click open this URL. Kong must allow the path without JWT. */
    public static final String VERIFY_ORIGIN = "https://carcat.az/verify/";

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
     * en: The verify page and its PDF. The POST that mints the share link does not land here.
     */
    public static boolean isVerify(String path) {
        String bare = bare(path);
        return bare.equals("/verify")
                || bare.startsWith("/verify/")
                || bare.contains("/verify/")
                || bare.contains("/digital-history/verify/")
                || bare.endsWith("/digital-history/verify");
    }

    public static String verifyUrl(String code) {
        return VERIFY_ORIGIN + code;
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
