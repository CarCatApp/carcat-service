package com.carland.carland_service.test_sima_idda.config;

/**
 * SIMA / IDDA test constants (pre-biosign staging). Not wired to application.yml yet.
 */
public final class SimaIddaConstants {

    private SimaIddaConstants() {
    }

    /** SIMA pre-biosign staging base URL (no trailing slash). */
    public static final String EXAMPLE_SIMA_BASE_URL = "https://pre-biosign-biometric-kyc.sima.az";

    /** Example IDDA staging base URL (placeholder — path not in current HTML docs). */
    public static final String EXAMPLE_IDDA_BASE_URL = "https://example-idda-staging.local";

    /** Partner Identifier header value for SIMA. */
    public static final String EXAMPLE_SIMA_IDENTIFIER = "1155";

    /** HMAC secret issued by SIMA (staging test key). */
    public static final String EXAMPLE_SIMA_HMAC_SECRET = "78TDFSBSDILSDFLMSDFIRASE98EMMAS7";

    /** Auth-Scheme header — must match HMAC algorithm used for Signature. */
    public static final String EXAMPLE_SIMA_AUTH_SCHEME = "HMACSHA256";

    /** Optional DeviceInfo header (same as curl tests). */
    public static final String EXAMPLE_SIMA_DEVICE_INFO = "carland-sima-test";

    /** Staging test person — Postman / Swagger defaults (not auto-filled on request). */
    public static final String EXAMPLE_TEST_PIN = "62HJ5KQ";
    public static final String EXAMPLE_TEST_DOCUMENT_NUMBER = "AB0668397";

    /** Recommended partner thresholds (configured on SIMA side; documented for gate clarity). */
    public static final double EXAMPLE_LIVENESS_THRESHOLD = 0.9;
    public static final double EXAMPLE_SIMILARITY_THRESHOLD = 0.9;

    /** Example IDDA partner / API key header (placeholder). */
    public static final String EXAMPLE_IDDA_PARTNER_CODE = "EXAMPLE_IDDA_PARTNER";
    public static final String EXAMPLE_IDDA_API_KEY = "EXAMPLE_IDDA_API_KEY";
}
