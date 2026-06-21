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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.ikos.identity.DefaultIdentityCorrelationEngine;
import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.simulation.SimulatedDataGenerator;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link DefaultRiskDetectionEngine} — risk scanning across unified identities.
 */
@DisplayName("DefaultRiskDetectionEngine")
class DefaultRiskDetectionEngineTest {

    private DefaultRiskDetectionEngine riskEngine;
    private DefaultIdentityCorrelationEngine correlationEngine;

    @BeforeEach
    void setUp() {
        riskEngine = new DefaultRiskDetectionEngine();
        correlationEngine = new DefaultIdentityCorrelationEngine();
    }

    @Test
    @DisplayName("Detects risks from simulated identity data")
    void detectsRisksFromSimulatedData() {
        var data = new SimulatedDataGenerator().generate(100);
        List<UnifiedIdentity> identities = correlationEngine.correlate(data.accounts());
        List<KnowledgeUnit> risks = riskEngine.detectRisks(identities);

        assertThat(risks).isNotEmpty();
        // All detected units should be RISK_OBSERVATION type
        assertThat(risks).allSatisfy(risk ->
                assertThat(risk.type()).isEqualTo(KnowledgeType.RISK_OBSERVATION));
    }

    @Test
    @DisplayName("Returns empty list when no identities have risks")
    void emptyWhenNoRisks() {
        List<KnowledgeUnit> risks = riskEngine.detectRisks(List.of());
        assertThat(risks).isEmpty();
    }

    @Test
    @DisplayName("Risk units have valid IDs, statements, and confidence scores")
    void riskUnitsHaveRequiredFields() {
        var data = new SimulatedDataGenerator().generate(50);
        List<UnifiedIdentity> identities = correlationEngine.correlate(data.accounts());
        List<KnowledgeUnit> risks = riskEngine.detectRisks(identities);

        assertThat(risks).allSatisfy(risk -> {
            assertThat(risk.id()).isNotBlank();
            assertThat(risk.statement()).isNotBlank();
            assertThat(risk.confidence()).isBetween(0.0, 1.0);
        });
    }

}
