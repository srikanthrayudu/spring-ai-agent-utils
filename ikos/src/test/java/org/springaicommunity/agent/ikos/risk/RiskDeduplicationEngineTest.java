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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link RiskDeduplicationEngine}.
 */
@DisplayName("RiskDeduplicationEngine")
class RiskDeduplicationEngineTest {

    @Test
    @DisplayName("Removes duplicate risks from repeated scans")
    void deduplicatesRepeatedScans() {
        var data = new SimulatedDataGenerator().generate(50);
        var corr = new DefaultIdentityCorrelationEngine();
        var risk = new DefaultRiskDetectionEngine();
        var dedup = new RiskDeduplicationEngine();

        List<UnifiedIdentity> identities = corr.correlate(data.accounts());

        // Run risk detection TWICE (simulates repeated scans)
        List<KnowledgeUnit> risks1 = risk.detectRisks(identities);
        List<KnowledgeUnit> risks2 = risk.detectRisks(identities);

        List<KnowledgeUnit> combined = new ArrayList<>(risks1);
        combined.addAll(risks2);

        var result = dedup.deduplicate(combined);

        // Should have fewer unique risks than combined (duplicates removed)
        assertThat(result.uniqueRisks().size()).isLessThan(combined.size());
        assertThat(result.duplicatesRemoved()).isGreaterThan(0);
        assertThat(result.reductionPercentage()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Empty input returns empty result")
    void emptyInput() {
        var dedup = new RiskDeduplicationEngine();
        var result = dedup.deduplicate(List.of());

        assertThat(result.uniqueRisks()).isEmpty();
        assertThat(result.originalCount()).isZero();
        assertThat(result.duplicatesRemoved()).isZero();
    }

    @Test
    @DisplayName("Single risk is not deduplicated")
    void singleRisk() {
        var data = new SimulatedDataGenerator().generate(10);
        var corr = new DefaultIdentityCorrelationEngine();
        var riskEngine = new DefaultRiskDetectionEngine();
        var dedup = new RiskDeduplicationEngine();

        List<KnowledgeUnit> risks = riskEngine.detectRisks(corr.correlate(data.accounts()));
        if (!risks.isEmpty()) {
            var result = dedup.deduplicate(List.of(risks.get(0)));
            assertThat(result.uniqueRisks()).hasSize(1);
            assertThat(result.duplicatesRemoved()).isZero();
        }
    }

    @Test
    @DisplayName("Fingerprints are deterministic")
    void deterministicFingerprints() {
        var data = new SimulatedDataGenerator().generate(20);
        var corr = new DefaultIdentityCorrelationEngine();
        var riskEngine = new DefaultRiskDetectionEngine();
        var dedup = new RiskDeduplicationEngine();

        List<KnowledgeUnit> risks = riskEngine.detectRisks(corr.correlate(data.accounts()));
        if (risks.size() >= 2) {
            String fp1 = dedup.computeFingerprint(risks.get(0));
            String fp2 = dedup.computeFingerprint(risks.get(0));
            assertThat(fp1).isEqualTo(fp2); // same input → same fingerprint
        }
    }

}
