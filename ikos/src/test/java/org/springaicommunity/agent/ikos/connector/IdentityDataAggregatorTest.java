/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.connector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.ikos.model.IdentityAccount;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link IdentityDataAggregator} and {@link SimulatedDataSource}.
 */
@DisplayName("IdentityDataAggregator")
class IdentityDataAggregatorTest {

    @Test
    @DisplayName("Aggregates from simulated data source")
    void aggregatesSimulatedData() {
        var source = new SimulatedDataSource(50);
        var aggregator = new IdentityDataAggregator(List.of(source));

        List<IdentityAccount> accounts = aggregator.fetchAllAccounts();
        assertThat(accounts).isNotEmpty();
        assertThat(aggregator.registeredSources()).containsExactly("Simulated ✓");
    }

    @Test
    @DisplayName("Handles empty source list")
    void handlesEmptySources() {
        var aggregator = new IdentityDataAggregator(List.of());
        assertThat(aggregator.fetchAllAccounts()).isEmpty();
        assertThat(aggregator.registeredSources()).isEmpty();
    }

    @Test
    @DisplayName("Multiple sources are merged")
    void multipleSources() {
        var source1 = new SimulatedDataSource(20);
        var source2 = new SimulatedDataSource(30);
        var aggregator = new IdentityDataAggregator(List.of(source1, source2));

        List<IdentityAccount> accounts = aggregator.fetchAllAccounts();
        assertThat(accounts.size()).isGreaterThan(
                new SimulatedDataSource(20).fetchAccounts().size());
    }

    @Test
    @DisplayName("SimulatedDataSource provides audit events")
    void simulatedAuditEvents() {
        var source = new SimulatedDataSource(50);
        var events = source.fetchAuditEvents(Instant.now().minusSeconds(86400));
        assertThat(events).isNotEmpty();
    }

    @Test
    @DisplayName("SimulatedDataSource caches results")
    void cachesResults() {
        var source = new SimulatedDataSource(50);
        var first = source.fetchAccounts();
        var second = source.fetchAccounts();
        assertThat(first).isSameAs(second); // same object reference = cached
    }

}
