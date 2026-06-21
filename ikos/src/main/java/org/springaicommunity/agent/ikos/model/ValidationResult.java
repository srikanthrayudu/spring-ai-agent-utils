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
package org.springaicommunity.agent.ikos.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Captures the outcome of validating a {@link KnowledgeUnit} against existing knowledge.
 *
 * <p>A {@code ValidationResult} records whether the unit passed validation, the calculated
 * confidence score, any detected contradictions or exceptions, and who performed the review.
 *
 * @author Antigravity
 */
public record ValidationResult(

        /** Whether the unit passed all validation checks. */
        boolean valid,

        /** Composite confidence score at the time of validation [0.0–1.0]. */
        double confidenceScore,

        /** Human-readable descriptions of detected contradictions with existing knowledge. */
        List<String> contradictions,

        /** Human-readable descriptions of edge cases or exceptions that limit applicability. */
        List<String> exceptions,

        /** Name or ID of the reviewer (human or system component). */
        String reviewedBy,

        /** Timestamp when validation was performed. */
        LocalDateTime reviewedAt

) {

    /**
     * Convenience factory for a passing, auto-validated result with no contradictions.
     */
    public static ValidationResult passing(double confidenceScore) {
        return new ValidationResult(true, confidenceScore, List.of(), List.of(), "system", LocalDateTime.now());
    }

    /**
     * Convenience factory for a failing result with detected contradictions.
     */
    public static ValidationResult failing(List<String> contradictions) {
        return new ValidationResult(false, 0.0, contradictions, List.of(), "system", LocalDateTime.now());
    }

    /**
     * Convenience factory for a human-reviewed result.
     */
    public static ValidationResult humanReviewed(boolean valid, double confidenceScore,
            List<String> contradictions, List<String> exceptions, String reviewedBy) {
        return new ValidationResult(valid, confidenceScore, contradictions, exceptions, reviewedBy, LocalDateTime.now());
    }
}
