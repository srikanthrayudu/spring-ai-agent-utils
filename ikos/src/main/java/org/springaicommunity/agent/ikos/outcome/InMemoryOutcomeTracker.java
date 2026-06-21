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
package org.springaicommunity.agent.ikos.outcome;

import org.springaicommunity.agent.ikos.model.Outcome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link OutcomeTracker}.
 *
 * <p>Stores outcomes keyed by knowledge unit ID. Replace with a persistent
 * implementation (e.g. backed by {@code FileMemoryStorage}) for production use.
 *
 * @author Antigravity
 */
public class InMemoryOutcomeTracker implements OutcomeTracker {

    private final Map<String, List<Outcome>> outcomeMap = new ConcurrentHashMap<>();

    @Override
    public void record(String knowledgeUnitId, Outcome outcome) {
        this.outcomeMap.computeIfAbsent(knowledgeUnitId, k -> new ArrayList<>()).add(outcome);
    }

    @Override
    public List<Outcome> getOutcomes(String knowledgeUnitId) {
        return List.copyOf(this.outcomeMap.getOrDefault(knowledgeUnitId, List.of()));
    }

    @Override
    public long countSuccessful(String knowledgeUnitId) {
        return getOutcomes(knowledgeUnitId).stream()
                .filter(Outcome::successful)
                .count();
    }

    @Override
    public long countTotal(String knowledgeUnitId) {
        return getOutcomes(knowledgeUnitId).size();
    }
}
