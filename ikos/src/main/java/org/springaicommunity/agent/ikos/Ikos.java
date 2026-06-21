/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos;

import org.springaicommunity.agent.ikos.advisors.KnowledgeEvolutionAdvisor;
import org.springaicommunity.agent.ikos.audit.AuditLogger;
import org.springaicommunity.agent.ikos.audit.FileAuditLogger;
import org.springaicommunity.agent.ikos.connector.IdentityDataAggregator;
import org.springaicommunity.agent.ikos.connector.IdentityDataSource;
import org.springaicommunity.agent.ikos.connector.SimulatedDataSource;
import org.springaicommunity.agent.ikos.context.DefaultContextAssembler;
import org.springaicommunity.agent.ikos.identity.DefaultIdentityCorrelationEngine;
import org.springaicommunity.agent.ikos.identity.IdentityCorrelationEngine;
import org.springaicommunity.agent.ikos.outcome.InMemoryOutcomeTracker;
import org.springaicommunity.agent.ikos.outcome.OutcomeLearningEngine;
import org.springaicommunity.agent.ikos.outcome.OutcomeTracker;
import org.springaicommunity.agent.ikos.promotion.DefaultPromotionEngine;
import org.springaicommunity.agent.ikos.promotion.PromotionEngine;
import org.springaicommunity.agent.ikos.report.InteractiveDashboardGenerator;
import org.springaicommunity.agent.ikos.risk.AlertConsolidationEngine;
import org.springaicommunity.agent.ikos.risk.BehavioralAnalyzer;
import org.springaicommunity.agent.ikos.risk.DefaultRiskDetectionEngine;
import org.springaicommunity.agent.ikos.risk.RiskDeduplicationEngine;
import org.springaicommunity.agent.ikos.risk.RiskDetectionEngine;
import org.springaicommunity.agent.ikos.storage.FileMemoryStorage;
import org.springaicommunity.agent.ikos.storage.MemoryStorage;
import org.springaicommunity.agent.ikos.tools.EngineeringMemoryTools;
import org.springaicommunity.agent.ikos.tools.IdentityGovernanceTools;
import org.springframework.util.Assert;

import java.io.File;

/**
 * Central factory / builder for wiring all IKOS components.
 *
 * <p>Replaces ~20 manual {@code new} calls with a single fluent builder:
 *
 * <pre>{@code
 * Ikos ikos = Ikos.builder()
 *     .storagePath("/path/to/ikos")
 *     .build();
 *
 * // Access any component
 * ikos.governanceTools().listIdentityRisks();
 * ikos.pipeline().createObservation(...);
 * ikos.advisor();  // for ChatClient.defaultAdvisors(...)
 * }</pre>
 *
 * <p>All components use sensible defaults but each can be overridden
 * via the builder before calling {@code build()}.
 *
 * @author Antigravity
 */
public final class Ikos {

    private final MemoryStorage storage;
    private final ApplicationMemory appMemory;
    private final GovernanceMemory govMemory;
    private final KnowledgeEvolutionPipeline pipeline;
    private final PromotionEngine promotionEngine;
    private final OutcomeLearningEngine learningEngine;
    private final IdentityCorrelationEngine correlationEngine;
    private final RiskDetectionEngine riskEngine;
    private final IdentityGovernanceTools governanceTools;
    private final EngineeringMemoryTools engineeringTools;
    private final ContextBuilder contextBuilder;
    private final KnowledgeEvolutionAdvisor advisor;
    private final BehavioralAnalyzer behavioralAnalyzer;
    private final InteractiveDashboardGenerator dashboardGenerator;
    private final AlertConsolidationEngine consolidationEngine;
    // Enterprise components
    private final AuditLogger auditLogger;
    private final RiskDeduplicationEngine deduplicationEngine;
    private final IdentityDataAggregator dataAggregator;

