package com.carland.carland_service.test_sima_idda;

import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaIdentityResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimaVerificationGateTest {

    @Test
    void qa1_highScoresPass() {
        assertTrue(SimaVerificationGate.scoresPass(0.996, 0.999));
    }

    @Test
    void qa2_lowLivenessFails() {
        assertFalse(SimaVerificationGate.scoresPass(0.85, 0.999));
    }

    @Test
    void qa3_lowSimilarityFails() {
        assertFalse(SimaVerificationGate.scoresPass(0.999, 0.80));
    }

    @Test
    void qa4_boundaryPasses() {
        assertTrue(SimaVerificationGate.scoresPass(0.90, 0.90));
    }

    @Test
    void qa5_justBelowLivenessFails() {
        assertFalse(SimaVerificationGate.scoresPass(0.899, 0.95));
    }

    @Test
    void nullScoresFail() {
        assertFalse(SimaVerificationGate.scoresPass(null, 0.99));
        assertFalse(SimaVerificationGate.scoresPass(0.99, null));
    }

    @Test
    void needsSimaSuccessToo() {
        SimaIdentityResult result = SimaIdentityResult.builder()
                .livenessScore(0.99)
                .similarityScore(0.99)
                .build();
        assertFalse(SimaVerificationGate.attemptVerified(SimaApiEnvelope.builder()
                .isSuccess(false)
                .result(result)
                .build()));
        assertTrue(SimaVerificationGate.attemptVerified(SimaApiEnvelope.builder()
                .isSuccess(true)
                .result(result)
                .build()));
    }
}
