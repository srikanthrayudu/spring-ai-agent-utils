/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.ikos.model.IdentityAccount;
import org.springaicommunity.agent.ikos.model.UnifiedIdentity;
import org.springaicommunity.agent.ikos.simulation.SimulatedDataGenerator;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link DefaultIdentityCorrelationEngine} — cross-platform identity resolution.
 */
@DisplayName("DefaultIdentityCorrelationEngine")
class DefaultIdentityCorrelationEngineTest {

    private DefaultIdentityCorrelationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DefaultIdentityCorrelationEngine();
    }

    @Test
    @DisplayName("Correlates simulated accounts into unified identities")
    void correlatesSimulatedData() {
        var data = new SimulatedDataGenerator().generate(100);
        List<UnifiedIdentity> identities = engine.correlate(data.accounts());

        assertThat(identities).isNotEmpty();
        // Fewer unified identities than raw accounts (many-to-one correlation)
        assertThat(identities.size()).isLessThanOrEqualTo(data.accounts().size());
    }

    @Test
    @DisplayName("Each unified identity has a display name")
    void unifiedIdentitiesHaveDisplayName() {
        var data = new SimulatedDataGenerator().generate(50);
        List<UnifiedIdentity> identities = engine.correlate(data.accounts());

        assertThat(identities).allSatisfy(uid ->
                assertThat(uid.displayName()).isNotBlank());
    }

    @Test
    @DisplayName("Returns empty list for empty input")
    void emptyInput() {
        List<UnifiedIdentity> result = engine.correlate(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Unified identities contain their source accounts")
    void containsSourceAccounts() {
        var data = new SimulatedDataGenerator().generate(30);
        List<UnifiedIdentity> identities = engine.correlate(data.accounts());

        for (UnifiedIdentity uid : identities) {
            assertThat(uid.accounts()).isNotEmpty();
            for (IdentityAccount account : uid.accounts()) {
                assertThat(account.platform()).isNotBlank();
            }
        }
    }

}
