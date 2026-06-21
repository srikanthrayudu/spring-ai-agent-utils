/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.synthesis;

import org.springaicommunity.agent.ikos.model.Evidence;
import org.springaicommunity.agent.ikos.model.KnowledgeState;
import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Keyword-overlap implementation of {@link PatternDiscoveryEngine}.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Tokenise each observation statement into meaningful keywords (len &gt; 3).</li>
 *   <li>Build a keyword → observation index.</li>
 *   <li>For each keyword shared by {@code minObservations} or more observations,
 *       synthesise a {@code PATTERN_CANDIDATE} whose statement is derived from
 *       the most frequent shared terms.</li>
 *   <li>Each candidate's evidence list references the contributing observation IDs.</li>
 *   <li>Candidates with an ID already in {@code existingPatternIds} are skipped
 *       to avoid duplicates on repeated runs.</li>
 * </ol>
 *
 * <p>Replace with an LLM-backed implementation for richer semantic discovery.
 *
 * @author Antigravity
 */
public class KeywordPatternDiscoveryEngine implements PatternDiscoveryEngine {

    /** Minimum number of observations that must share a keyword to trigger a pattern. */
    private final int minObservations;

    /** Minimum keyword overlap ratio between two observations to be considered related. */
    private final double overlapThreshold;

    private final Set<String> existingPatternIds;

    public KeywordPatternDiscoveryEngine() {
        this(2, 0.25, new HashSet<>());
    }

    public KeywordPatternDiscoveryEngine(int minObservations, double overlapThreshold,
                                         Set<String> existingPatternIds) {
        this.minObservations   = minObservations;
        this.overlapThreshold  = overlapThreshold;
        this.existingPatternIds = existingPatternIds;
    }

    @Override
    public List<KnowledgeUnit> discover(List<KnowledgeUnit> observations) {
        if (observations == null || observations.size() < minObservations) {
            return List.of();
        }

        // Build keyword → [observation] index
        Map<String, List<KnowledgeUnit>> keywordIndex = new LinkedHashMap<>();
        for (KnowledgeUnit obs : observations) {
            for (String kw : tokenize(obs.statement())) {
                keywordIndex.computeIfAbsent(kw, k -> new ArrayList<>()).add(obs);
            }
        }

        List<KnowledgeUnit> discovered = new ArrayList<>();
        Set<String> usedObsGroups = new HashSet<>();

        // Find keywords shared by enough observations
        for (Map.Entry<String, List<KnowledgeUnit>> entry : keywordIndex.entrySet()) {
            String keyword = entry.getKey();
            List<KnowledgeUnit> relatedObs = entry.getValue();

            if (relatedObs.size() < minObservations) continue;

            // Deduplicate by grouping key
            String groupKey = relatedObs.stream()
                    .map(KnowledgeUnit::id)
                    .sorted()
                    .collect(Collectors.joining("+"));

            if (usedObsGroups.contains(groupKey)) continue;
            usedObsGroups.add(groupKey);

            // Generate a stable pattern ID from the group key hash
            String patternId = "AUTO-PAT-" + Math.abs(groupKey.hashCode() % 10000);
            if (existingPatternIds.contains(patternId)) continue;
            existingPatternIds.add(patternId);

            // Build a human-readable statement from shared keywords
            String statement = buildStatement(keyword, relatedObs);

            // Evidence = each contributing observation
            List<Evidence> evidence = relatedObs.stream()
                    .map(o -> new Evidence(o.id(), o.statement(), 0.6))
                    .collect(Collectors.toList());

            // Average confidence of backing observations
            double avgConfidence = relatedObs.stream()
                    .mapToDouble(KnowledgeUnit::confidence)
                    .average()
                    .orElse(0.3);

            // Use the context of the first observation as base context
            Object context = relatedObs.get(0).context();

            KnowledgeUnit pattern = KnowledgeUnit.builder()
                    .id(patternId)
                    .statement(statement)
                    .type(KnowledgeType.LOCAL_PATTERN)
                    .state(KnowledgeState.PATTERN_CANDIDATE)
                    .context(context)
                    .evidence(evidence)
                    .confidence(avgConfidence)
                    .lastReviewed(LocalDateTime.now())
                    .build();

            discovered.add(pattern);
        }

        return discovered;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String buildStatement(String triggerKeyword, List<KnowledgeUnit> relatedObs) {
        // Find all shared keywords across the group
        List<Set<String>> tokenSets = relatedObs.stream()
                .map(o -> tokenize(o.statement()))
                .collect(Collectors.toList());

        Set<String> shared = new HashSet<>(tokenSets.get(0));
        for (Set<String> ts : tokenSets) {
            shared.retainAll(ts);
        }

        String sharedPhrase = shared.isEmpty()
                ? triggerKeyword
                : String.join(", ", shared.stream().sorted().limit(3).toList());

        return String.format(
                "Pattern detected across %d observations involving: %s",
                relatedObs.size(), sharedPhrase);
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(t -> t.length() > 3)
                .collect(Collectors.toSet());
    }
}
