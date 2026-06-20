/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory.risk;

import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.UnifiedIdentity;

import java.util.List;

/**
 * Converts privilege intelligence and identity data into security risk observations.
 *
 * <p>Implements the Risk Detection Engine layer from the IKOS architecture:
 * <pre>
 *   Unified Identity + Effective Privileges → Risk Observations
 * </pre>
 *
 * @author Antigravity
 */
public interface RiskDetectionEngine {

    /**
     * Analyzes a unified identity for all known risk patterns
     * and returns any detected risks as {@link KnowledgeUnit} observations.
     *
     * @param identity the correlated identity to analyze
     * @return list of risk observation KnowledgeUnits
     */
    List<KnowledgeUnit> detectRisks(UnifiedIdentity identity);

    /**
     * Analyzes a batch of identities for risks.
     *
     * @param identities all correlated identities
     * @return list of risk observations across all identities
     */
    List<KnowledgeUnit> detectRisks(List<UnifiedIdentity> identities);
}
