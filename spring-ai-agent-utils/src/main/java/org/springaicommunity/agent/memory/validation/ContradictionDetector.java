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
package org.springaicommunity.agent.memory.validation;

import org.springaicommunity.agent.memory.model.KnowledgeUnit;

import java.util.List;

/**
 * Detects direct contradictions between a candidate {@link KnowledgeUnit} and
 * the existing knowledge corpus.
 *
 * <p>A contradiction occurs when two units make mutually exclusive claims about
 * the same subject or context. Detection is keyword/semantic and should be
 * extended with LLM-based analysis for high-value knowledge.
 *
 * @author Antigravity
 */
public interface ContradictionDetector {

    /**
     * Find contradictions between {@code candidate} and the existing corpus.
     *
     * @param candidate         the unit being evaluated
     * @param existingKnowledge the corpus to check against
     * @return list of human-readable contradiction descriptions; empty if none found
     */
    List<String> detectContradictions(KnowledgeUnit candidate, List<KnowledgeUnit> existingKnowledge);
}
