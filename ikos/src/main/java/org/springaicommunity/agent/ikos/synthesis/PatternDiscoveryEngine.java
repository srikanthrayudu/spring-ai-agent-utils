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

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;

import java.util.List;

/**
 * Automatically discovers candidate patterns from a corpus of observations.
 *
 * <p>Invoked by {@code KnowledgeEvolutionPipeline} after every new observation
 * is recorded. When enough observations share a common theme, a
 * {@link org.springaicommunity.agent.ikos.model.KnowledgeState#PATTERN_CANDIDATE}
 * unit is generated automatically — no human needs to propose it.
 *
 * @author Antigravity
 */
public interface PatternDiscoveryEngine {

    /**
     * Scan the provided observations and return any newly discovered
     * {@code PATTERN_CANDIDATE} units.
     *
     * @param observations all current observations in ApplicationMemory
     * @return list of auto-generated pattern candidates; empty if none found
     */
    List<KnowledgeUnit> discover(List<KnowledgeUnit> observations);
}
