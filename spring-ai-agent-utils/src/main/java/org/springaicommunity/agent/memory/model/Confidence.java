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
package org.springaicommunity.agent.memory.model;

/**
 * Composite confidence model for a {@link KnowledgeUnit}.
 *
 * <p>The overall confidence is computed as:
 * <pre>
 * Confidence = evidenceScore × consistencyScore × validationScore × outcomeScore
 * </pre>
 *
 * <ul>
 *   <li><b>evidenceScore</b>   — grows with number of supporting evidence items:
 *       {@code min(1.0, 0.3 + 0.2 × (count − 1))}</li>
 *   <li><b>consistencyScore</b> — decreases with contradictions:
 *       {@code 1.0 − (contradictions / totalRelated)}</li>
 *   <li><b>validationScore</b>  — reflects review quality:
 *       {@code 1.0} human-validated, {@code 0.7} auto-validated, {@code 0.5} unvalidated</li>
 *   <li><b>outcomeScore</b>    — reflects real-world performance:
 *       {@code successes / total} outcomes; {@code 1.0} when no outcomes yet</li>
 * </ul>
 *
 * <p>A unit whose {@link #value()} is below {@code 0.5} must NOT be included in
 * a {@code ContextPackage} surfaced to an agent.
 *
 * @author Antigravity
 */
public record Confidence(

        /** Fraction derived from evidence count [0.3–1.0]. */
        double evidenceScore,

        /** Fraction reflecting absence of contradictions [0.0–1.0]. */
        double consistencyScore,

        /** Fraction reflecting review quality [0.5–1.0]. */
        double validationScore,

        /** Fraction reflecting real-world outcome success [0.0–1.0]. */
        double outcomeScore

) {

    // ── Validation score constants ─────────────────────────────────────────

    public static final double VALIDATION_HUMAN    = 1.0;
    public static final double VALIDATION_AUTO     = 0.7;
    public static final double VALIDATION_NONE     = 0.5;

    // ── Minimum threshold for context inclusion ────────────────────────────

    public static final double MINIMUM_THRESHOLD   = 0.5;

    // ── Factory methods ────────────────────────────────────────────────────

    /**
     * Initial confidence for a brand-new observation with a single evidence item.
     */
    public static Confidence initial() {
        return new Confidence(0.3, 1.0, VALIDATION_NONE, 1.0);
    }

    /**
     * Creates a Confidence with the given evidence score and defaults for the other factors.
     */
    public static Confidence ofEvidenceCount(int count) {
        double evScore = Math.min(1.0, 0.3 + 0.2 * (count - 1));
        return new Confidence(evScore, 1.0, VALIDATION_NONE, 1.0);
    }

    // ── Core formula ───────────────────────────────────────────────────────

    /**
     * Returns the overall confidence value: evidenceScore × consistencyScore ×
     * validationScore × outcomeScore, clamped to [0.0, 1.0].
     */
    public double value() {
        return Math.max(0.0, Math.min(1.0,
                evidenceScore * consistencyScore * validationScore * outcomeScore));
    }

    /**
     * Returns {@code true} if this confidence meets the minimum threshold for
     * inclusion in agent context assembly.
     */
    public boolean isTrusted() {
        return value() >= MINIMUM_THRESHOLD;
    }

    /**
     * Returns a new {@code Confidence} with the outcome score updated based on
     * recorded outcomes (call after an {@code Outcome} is persisted).
     *
     * @param successfulOutcomes number of successful outcomes observed
     * @param totalOutcomes      total outcomes observed (must be ≥ 1)
     */
    public Confidence withOutcomes(int successfulOutcomes, int totalOutcomes) {
        double newOutcomeScore = totalOutcomes > 0
                ? (double) successfulOutcomes / totalOutcomes
                : 1.0;
        return new Confidence(evidenceScore, consistencyScore, validationScore, newOutcomeScore);
    }

    /**
     * Returns a new {@code Confidence} reflecting human validation.
     */
    public Confidence withHumanValidation() {
        return new Confidence(evidenceScore, consistencyScore, VALIDATION_HUMAN, outcomeScore);
    }

    /**
     * Returns a new {@code Confidence} with the consistency score updated
     * based on contradictions detected.
     *
     * @param contradictions number of contradictions found
     * @param totalRelated   total related knowledge units compared
     */
    public Confidence withConsistency(int contradictions, int totalRelated) {
        double newConsistency = totalRelated > 0
                ? Math.max(0.0, 1.0 - ((double) contradictions / totalRelated))
                : 1.0;
        return new Confidence(evidenceScore, newConsistency, validationScore, outcomeScore);
    }

    @Override
    public String toString() {
        return String.format("Confidence{value=%.3f [evidence=%.2f, consistency=%.2f, validation=%.2f, outcome=%.2f]}",
                value(), evidenceScore, consistencyScore, validationScore, outcomeScore);
    }
}