    private Ikos(Builder b) {
        this.storage = b.storage;
        this.appMemory = b.appMemory;
        this.govMemory = b.govMemory;
        this.pipeline = b.pipeline;
        this.promotionEngine = b.promotionEngine;
        this.learningEngine = b.learningEngine;
        this.correlationEngine = b.correlationEngine;
        this.riskEngine = b.riskEngine;
        this.governanceTools = b.governanceTools;
        this.engineeringTools = b.engineeringTools;
        this.contextBuilder = b.contextBuilder;
        this.advisor = b.advisor;
        this.behavioralAnalyzer = b.behavioralAnalyzer;
        this.dashboardGenerator = b.dashboardGenerator;
        this.consolidationEngine = b.consolidationEngine;
        this.auditLogger = b.auditLogger;
        this.deduplicationEngine = b.deduplicationEngine;
        this.dataAggregator = b.dataAggregator;
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public MemoryStorage storage() { return storage; }
    public ApplicationMemory appMemory() { return appMemory; }
    public GovernanceMemory govMemory() { return govMemory; }
    public KnowledgeEvolutionPipeline pipeline() { return pipeline; }
    public PromotionEngine promotionEngine() { return promotionEngine; }
    public OutcomeLearningEngine learningEngine() { return learningEngine; }
    public IdentityCorrelationEngine correlationEngine() { return correlationEngine; }
    public RiskDetectionEngine riskEngine() { return riskEngine; }
    public IdentityGovernanceTools governanceTools() { return governanceTools; }
    public EngineeringMemoryTools engineeringTools() { return engineeringTools; }
    public ContextBuilder contextBuilder() { return contextBuilder; }
    public KnowledgeEvolutionAdvisor advisor() { return advisor; }
    public BehavioralAnalyzer behavioralAnalyzer() { return behavioralAnalyzer; }
    public InteractiveDashboardGenerator dashboardGenerator() { return dashboardGenerator; }
    public AlertConsolidationEngine consolidationEngine() { return consolidationEngine; }
    /** Enterprise: immutable audit trail for all operations. */
    public AuditLogger auditLogger() { return auditLogger; }
    /** Enterprise: fingerprint-based risk deduplication. */
    public RiskDeduplicationEngine deduplicationEngine() { return deduplicationEngine; }
    /** Enterprise: aggregated identity data from multiple connectors. */
    public IdentityDataAggregator dataAggregator() { return dataAggregator; }

    // ── Builder ──────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String storagePath;
        private MemoryStorage storage;
        private ApplicationMemory appMemory;
        private GovernanceMemory govMemory;
        private KnowledgeEvolutionPipeline pipeline;
        private PromotionEngine promotionEngine;
        private OutcomeTracker outcomeTracker;
        private OutcomeLearningEngine learningEngine;
        private IdentityCorrelationEngine correlationEngine;
        private RiskDetectionEngine riskEngine;
        private IdentityGovernanceTools governanceTools;
        private EngineeringMemoryTools engineeringTools;
        private ContextBuilder contextBuilder;
        private KnowledgeEvolutionAdvisor advisor;
        private BehavioralAnalyzer behavioralAnalyzer;
        private InteractiveDashboardGenerator dashboardGenerator;
        private AlertConsolidationEngine consolidationEngine;
        private AuditLogger auditLogger;
        private RiskDeduplicationEngine deduplicationEngine;
        private IdentityDataAggregator dataAggregator;
        private java.util.List<IdentityDataSource> dataSources;
        private int advisorMaxUnits = 10;
        private int advisorOrder = 100;
        private boolean auditEnabled = true;

        private Builder() {}

        /** Required — root directory for IKOS knowledge persistence. */
        public Builder storagePath(String storagePath) {
            this.storagePath = storagePath;
            return this;
        }

        /** Override the default {@link FileMemoryStorage}. */
        public Builder storage(MemoryStorage storage) {
            this.storage = storage;
            return this;
        }

        /** Override the default identity correlation engine. */
        public Builder correlationEngine(IdentityCorrelationEngine correlationEngine) {
            this.correlationEngine = correlationEngine;
            return this;
        }

        /** Override the default risk detection engine. */
        public Builder riskEngine(RiskDetectionEngine riskEngine) {
            this.riskEngine = riskEngine;
            return this;
        }

        /** Override the default outcome tracker. */
        public Builder outcomeTracker(OutcomeTracker outcomeTracker) {
            this.outcomeTracker = outcomeTracker;
            return this;
        }

        /** Enterprise: override the default file-based audit logger. */
        public Builder auditLogger(AuditLogger auditLogger) {
            this.auditLogger = auditLogger;
            return this;
        }

        /** Enterprise: enable/disable audit trail (default: true). */
        public Builder auditEnabled(boolean enabled) {
            this.auditEnabled = enabled;
            return this;
        }

        /** Enterprise: override the default deduplication engine. */
        public Builder deduplicationEngine(RiskDeduplicationEngine engine) {
            this.deduplicationEngine = engine;
            return this;
        }

        /** Enterprise: register identity data source connectors. */
        public Builder dataSources(java.util.List<IdentityDataSource> sources) {
            this.dataSources = sources;
            return this;
        }

        /** Enterprise: override the default data aggregator. */
        public Builder dataAggregator(IdentityDataAggregator aggregator) {
            this.dataAggregator = aggregator;
            return this;
        }

        /** Configure how many knowledge units the advisor injects per query. */
        public Builder advisorMaxUnits(int maxUnits) {
            this.advisorMaxUnits = maxUnits;
            return this;
        }

        /** Set the advisor order in the advisor chain. */
        public Builder advisorOrder(int order) {
            this.advisorOrder = order;
            return this;
        }

        /**
         * Builds the full IKOS component graph with defaults for anything not set.
         *
         * <p>Requires either {@code storagePath} or a custom {@code storage}.
         */
        public Ikos build() {
            // ── Storage ──────────────────────────────────────────────────
            if (this.storage == null) {
                Assert.hasText(this.storagePath, "Either storagePath or storage must be set");
                new File(this.storagePath).mkdirs();
                this.storage = new FileMemoryStorage(this.storagePath);
            }

            // ── Memory layers ────────────────────────────────────────────
            if (this.appMemory == null) {
                this.appMemory = new ApplicationMemory(this.storage);
            }
            if (this.govMemory == null) {
                this.govMemory = new GovernanceMemory(this.storage);
            }

            // ── Pipeline ─────────────────────────────────────────────────
            if (this.pipeline == null) {
                this.pipeline = new KnowledgeEvolutionPipeline(this.storage);
            }

            // ── Promotion ────────────────────────────────────────────────
            if (this.promotionEngine == null) {
                this.promotionEngine = new DefaultPromotionEngine(this.storage);
            }

            // ── Outcome learning ─────────────────────────────────────────
            if (this.outcomeTracker == null) {
                this.outcomeTracker = new InMemoryOutcomeTracker();
            }
            if (this.learningEngine == null) {
                this.learningEngine = new OutcomeLearningEngine(this.storage, this.outcomeTracker);
            }

            // ── Engines ──────────────────────────────────────────────────
            if (this.correlationEngine == null) {
                this.correlationEngine = new DefaultIdentityCorrelationEngine();
            }
            if (this.riskEngine == null) {
                this.riskEngine = new DefaultRiskDetectionEngine();
            }

            // ── Tools ────────────────────────────────────────────────────
            if (this.governanceTools == null) {
                this.governanceTools = new IdentityGovernanceTools(
                        this.storage, this.pipeline, this.correlationEngine, this.riskEngine);
            }
            if (this.engineeringTools == null) {
                this.engineeringTools = new EngineeringMemoryTools(
                        this.storage, this.pipeline, this.appMemory, this.govMemory);
            }

            // ── Context ──────────────────────────────────────────────────
            if (this.contextBuilder == null) {
                this.contextBuilder = new ContextBuilder(this.appMemory, this.govMemory);
            }

            // ── Advisor (requires Spring AI on classpath) ────────────────
            if (this.advisor == null) {
                try {
                    this.advisor = KnowledgeEvolutionAdvisor.builder()
                            .contextBuilder(this.contextBuilder)
                            .maxRetrievedUnits(this.advisorMaxUnits)
                            .order(this.advisorOrder)
                            .build();
                } catch (NoClassDefFoundError e) {
                    // Spring AI not on classpath — advisor is optional for non-LLM demos
                    this.advisor = null;
                }
            }

            // ── Analytics ────────────────────────────────────────────────
            if (this.behavioralAnalyzer == null) {
                this.behavioralAnalyzer = new BehavioralAnalyzer();
            }
            if (this.dashboardGenerator == null) {
                this.dashboardGenerator = new InteractiveDashboardGenerator();
            }
            if (this.consolidationEngine == null) {
                this.consolidationEngine = new AlertConsolidationEngine();
            }

            // ── Enterprise: Audit Trail ──────────────────────────────────
            if (this.auditLogger == null && this.auditEnabled && this.storagePath != null) {
                this.auditLogger = new FileAuditLogger(this.storagePath);
            }

            // ── Enterprise: Deduplication ────────────────────────────────
            if (this.deduplicationEngine == null) {
                this.deduplicationEngine = new RiskDeduplicationEngine();
            }

            // ── Enterprise: Data Aggregator ──────────────────────────────
            if (this.dataAggregator == null) {
                java.util.List<IdentityDataSource> sources = this.dataSources != null
                        ? this.dataSources
                        : java.util.List.of(new SimulatedDataSource());
                this.dataAggregator = new IdentityDataAggregator(sources);
            }

            return new Ikos(this);
        }
    }

}
