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
import org.springaicommunity.agent.ikos.simulation.SimulatedDataGenerator;

import java.time.Instant;
import java.util.List;

/**
 * Built-in simulated data source for demos and testing.
 *
 * <p>Wraps {@link SimulatedDataGenerator} to implement the
 * {@link IdentityDataSource} SPI, allowing the simulation to
 * participate alongside real connectors.
 *
 * @author Antigravity
 */
public class SimulatedDataSource implements IdentityDataSource {

    private final SimulatedDataGenerator generator;
    private final int identityCount;
    private SimulatedDataGenerator.GeneratedData cachedData;

    public SimulatedDataSource(int identityCount) {
        this.generator = new SimulatedDataGenerator();
        this.identityCount = identityCount;
    }

    public SimulatedDataSource() {
        this(200);
    }

    @Override
    public String platformName() {
        return "Simulated";
    }

    @Override
    public String displayName() {
        return "Simulated Data (5 platforms)";
    }

    @Override
    public List<IdentityAccount> fetchAccounts() {
        return getData().accounts();
    }

    @Override
    public List<AuditEvent> fetchAuditEvents(Instant since) {
        return getData().auditEvents();
    }

    @Override
    public int priority() {
        return 999; // lowest priority — real sources come first
    }

    /**
     * Access the full generated dataset (accounts, events, offboarding, etc.).
     */
    public SimulatedDataGenerator.GeneratedData getData() {
        if (cachedData == null) {
            cachedData = generator.generate(identityCount);
        }
        return cachedData;
    }

}
