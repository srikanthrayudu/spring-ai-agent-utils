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
import org.springaicommunity.agent.memory.model.KnowledgeState;
import org.springaicommunity.agent.memory.model.Outcome;
import org.springaicommunity.agent.memory.storage.MemoryStorage;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Updates a {@link KnowledgeUnit}'s confidence based on its recorded outcomes.
 *
 * <p>Implements the outcome-driven learning loop:
 * <pre>
 *   Outcome recorded → confidence recalculated → unit updated in storage
 * </pre>
 *
 * <p>If cumulative success rate falls below {@code 0.3}, the unit is
 * transitioned to {@code DEPRECATED} automatically.
 *
 * @author Antigravity
 */
public class OutcomeLearningEngine {

    private static final double DEPRECATION_THRESHOLD = 0.3;

    private final MemoryStorage storage;
    private final OutcomeTracker tracker;

    public OutcomeLearningEngine(MemoryStorage storage, OutcomeTracker tracker) {
        Assert.notNull(storage, "Storage must not be null");
        Assert.notNull(tracker, "OutcomeTracker must not be null");
        this.storage = storage;
        this.tracker = tracker;
    }

    /**
     * Record an outcome for a knowledge unit and update its confidence in storage.
     *
     * @param unitId  the ID of the knowledge unit that drove the decision
     * @param outcome the outcome of the action taken
     * @return the updated {@link KnowledgeUnit}, or empty if not found
     */
    public Optional<KnowledgeUnit> learn(String unitId, Outcome outcome) {
        Optional<KnowledgeUnit> optUnit = this.storage.getKnowledgeUnit(unitId);
        if (optUnit.isEmpty()) return Optional.empty();

        // Record the outcome
        this.tracker.record(unitId, outcome);

        KnowledgeUnit unit = optUnit.get();
        long successful = this.tracker.countSuccessful(unitId);
        long total      = this.tracker.countTotal(unitId);

        double outcomeScore = total > 0 ? (double) successful / total : 1.0;
        double newConfidence = Math.max(0.0, Math.min(1.0,
                unit.confidence() * outcomeScore));

        // Auto-deprecate when cumulative success drops below threshold
        KnowledgeState newState = (outcomeScore < DEPRECATION_THRESHOLD && unit.getState() == KnowledgeState.KNOWLEDGE)
                ? KnowledgeState.DEPRECATED
                : unit.getState();

        KnowledgeUnit updated = unit.toBuilder()
                .confidence(newConfidence)
                .state(newState)
                .outcome(outcome)
                .lastReviewed(LocalDateTime.now())
                .build();

        this.storage.saveKnowledgeUnit(updated);
        return Optional.of(updated);
    }
}
