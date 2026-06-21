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

import org.springaicommunity.agent.ikos.model.Evidence;
import org.springaicommunity.agent.ikos.model.KnowledgeState;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ContextScorer} using the 5-factor formula:
 * <pre>
 *   score = relevance × confidence × similarity × evidenceStrength × recency
 * </pre>
 *
 * <p>Units in {@code DEPRECATED} or {@code ARCHIVED} state always score {@code 0.0}.
 * Units with confidence below {@code 0.5} are clamped to {@code 0.0}.
 *
 * @author Antigravity
 */
public class DefaultContextScorer implements ContextScorer {

    /** Decay rate λ for recency: exp(-λ × days). 0.01 ≈ 70-day half-life. */
    private static final double DECAY_LAMBDA = 0.01;

    private static final double MINIMUM_CONFIDENCE = 0.5;
    private static final double MINIMUM_SCORE = 0.05;

    @Override
    public double score(KnowledgeUnit unit, String query, LocalDateTime now) {
        // Exclude terminal states
        KnowledgeState state = unit.getState();
        if (state == KnowledgeState.DEPRECATED || state == KnowledgeState.ARCHIVED) return 0.0;

        // Exclude low-confidence units
        if (unit.confidence() < MINIMUM_CONFIDENCE) return 0.0;

        double relevance       = computeRelevance(unit, query);
        double confidence      = unit.confidence();
        double evidenceStrength = computeEvidenceStrength(unit.evidence());
        double recency         = computeRecency(unit.getLastReviewed(), now);
        // similarity = 1.0 until embedding-based similarity is wired in
        double similarity      = 1.0;

        return relevance * confidence * similarity * evidenceStrength * recency;
    }

    // ── Factor computations ────────────────────────────────────────────────

    private double computeRelevance(KnowledgeUnit unit, String query) {
        if (query == null || query.isBlank()) return 0.0;
        Set<String> queryKeywords = tokenize(query);
        if (queryKeywords.isEmpty()) return 0.0;

        String searchable = (unit.statement() + " "
                + (unit.context() != null ? unit.context().toString() : "")).toLowerCase();

        long matches = queryKeywords.stream().filter(searchable::contains).count();
        return (double) matches / queryKeywords.size();
    }

    private double computeEvidenceStrength(List<Evidence> evidence) {
        if (evidence == null || evidence.isEmpty()) return 0.5; // neutral when no evidence
        return evidence.stream()
                .mapToDouble(Evidence::strength)
                .average()
                .orElse(0.5);
    }

    private double computeRecency(LocalDateTime lastReviewed, LocalDateTime now) {
        if (lastReviewed == null) return 0.5; // neutral when unknown
        long days = ChronoUnit.DAYS.between(lastReviewed, now);
        return Math.exp(-DECAY_LAMBDA * Math.max(0, days));
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(t -> t.length() > 3)
                .collect(Collectors.toSet());
    }
}
