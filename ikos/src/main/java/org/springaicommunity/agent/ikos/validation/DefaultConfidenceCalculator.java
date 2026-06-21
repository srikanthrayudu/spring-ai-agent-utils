/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.agent.ikos.validation;

import org.springaicommunity.agent.ikos.model.Confidence;
import org.springaicommunity.agent.ikos.model.Evidence;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.Outcome;
import org.springaicommunity.agent.ikos.model.ValidationResult;

import java.util.List;

/**
 * Default implementation of {@link ConfidenceCalculator}.
 *
 * <p>Each factor is computed independently:
 * <ul>
 *   <li><b>evidenceScore</b>:   {@code min(1.0, 0.3 + 0.2 × (evidenceCount − 1))}</li>
 *   <li><b>consistencyScore</b>: {@code 1.0 − (contradictions / max(1, totalKnown))}</li>
 *   <li><b>validationScore</b>:  Human → 1.0 | Auto (system) → 0.7 | None → 0.5</li>
 *   <li><b>outcomeScore</b>:     Outcome success/failure; 1.0 when no outcomes recorded</li>
 * </ul>
 *
 * @author Antigravity
 */
public class DefaultConfidenceCalculator implements ConfidenceCalculator {

    /** Number of related units used as denominator for consistency. */
    private final int totalRelatedKnowledge;

    /**
     * Constructs a calculator with a known corpus size for consistency scoring.
     *
     * @param totalRelatedKnowledge total knowledge units in the relevant scope;
     *                              used to normalize contradiction count.
     */
    public DefaultConfidenceCalculator(int totalRelatedKnowledge) {
        this.totalRelatedKnowledge = Math.max(1, totalRelatedKnowledge);
    }

    /** Constructs a calculator assuming an empty corpus (consistency = 1.0). */
    public DefaultConfidenceCalculator() {
        this(1);
    }

    @Override
    public Confidence calculate(KnowledgeUnit unit, ValidationResult validationResult) {
        double evidenceScore   = computeEvidenceScore(unit.evidence());
        double consistencyScore = computeConsistencyScore(validationResult);
        double validationScore  = computeValidationScore(validationResult);
        double outcomeScore     = computeOutcomeScore(unit.getOutcome());
        return new Confidence(evidenceScore, consistencyScore, validationScore, outcomeScore);
    }

    // ── Factor computations ────────────────────────────────────────────────

    private double computeEvidenceScore(List<Evidence> evidence) {
        int count = (evidence == null) ? 0 : evidence.size();
        return Math.min(1.0, 0.3 + 0.2 * Math.max(0, count - 1));
    }

    private double computeConsistencyScore(ValidationResult result) {
        if (result == null) return 1.0;
        int contradictions = result.contradictions() == null ? 0 : result.contradictions().size();
        return Math.max(0.0, 1.0 - ((double) contradictions / totalRelatedKnowledge));
    }

    private double computeValidationScore(ValidationResult result) {
        if (result == null) return Confidence.VALIDATION_NONE;
        String reviewer = result.reviewedBy();
        if (reviewer == null || reviewer.isBlank() || "system".equalsIgnoreCase(reviewer)) {
            return Confidence.VALIDATION_AUTO;
        }
        return Confidence.VALIDATION_HUMAN;
    }

    private double computeOutcomeScore(Outcome outcome) {
        // No outcome recorded yet → neutral (1.0)
        if (outcome == null) return 1.0;
        // Binary outcome: successful → boosts, failure → penalises
        return outcome.successful() ? 1.0 : 0.3;
    }
}
