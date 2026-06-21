/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.bridge;

import org.springaicommunity.agent.ikos.Ikos;
import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.utils.AgentEnvironment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enriches the standard {@link AgentEnvironment} with IKOS-specific context
 * that gets injected into every LLM system prompt.
 *
 * <p>The agent sees not just OS/hostname info but also:
 * <ul>
 *   <li>Connected data source count</li>
 *   <li>Total identities being monitored</li>
 *   <li>Active risk and incident counts</li>
 *   <li>Audit trail status</li>
 *   <li>Knowledge store statistics</li>
 * </ul>
 *
 * @author Antigravity
 */
public class IkosEnvironmentProvider {

    private final Ikos ikos;

    public IkosEnvironmentProvider(Ikos ikos) {
        this.ikos = ikos;
    }

    /**
     * Returns enriched environment info combining standard AgentEnvironment
     * with IKOS platform context.
     */
    public String enrichedInfo() {
        return AgentEnvironment.info() + "\n\n" + ikosContext();
    }

    /**
     * Returns IKOS-specific context as a structured block for system prompts.
     */
    public String ikosContext() {
        List<KnowledgeUnit> all = ikos.storage().listKnowledgeUnits();
        long risks = all.stream().filter(u -> u.type() == KnowledgeType.RISK_OBSERVATION).count();
        long incidents = all.stream().filter(u -> u.type() == KnowledgeType.SECURITY_INCIDENT).count();
        long patterns = all.stream().filter(u -> u.type() == KnowledgeType.GLOBAL_PATTERN
                || u.type() == KnowledgeType.LOCAL_PATTERN).count();
        long remediations = all.stream().filter(u -> u.type() == KnowledgeType.REMEDIATION_ACTION).count();

        List<String> dataSources = ikos.dataAggregator() != null
                ? ikos.dataAggregator().registeredSources()
                : List.of("None configured");

        boolean auditEnabled = ikos.auditLogger() != null;
        int auditCount = auditEnabled ? ikos.auditLogger().recent(999).size() : 0;

        return """
                <ikos_platform_context>
                Platform: IKOS (Identity Knowledge Operating System) v0.11.0
                Timestamp: %s

                Data Sources: %s
                Connected Sources: %d

                Knowledge Store:
                  Total Units: %d
                  Risk Observations: %d
                  Security Incidents: %d
                  Patterns (local+global): %d
                  Remediations: %d

                Audit Trail: %s (%d entries)
                Deduplication Engine: Active (SHA-256 fingerprint)

                Available IKOS Tools (13 Governance + Agent Utils):
                  Identity Governance:
                  - AnalyzeIdentityRisks: Deep-dive into a specific identity
                  - DetectOffboardingGap: Check cross-platform offboarding status
                  - RecordSecurityIncident: Log an incident with full context
                  - RecordRemediationAction: Track remediation outcomes
                  - RecordAuditFinding: Record compliance findings
                  - RecommendRemediation: Generate policy-aligned remediation plans
                  - ListIdentityRisks: View all active risks
                  - ListAllIkosKnowledge: Browse the entire knowledge store
                  - ComplianceCheck: Map risks to NIST/GDPR/CIS/MITRE frameworks
                  - ComputeBlastRadius: Assess lateral movement risk for an identity
                  - QueryIdentityGraph: Explore cross-platform identity relationships
                  SOC Operations:
                  - ContainIdentity: Execute containment (DISABLE/REVOKE/ISOLATE/MONITOR)
                  - EscalateToSOC: Escalate to SOC Tier-3 with full context package
                </ikos_platform_context>
                """.formatted(
                LocalDateTime.now(),
                String.join(", ", dataSources),
                dataSources.size(),
                all.size(), risks, incidents, patterns, remediations,
                auditEnabled ? "Enabled" : "Disabled", auditCount);
    }

}
