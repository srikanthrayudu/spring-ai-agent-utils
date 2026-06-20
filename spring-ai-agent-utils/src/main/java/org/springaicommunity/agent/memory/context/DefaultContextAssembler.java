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
package org.springaicommunity.agent.memory.context;

import org.springaicommunity.agent.memory.ApplicationMemory;
import org.springaicommunity.agent.memory.GovernanceMemory;
import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Assembles a ranked {@link ContextPackage} from both {@link ApplicationMemory}
 * and {@link GovernanceMemory} using the 5-factor {@link ContextScorer}.
 *
 * <p>This is the primary integration point for {@code KnowledgeEvolutionAdvisor}.
 * It replaces the keyword-only scoring in {@code ContextBuilder} with a richer
 * multi-factor ranking.
 *
 * @author Antigravity
 */
public class DefaultContextAssembler implements ContextAssembler {

    private final ApplicationMemory applicationMemory;
    private final GovernanceMemory governanceMemory;
    private final ContextScorer scorer;

    private static final double MINIMUM_SCORE = 0.05;

    public DefaultContextAssembler(ApplicationMemory applicationMemory,
                                   GovernanceMemory governanceMemory,
                                   ContextScorer scorer) {
        Assert.notNull(applicationMemory, "ApplicationMemory must not be null");
        Assert.notNull(governanceMemory, "GovernanceMemory must not be null");
        Assert.notNull(scorer, "ContextScorer must not be null");
        this.applicationMemory = applicationMemory;
        this.governanceMemory = governanceMemory;
        this.scorer = scorer;
    }

    /** Convenience constructor using the default 5-factor scorer. */
    public DefaultContextAssembler(ApplicationMemory applicationMemory, GovernanceMemory governanceMemory) {
        this(applicationMemory, governanceMemory, new DefaultContextScorer());
    }

    @Override
    public ContextPackage assemble(String query, int limit) {
        Assert.hasText(query, "Query must not be blank");
        Assert.isTrue(limit > 0, "Limit must be positive");

        LocalDateTime now = LocalDateTime.now();

        List<KnowledgeUnit> corpus = gatherAll();

        List<ScoredUnit> scored = corpus.stream()
                .map(unit -> new ScoredUnit(unit, this.scorer.score(unit, query, now)))
                .filter(su -> su.score() > MINIMUM_SCORE)
                .sorted(Comparator.comparingDouble(ScoredUnit::score).reversed())
                .limit(limit)
                .toList();

        List<KnowledgeUnit> ranked = scored.stream().map(ScoredUnit::unit).toList();
        double overallRelevance = scored.stream()
                .mapToDouble(ScoredUnit::score)
                .average()
                .orElse(0.0);

        return new ContextPackage(query, ranked, overallRelevance);
    }

    private List<KnowledgeUnit> gatherAll() {
        List<KnowledgeUnit> all = new ArrayList<>();
        // Application Memory
        all.addAll(this.applicationMemory.getObservations());
        all.addAll(this.applicationMemory.getLocalPatterns());
        all.addAll(this.applicationMemory.getLocalOpinions());
        all.addAll(this.applicationMemory.getDecisions());
        all.addAll(this.applicationMemory.getIncidents());
        all.addAll(this.applicationMemory.getArtifacts());
        // Governance Memory
        all.addAll(this.governanceMemory.getEngineeringKnowledge());
        all.addAll(this.governanceMemory.getPrinciples());
        all.addAll(this.governanceMemory.getGlobalPatterns());
        all.addAll(this.governanceMemory.getGlobalOpinions());
        all.addAll(this.governanceMemory.getToolKnowledge());
        return all;
    }

    private record ScoredUnit(KnowledgeUnit unit, double score) {}
}
