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

import org.springaicommunity.agent.ikos.model.AuditEvent;
import org.springaicommunity.agent.ikos.model.IdentityAccount;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregates identity data from multiple {@link IdentityDataSource} connectors.
 *
 * <p>Iterates all registered sources in priority order, merges results,
 * and provides a unified view for the correlation and risk engines.
 *
 * @author Antigravity
 */
public class IdentityDataAggregator {

    private final List<IdentityDataSource> sources;

    public IdentityDataAggregator(List<IdentityDataSource> sources) {
        this.sources = sources.stream()
                .sorted(Comparator.comparingInt(IdentityDataSource::priority))
                .toList();
    }

    /**
     * Fetch all accounts from all available data sources.
     */
    public List<IdentityAccount> fetchAllAccounts() {
        List<IdentityAccount> all = new ArrayList<>();
        for (IdentityDataSource source : sources) {
            if (source.isAvailable()) {
                try {
                    all.addAll(source.fetchAccounts());
                } catch (Exception e) {
                    System.err.println("[IKOS] Data source '" + source.platformName()
                            + "' failed: " + e.getMessage());
                }
            }
        }
        return all;
    }

    /**
     * Fetch all audit events from all available data sources.
     */
    public List<AuditEvent> fetchAllAuditEvents(Instant since) {
        List<AuditEvent> all = new ArrayList<>();
        for (IdentityDataSource source : sources) {
            if (source.isAvailable()) {
                try {
                    all.addAll(source.fetchAuditEvents(since));
                } catch (Exception e) {
                    System.err.println("[IKOS] Data source '" + source.platformName()
                            + "' audit fetch failed: " + e.getMessage());
                }
            }
        }
        return all;
    }

    /**
     * Returns the list of registered data source platform names.
     */
    public List<String> registeredSources() {
        return sources.stream()
                .map(s -> s.platformName() + (s.isAvailable() ? " ✓" : " ✗"))
                .toList();
    }

}
