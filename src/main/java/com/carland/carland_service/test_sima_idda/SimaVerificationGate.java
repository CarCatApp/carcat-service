package com.carland.carland_service.test_sima_idda;

import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaIdentityResult;

/**
 * CRCT-249: verified only when SIMA isSuccess and both scores are &gt;= 0.90.
 */
public final class SimaVerificationGate {

    public static final double SCORE_THRESHOLD = 0.90;

    private SimaVerificationGate() {
    }

    public static boolean scoresPass(Double livenessScore, Double similarityScore) {
        return livenessScore != null
                && similarityScore != null
                && livenessScore >= SCORE_THRESHOLD
                && similarityScore >= SCORE_THRESHOLD;
    }

    public static boolean attemptVerified(SimaApiEnvelope envelope) {
        if (envelope == null || !Boolean.TRUE.equals(envelope.getIsSuccess()) || envelope.getResult() == null) {
            return false;
        }
        SimaIdentityResult result = envelope.getResult();
        return scoresPass(result.getLivenessScore(), result.getSimilarityScore());
    }
}
