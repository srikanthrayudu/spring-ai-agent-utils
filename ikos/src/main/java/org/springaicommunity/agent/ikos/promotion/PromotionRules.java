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
package org.springaicommunity.agent.ikos.promotion;

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.ValidationResult;

/**
 * Declarative promotion rules evaluated before {@link PromotionEngine#nominate}.
 *
 * <p>Implementations define the minimum thresholds a {@link KnowledgeUnit} must
 * satisfy before it can enter the human-review queue.
 *
 * @author Antigravity
 */
public interface PromotionRules {

    /** Minimum number of distinct evidence items required. */
    default int minimumEvidenceCount() { return 2; }

    /** Minimum confidence value required (0.0–1.0). */
    default double minimumConfidence() { return 0.5; }

    /**
     * Returns {@code true} if the unit has enough evidence items.
     */
    default boolean meetsEvidenceThreshold(KnowledgeUnit unit) {
        int count = unit.evidence() == null ? 0 : unit.evidence().size();
        return count >= minimumEvidenceCount();
    }

    /**
     * Returns {@code true} if the unit's confidence meets the minimum threshold.
     */
    default boolean meetsConfidenceThreshold(KnowledgeUnit unit) {
        return unit.confidence() >= minimumConfidence();
    }

    /**
     * Returns {@code true} if the unit passes all promotion gates.
     */
    default boolean eligible(KnowledgeUnit unit, ValidationResult validationResult) {
        return validationResult.valid()
                && meetsEvidenceThreshold(unit)
                && meetsConfidenceThreshold(unit);
    }
}
