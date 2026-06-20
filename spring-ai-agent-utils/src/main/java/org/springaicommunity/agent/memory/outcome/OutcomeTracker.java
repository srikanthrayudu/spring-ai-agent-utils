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
package org.springaicommunity.agent.memory.outcome;

import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.Outcome;

import java.util.List;

/**
 * Tracks {@link Outcome} records for specific {@link KnowledgeUnit} instances.
 *
 * <p>Outcomes are the real-world results of decisions and recommendations
 * driven by knowledge units. Tracking them closes the learning loop:
 * <pre>
 *   Decision → Action → Outcome → Confidence Update → Knowledge Update
 * </pre>
 *
 * @author Antigravity
 */
public interface OutcomeTracker {

    /**
     * Record an outcome for the given knowledge unit ID.
     *
     * @param knowledgeUnitId the ID of the unit that drove the decision
     * @param outcome         the outcome observed
     */
    void record(String knowledgeUnitId, Outcome outcome);

    /**
     * Retrieve all recorded outcomes for a specific knowledge unit.
     *
     * @param knowledgeUnitId the unit to query
     * @return list of outcomes in recording order; empty if none
     */
    List<Outcome> getOutcomes(String knowledgeUnitId);

    /**
     * Count successful outcomes for a specific knowledge unit.
     */
    long countSuccessful(String knowledgeUnitId);

    /**
     * Count total outcomes for a specific knowledge unit.
     */
    long countTotal(String knowledgeUnitId);
}
