package com.carland.carland_service.test_sima_idda.config;

/**
 * Demo / patron walkthrough constants. Not wired to application.yml or env.
 * Replace EXAMPLE_* values when real SIMA / IDDA credentials arrive.
 */
public final class SimaIddaConstants {

    private SimaIddaConstants() {
    }

    /** Example SIMA staging base URL (no trailing slash). */
    public static final String EXAMPLE_SIMA_BASE_URL = "https://example-sima-staging.azintelecom.az";

    /** Example IDDA staging base URL (placeholder — path not in current HTML docs). */
    public static final String EXAMPLE_IDDA_BASE_URL = "https://example-idda-staging.local";

    /** Partner Identifier header value for SIMA. */
    public static final String EXAMPLE_SIMA_IDENTIFIER = "12345";

    /** HMAC secret issued by SIMA (example only). */
    public static final String EXAMPLE_SIMA_HMAC_SECRET = "EXAMPLE_SIMA_HMAC_SECRET_KEY_DO_NOT_USE_IN_PROD";

    /** Auth-Scheme header — must match HMAC algorithm used for Signature. */
    public static final String EXAMPLE_SIMA_AUTH_SCHEME = "HMACSHA256";

    /** Recommended partner thresholds (configured on SIMA side; documented for gate clarity). */
    public static final double EXAMPLE_LIVENESS_THRESHOLD = 0.9;
    public static final double EXAMPLE_SIMILARITY_THRESHOLD = 0.9;

    /** Example IDDA partner / API key header (placeholder). */
    public static final String EXAMPLE_IDDA_PARTNER_CODE = "EXAMPLE_IDDA_PARTNER";
    public static final String EXAMPLE_IDDA_API_KEY = "EXAMPLE_IDDA_API_KEY";
}
