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

import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.ValidationResult;

import java.util.List;

/**
 * Validates a {@link KnowledgeUnit} against existing organizational knowledge.
 *
 * <p>Implementations check for contradictions, verify context applicability,
 * and calculate an initial confidence score. The result drives the state
 * transition from {@code PATTERN_CANDIDATE} → {@code VALIDATED_PATTERN}
 * (or back to {@code OBSERVATION} on failure).
 *
 * @author Antigravity
 */
public interface KnowledgeValidator {

    /**
     * Validate {@code candidate} against the provided corpus of existing knowledge.
     *
     * @param candidate the unit to validate
     * @param existingKnowledge the current knowledge corpus used for contradiction detection
     * @return a {@link ValidationResult} describing whether the unit is valid and its confidence
     */
    ValidationResult validate(KnowledgeUnit candidate, List<KnowledgeUnit> existingKnowledge);
}
