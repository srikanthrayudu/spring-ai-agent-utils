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
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.ValidationResult;

/**
 * Computes the composite {@link Confidence} for a {@link KnowledgeUnit}.
 *
 * <p>The formula is:
 * <pre>
 *   Confidence = evidenceScore × consistencyScore × validationScore × outcomeScore
 * </pre>
 *
 * @author Antigravity
 */
public interface ConfidenceCalculator {

    /**
     * Calculate a full {@link Confidence} object for the given unit and its
     * associated {@link ValidationResult}.
     *
     * @param unit             the knowledge unit being evaluated
     * @param validationResult the result of the most recent validation pass
     * @return composite confidence broken down by all four factors
     */
    Confidence calculate(KnowledgeUnit unit, ValidationResult validationResult);
}
