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
package org.springaicommunity.agent.memory.validation;

import org.springaicommunity.agent.memory.model.Confidence;
import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.KnowledgeState;
import org.springaicommunity.agent.memory.model.ValidationResult;

import java.util.List;

/**
 * Default implementation of {@link KnowledgeValidator}.
 *
 * <p>Uses a {@link ContradictionDetector} to identify contradictions, then delegates
 * confidence computation to a {@link ConfidenceCalculator}. Marks the unit valid
 * when no contradictions are found.
 *
 * @author Antigravity
 */
public class DefaultKnowledgeValidator implements KnowledgeValidator {

    private final ContradictionDetector contradictionDetector;
    private final ConfidenceCalculator confidenceCalculator;

    public DefaultKnowledgeValidator(ContradictionDetector contradictionDetector,
                                     ConfidenceCalculator confidenceCalculator) {
        this.contradictionDetector = contradictionDetector;
        this.confidenceCalculator = confidenceCalculator;
    }

    @Override
    public ValidationResult validate(KnowledgeUnit candidate, List<KnowledgeUnit> existingKnowledge) {
        // Only validate units in states that should progress
        KnowledgeState state = candidate.getState();
        if (state == KnowledgeState.KNOWLEDGE
                || state == KnowledgeState.DEPRECATED
                || state == KnowledgeState.ARCHIVED) {
            return ValidationResult.passing(candidate.confidence());
        }

        // Detect contradictions
        List<String> contradictions = this.contradictionDetector.detectContradictions(candidate, existingKnowledge);
        boolean valid = contradictions.isEmpty();

        // Calculate composite confidence
        ValidationResult preliminary = valid
                ? ValidationResult.passing(0.0)
                : ValidationResult.failing(contradictions);
        double confidenceValue = this.confidenceCalculator.calculate(candidate, preliminary).value();

        return new ValidationResult(
                valid,
                confidenceValue,
                contradictions,
                List.of(),
                "system",
                java.time.LocalDateTime.now()
        );
    }
}
