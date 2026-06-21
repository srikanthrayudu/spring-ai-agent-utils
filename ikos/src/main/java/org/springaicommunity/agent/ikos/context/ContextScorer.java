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
package org.springaicommunity.agent.ikos.context;

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;

import java.time.LocalDateTime;

/**
 * Scores a {@link KnowledgeUnit} for inclusion in a {@link ContextPackage}.
 *
 * <p>The composite score is:
 * <pre>
 *   score = relevance × confidence × similarity × evidenceStrength × recency
 * </pre>
 *
 * <ul>
 *   <li><b>relevance</b>       — keyword match fraction against the query</li>
 *   <li><b>confidence</b>      — unit's composite confidence value</li>
 *   <li><b>similarity</b>      — structural similarity to query (default: 1.0 until embeddings)</li>
 *   <li><b>evidenceStrength</b> — avg strength of backing evidence items</li>
 *   <li><b>recency</b>         — exponential decay: exp(-λ × daysSince) [λ = 0.01]</li>
 * </ul>
 *
 * @author Antigravity
 */
public interface ContextScorer {

    /**
     * Score a unit for context inclusion.
     *
     * @param unit  the knowledge unit to score
     * @param query the agent's question or task description
     * @param now   current timestamp for recency computation
     * @return score in [0.0, 1.0]; units scoring below 0.05 should be excluded
     */
    double score(KnowledgeUnit unit, String query, LocalDateTime now);
}
