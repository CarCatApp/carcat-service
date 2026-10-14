package com.carland.carland_service.entity;

/**
 * tr: Şöbənin rezervasiyanı necə bağladığı. Köhnə boş sətir webhook sayılır.
 * en: How a branch closes a booking. A blank legacy row counts as webhook.
 */
public final class BranchCompletion {

    public static final String WEBHOOK = "webhook";
    public static final String DETAILED = "detailed";

    private BranchCompletion() {
    }

    public static String shown(String stored) {
        return DETAILED.equals(stored) ? DETAILED : WEBHOOK;
    }
}
