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

import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.KnowledgeType;

import java.util.List;
import java.util.Map;

/**
 * An assembled, ranked snapshot of memory retrieved for a specific agent query.
 *
 * <p>Partitions retrieved units by semantic role so the advisor can inject them
 * into structured prompt sections.
 *
 * @author Antigravity
 */
public class ContextPackage {

    private final String query;
    private final List<KnowledgeUnit> rankedUnits;
    private final double overallRelevance;

    public ContextPackage(String query, List<KnowledgeUnit> rankedUnits, double overallRelevance) {
        this.query = query;
        this.rankedUnits = List.copyOf(rankedUnits);
        this.overallRelevance = overallRelevance;
    }

    /** The original query used to assemble this package. */
    public String query() { return query; }

    /** All retrieved units, sorted descending by composite score. */
    public List<KnowledgeUnit> rankedUnits() { return rankedUnits; }

    /** Average composite score across all included units (0.0–1.0). */
    public double overallRelevance() { return overallRelevance; }

    /** Returns units belonging to Application Memory and IKOS operational types. */
    public List<KnowledgeUnit> operationalUnits() {
        return rankedUnits.stream()
                .filter(u -> u.type() == KnowledgeType.OBSERVATION
                        || u.type() == KnowledgeType.LOCAL_PATTERN
                        || u.type() == KnowledgeType.LOCAL_OPINION
                        || u.type() == KnowledgeType.DECISION
                        || u.type() == KnowledgeType.INCIDENT
                        || u.type() == KnowledgeType.ARTIFACT
                        || u.type() == KnowledgeType.RISK_OBSERVATION
                        || u.type() == KnowledgeType.SECURITY_INCIDENT
                        || u.type() == KnowledgeType.REMEDIATION_ACTION
                        || u.type() == KnowledgeType.AUDIT_FINDING)
                .toList();
    }

    /** Returns units belonging to Governance Memory types. */
    public List<KnowledgeUnit> governanceUnits() {
        return rankedUnits.stream()
                .filter(u -> u.type() == KnowledgeType.ENGINEERING_KNOWLEDGE
                        || u.type() == KnowledgeType.PRINCIPLE
                        || u.type() == KnowledgeType.GLOBAL_PATTERN
                        || u.type() == KnowledgeType.GLOBAL_OPINION
                        || u.type() == KnowledgeType.TOOL_KNOWLEDGE
                        || u.type() == KnowledgeType.RECOMMENDATION)
                .toList();
    }

    /** Returns {@code true} if the package has no units (nothing relevant found). */
    public boolean isEmpty() { return rankedUnits.isEmpty(); }
}
