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

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keyword-based implementation of {@link ContradictionDetector}.
 *
 * <p>Detects simple negation contradictions by checking whether the candidate
 * statement and an existing unit's statement share the same key topic keywords
 * while using opposing terms (e.g. "always" vs "never", "use" vs "avoid").
 *
 * <p>This is a best-effort heuristic. For production use with critical knowledge,
 * replace with an LLM-backed semantic contradiction check.
 *
 * @author Antigravity
 */
public class KeywordContradictionDetector implements ContradictionDetector {

    private static final List<String[]> ANTONYM_PAIRS = List.of(
            new String[]{"always", "never"},
            new String[]{"use", "avoid"},
            new String[]{"enable", "disable"},
            new String[]{"increase", "decrease"},
            new String[]{"required", "optional"},
            new String[]{"must", "must not"},
            new String[]{"synchronous", "asynchronous"}
    );

    @Override
    public List<String> detectContradictions(KnowledgeUnit candidate, List<KnowledgeUnit> existingKnowledge) {
        List<String> contradictions = new ArrayList<>();
        String candidateText = candidate.statement().toLowerCase();
        Set<String> candidateKeywords = tokenize(candidateText);

        for (KnowledgeUnit existing : existingKnowledge) {
            if (existing.id().equals(candidate.id())) continue;
            String existingText = existing.statement().toLowerCase();
            Set<String> existingKeywords = tokenize(existingText);

            // Check for shared topic with opposing sentiment
            boolean topicOverlap = candidateKeywords.stream().anyMatch(existingKeywords::contains);
            if (topicOverlap) {
                for (String[] pair : ANTONYM_PAIRS) {
                    boolean candidateHasA = candidateText.contains(pair[0]);
                    boolean candidateHasB = candidateText.contains(pair[1]);
                    boolean existingHasA = existingText.contains(pair[0]);
                    boolean existingHasB = existingText.contains(pair[1]);

                    if ((candidateHasA && existingHasB) || (candidateHasB && existingHasA)) {
                        contradictions.add(String.format(
                                "Potential contradiction with [%s]: '%s' vs '%s'",
                                existing.id(), candidate.statement(), existing.statement()));
                        break;
                    }
                }
            }
        }
        return contradictions;
    }

    private Set<String> tokenize(String text) {
        return java.util.Arrays.stream(text.split("\\W+"))
                .filter(t -> t.length() > 3)
                .collect(Collectors.toSet());
    }
}
