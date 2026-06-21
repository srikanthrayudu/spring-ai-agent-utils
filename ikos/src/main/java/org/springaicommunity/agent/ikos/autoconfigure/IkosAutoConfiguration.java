/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.autoconfigure;

import org.springaicommunity.agent.ikos.Ikos;
import org.springaicommunity.agent.ikos.audit.AuditLogger;
import org.springaicommunity.agent.ikos.audit.FileAuditLogger;
import org.springaicommunity.agent.ikos.config.IkosProperties;
import org.springaicommunity.agent.ikos.connector.IdentityDataAggregator;
import org.springaicommunity.agent.ikos.connector.IdentityDataSource;
import org.springaicommunity.agent.ikos.connector.SimulatedDataSource;
import org.springaicommunity.agent.ikos.risk.RiskDeduplicationEngine;
import org.springaicommunity.agent.ikos.web.IkosRestController;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring Boot auto-configuration for IKOS.
 *
 * <p>Automatically wires all IKOS components when the application includes
 * the {@code ikos} module on its classpath.
 *
 * <p>Configuration via {@code application.yml}:
 * <pre>
 * ikos:
 *   storage-path: /var/ikos
 *   policies:
 *     dormant-admin-threshold-days: 90
 *     cross-platform-admin-min-platforms: 3
 *   advisor:
 *     max-units: 10
 *     order: 100
 *   audit:
 *     enabled: true
 *   api:
 *     enabled: true
 *     base-path: /api/ikos
 * </pre>
 *
 * @author Antigravity
 */
@AutoConfiguration
@ConditionalOnClass(Ikos.class)
public class IkosAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "ikos")
    @ConditionalOnMissingBean
    IkosProperties ikosProperties() {
        return new IkosProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    Ikos ikos(IkosProperties properties) {
        return Ikos.builder()
                .storagePath(properties.getStoragePath())
                .advisorMaxUnits(properties.getAdvisor().getMaxUnits())
                .advisorOrder(properties.getAdvisor().getOrder())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "ikos.audit.enabled", havingValue = "true", matchIfMissing = true)
    AuditLogger ikosAuditLogger(IkosProperties properties) {
        return new FileAuditLogger(properties.getStoragePath());
    }

    @Bean
    @ConditionalOnMissingBean
    RiskDeduplicationEngine riskDeduplicationEngine() {
        return new RiskDeduplicationEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    IdentityDataAggregator identityDataAggregator(
            ObjectProvider<List<IdentityDataSource>> sourcesProvider) {
        List<IdentityDataSource> sources = sourcesProvider.getIfAvailable(List::of);
        if (sources.isEmpty()) {
            // Fallback to simulated data for demos
            sources = List.of(new SimulatedDataSource());
        }
        return new IdentityDataAggregator(sources);
    }

    // ── Web Layer (only if Spring Web is on classpath) ───────────────────

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication
    @ConditionalOnProperty(name = "ikos.api.enabled", havingValue = "true", matchIfMissing = true)
    static class IkosWebConfiguration {

        @Bean
        @ConditionalOnMissingBean
        IkosRestController ikosRestController(Ikos ikos, AuditLogger auditLogger) {
            return new IkosRestController(ikos, auditLogger);
        }

    }

}
