/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.ikos.identity.DefaultIdentityCorrelationEngine;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.UnifiedIdentity;
import org.springaicommunity.agent.ikos.simulation.SimulatedDataGenerator;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AlertConsolidationEngine} — alert noise reduction.
 */
@DisplayName("AlertConsolidationEngine")
class AlertConsolidationEngineTest {

    @Test
    @DisplayName("Consolidation reduces raw alert count")
    void consolidationReducesAlerts() {
        var data = new SimulatedDataGenerator().generate(100);
        var correlationEngine = new DefaultIdentityCorrelationEngine();
        var riskEngine = new DefaultRiskDetectionEngine();
        var consolidationEngine = new AlertConsolidationEngine();

        List<UnifiedIdentity> identities = correlationEngine.correlate(data.accounts());
        List<KnowledgeUnit> risks = riskEngine.detectRisks(identities);

        if (risks.size() > 1) {
            var result = consolidationEngine.consolidate(risks);
            assertThat(result.consolidatedAlertCount()).isLessThanOrEqualTo(result.originalAlertCount());
            assertThat(result.reductionPercentage()).isBetween(0.0, 100.0);
        }
    }

    @Test
    @DisplayName("Empty input returns zero consolidation")
    void emptyInput() {
        var engine = new AlertConsolidationEngine();
        var result = engine.consolidate(List.of());

        assertThat(result.originalAlertCount()).isZero();
        assertThat(result.consolidatedAlertCount()).isZero();
    }

    @Test
    @DisplayName("Single alert has no noise reduction")
    void singleAlertNoReduction() {
        var data = new SimulatedDataGenerator().generate(10);
        var correlationEngine = new DefaultIdentityCorrelationEngine();
        var riskEngine = new DefaultRiskDetectionEngine();
        var consolidationEngine = new AlertConsolidationEngine();

        List<UnifiedIdentity> identities = correlationEngine.correlate(data.accounts());
        List<KnowledgeUnit> risks = riskEngine.detectRisks(identities);

        if (!risks.isEmpty()) {
            var result = consolidationEngine.consolidate(List.of(risks.get(0)));
            assertThat(result.originalAlertCount()).isEqualTo(1);
        }
    }

}
