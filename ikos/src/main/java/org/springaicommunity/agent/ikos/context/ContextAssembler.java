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

/**
 * Assembles a ranked {@link ContextPackage} for an agent query.
 *
 * <p>Retrieves units from both {@code ApplicationMemory} and {@code GovernanceMemory},
 * scores each unit with a {@link ContextScorer}, applies a minimum-score filter,
 * and returns the top-{@code limit} results sorted by descending composite score.
 *
 * @author Antigravity
 */
public interface ContextAssembler {

    /**
     * Assemble a {@link ContextPackage} for the given query.
     *
     * @param query the agent's question or task description
     * @param limit maximum number of units to include
     * @return ranked, filtered context package ready for prompt injection
     */
    ContextPackage assemble(String query, int limit);
}
