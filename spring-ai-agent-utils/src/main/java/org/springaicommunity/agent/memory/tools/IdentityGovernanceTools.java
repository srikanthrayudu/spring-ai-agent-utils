/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory.tools;

import org.springaicommunity.agent.memory.KnowledgeEvolutionPipeline;
import org.springaicommunity.agent.memory.identity.IdentityCorrelationEngine;
import org.springaicommunity.agent.memory.model.*;
import org.springaicommunity.agent.memory.risk.RiskDetectionEngine;
import org.springaicommunity.agent.memory.storage.MemoryStorage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring AI tools for agent interaction with the Identity Knowledge Operating System (IKOS).
 *
 * <p>Exposes identity governance capabilities as agent-callable tools:
 * risk detection, privilege analysis, incident recording, remediation tracking,
 * and knowledge evolution.
 *
 * @author Antigravity
 */
public class IdentityGovernanceTools {

    private final MemoryStorage storage;
    private final KnowledgeEvolutionPipeline pipeline;
    private final IdentityCorrelationEngine correlationEngine;
    private final RiskDetectionEngine riskEngine;

    public IdentityGovernanceTools(MemoryStorage storage,
                                   KnowledgeEvolutionPipeline pipeline,
                                   IdentityCorrelationEngine correlationEngine,
                                   RiskDetectionEngine riskEngine) {
        Assert.notNull(storage, "Storage must not be null");
        Assert.notNull(pipeline, "Pipeline must not be null");
        Assert.notNull(correlationEngine, "CorrelationEngine must not be null");
        Assert.notNull(riskEngine, "RiskEngine must not be null");
        this.storage = storage;
        this.pipeline = pipeline;
        this.correlationEngine = correlationEngine;
        this.riskEngine = riskEngine;
    }

    // ── Identity Analysis Tools ─────────────────────────────────────────────

    @Tool(name = "AnalyzeIdentityRisks",
          description = "Analyze a unified identity across all platforms and detect security risks " +
                        "(offboarding gaps, cross-platform admin, dormant admin, privilege creep, orphaned accounts).")
    public String analyzeIdentityRisks(
            @ToolParam(description = "Display name of the identity to analyze") String displayName) {

        // Search for the identity in stored risk observations
        List<KnowledgeUnit> existingRisks = storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION)
                .stream()
                .filter(u -> u.statement() != null && u.statement().contains(displayName))
                .toList();

        if (existingRisks.isEmpty()) {
            return "No risk observations found for identity: " + displayName +
                   ". Use DetectOffboardingGap or RecordIdentityEvent to generate observations.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══ IDENTITY RISK ANALYSIS: ").append(displayName).append(" ═══\n\n");
        sb.append("Risks detected: ").append(existingRisks.size()).append("\n\n");

        for (KnowledgeUnit risk : existingRisks) {
            sb.append("  [").append(risk.id()).append("] ");
            sb.append(risk.statement()).append("\n");
            sb.append("    Confidence: ").append(String.format("%.2f", risk.confidence())).append("\n");
            if (risk.context() != null) {
                sb.append("    Context: ").append(risk.context()).append("\n");
            }
            if (risk.evidence() != null) {
                sb.append("    Evidence: ").append(risk.evidence().size()).append(" item(s)\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    @Tool(name = "DetectOffboardingGap",
          description = "Detect offboarding gap: user disabled in one platform but still active in another. " +
                        "Creates a RISK_OBSERVATION in the knowledge store.")
    public String detectOffboardingGap(
            @ToolParam(description = "User display name") String displayName,
            @ToolParam(description = "Platform where user is DISABLED (e.g. ActiveDirectory)") String disabledPlatform,
            @ToolParam(description = "Platform where user is still ACTIVE (e.g. AWS_IAM)") String activePlatform,
            @ToolParam(description = "User's account ID on the active platform") String activeAccountId) {

        String statement = String.format(
                "OFFBOARDING GAP: %s is disabled on %s but still active on %s (account: %s)",
                displayName, disabledPlatform, activePlatform, activeAccountId);

        KnowledgeUnit risk = pipeline.createObservation(
                "RISK-OBG-" + System.currentTimeMillis() % 10000,
                statement,
                String.format("Identity: %s | Risk: OFFBOARDING_GAP (CRITICAL) | Policy: PAM-001, NIST AC-2",
                        displayName),
                disabledPlatform + " → " + activePlatform);

        return String.format(
                "⚠ Offboarding Gap Detected!\n" +
                "  Risk ID: %s\n" +
                "  Identity: %s\n" +
                "  Disabled: %s\n" +
                "  Still Active: %s (account: %s)\n" +
                "  Severity: CRITICAL\n" +
                "  Policies: PAM-001 (Offboarding Sync), NIST AC-2 (Account Management)\n" +
                "  Confidence: %.2f\n" +
                "  Recommended: Disable %s account immediately, revoke all active tokens.",
                risk.id(), displayName, disabledPlatform, activePlatform,
                activeAccountId, risk.confidence(), activePlatform);
    }

    @Tool(name = "RecordSecurityIncident",
          description = "Record a security incident (SoD violation, unauthorized access, token abuse, etc.) " +
                        "in the IKOS knowledge store.")
    public String recordSecurityIncident(
            @ToolParam(description = "Unique incident ID (e.g. INC-001)") String id,
            @ToolParam(description = "Incident description") String statement,
            @ToolParam(description = "Affected systems, scope, and timeline") String context,
            @ToolParam(description = "Source: alert system, log, or reporter") String source) {

        KnowledgeUnit incident = KnowledgeUnit.builder()
                .id(id)
                .statement(statement)
                .type(KnowledgeType.SECURITY_INCIDENT)
                .state(KnowledgeState.OBSERVATION)
                .context(context)
                .evidenceStrings(List.of(source))
                .confidence(1.0)
                .lastReviewed(LocalDateTime.now())
                .build();

        storage.saveKnowledgeUnit(incident);

        return String.format(
                "🔴 Security Incident Recorded\n" +
                "  ID: %s\n" +
                "  Statement: %s\n" +
                "  Context: %s\n" +
                "  Source: %s\n" +
                "  Auto-discovery triggered for related patterns.",
                id, statement, context, source);
    }

    @Tool(name = "RecordRemediationAction",
          description = "Record a remediation action taken in response to a risk or incident " +
                        "(e.g. 'Disabled AWS account', 'Revoked API tokens').")
    public String recordRemediationAction(
            @ToolParam(description = "Unique action ID (e.g. REM-001)") String id,
            @ToolParam(description = "Remediation action taken") String action,
            @ToolParam(description = "Related risk or incident ID") String relatedRiskId,
            @ToolParam(description = "Was the remediation successful? (true/false)") boolean successful) {

        KnowledgeUnit remediation = KnowledgeUnit.builder()
                .id(id)
                .statement(action)
                .type(KnowledgeType.REMEDIATION_ACTION)
                .state(KnowledgeState.OBSERVATION)
                .context("Related to: " + relatedRiskId)
                .evidenceStrings(List.of(relatedRiskId))
                .confidence(successful ? 0.9 : 0.4)
                .outcome(new Outcome(successful, action, LocalDateTime.now()))
                .lastReviewed(LocalDateTime.now())
                .build();

        storage.saveKnowledgeUnit(remediation);

        String status = successful ? "✅ Successful" : "⚠ Failed/Partial";
        return String.format(
                "%s Remediation Action Recorded\n" +
                "  ID: %s\n" +
                "  Action: %s\n" +
                "  Related Risk: %s\n" +
                "  Outcome: %s",
                status, id, action, relatedRiskId, status);
    }

    @Tool(name = "RecordAuditFinding",
          description = "Record an audit finding from an identity access review or compliance check.")
    public String recordAuditFinding(
            @ToolParam(description = "Unique finding ID (e.g. AUD-001)") String id,
            @ToolParam(description = "Audit finding description") String finding,
            @ToolParam(description = "Compliance framework and control (e.g. 'NIST AC-6')") String policyRef,
            @ToolParam(description = "Severity: LOW, MEDIUM, HIGH, CRITICAL") String severity) {

        KnowledgeUnit auditFinding = KnowledgeUnit.builder()
                .id(id)
                .statement(finding)
                .type(KnowledgeType.AUDIT_FINDING)
                .state(KnowledgeState.OBSERVATION)
                .context("Policy: " + policyRef + " | Severity: " + severity)
                .evidenceStrings(List.of(policyRef))
                .confidence(0.85)
                .lastReviewed(LocalDateTime.now())
                .build();

        storage.saveKnowledgeUnit(auditFinding);

        return String.format(
                "📋 Audit Finding Recorded\n" +
                "  ID: %s\n" +
                "  Finding: %s\n" +
                "  Policy: %s\n" +
                "  Severity: %s",
                id, finding, policyRef, severity);
    }

    @Tool(name = "RecommendRemediation",
          description = "Generate a remediation recommendation for a detected risk, " +
                        "including policy references, evidence, and expected risk reduction.")
    public String recommendRemediation(
            @ToolParam(description = "Risk observation ID to remediate") String riskId) {

        Optional<KnowledgeUnit> optRisk = storage.getKnowledgeUnit(riskId);
        if (optRisk.isEmpty()) {
            return "Risk observation not found: " + riskId;
        }

        KnowledgeUnit risk = optRisk.get();
        String recommendation = generateRecommendation(risk);

        // Store the recommendation as a knowledge unit
        String recId = "REC-" + riskId;
        KnowledgeUnit rec = KnowledgeUnit.builder()
                .id(recId)
                .statement(recommendation)
                .type(KnowledgeType.RECOMMENDATION)
                .state(KnowledgeState.RECOMMENDATION)
                .context("Generated from: " + riskId)
                .evidenceStrings(List.of(riskId))
                .confidence(risk.confidence())
                .lastReviewed(LocalDateTime.now())
                .build();

        storage.saveKnowledgeUnit(rec);

        // Find similar historical incidents
        List<KnowledgeUnit> similar = findSimilarIncidents(risk);

        StringBuilder sb = new StringBuilder();
        sb.append("═══ IKOS RECOMMENDATION ═══\n\n");
        sb.append("Risk: ").append(risk.statement()).append("\n\n");
        sb.append("Recommendation:\n  ").append(recommendation).append("\n\n");
        sb.append("Confidence: ").append(String.format("%.2f", risk.confidence())).append("\n");
        sb.append("Evidence: ").append(risk.evidence() != null ? risk.evidence().size() : 0).append(" item(s)\n");

        if (!similar.isEmpty()) {
            sb.append("\nSimilar Historical Incidents:\n");
            for (KnowledgeUnit s : similar) {
                sb.append("  - [").append(s.id()).append("] ").append(s.statement()).append("\n");
            }
        }

        sb.append("\n--- EXPLAINABILITY ---\n");
        sb.append("Why? ").append(risk.context()).append("\n");
        sb.append("Expected Risk Reduction: ").append(estimateRiskReduction(risk)).append("\n");

        return sb.toString();
    }

    @Tool(name = "ListIdentityRisks",
          description = "List all current identity risk observations in the IKOS knowledge store.")
    public String listIdentityRisks() {
        List<KnowledgeUnit> risks = storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION);
        List<KnowledgeUnit> incidents = storage.listKnowledgeUnitsByType(KnowledgeType.SECURITY_INCIDENT);

        if (risks.isEmpty() && incidents.isEmpty()) {
            return "No identity risks or security incidents recorded yet.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══ IKOS RISK DASHBOARD ═══\n\n");

        if (!risks.isEmpty()) {
            sb.append("RISK OBSERVATIONS (").append(risks.size()).append("):\n");
            for (KnowledgeUnit r : risks) {
                sb.append(String.format("  [%s] (Conf: %.2f) %s%n",
                        r.id(), r.confidence(), truncate(r.statement(), 80)));
            }
            sb.append("\n");
        }

        if (!incidents.isEmpty()) {
            sb.append("SECURITY INCIDENTS (").append(incidents.size()).append("):\n");
            for (KnowledgeUnit i : incidents) {
                sb.append(String.format("  [%s] (Conf: %.2f) %s%n",
                        i.id(), i.confidence(), truncate(i.statement(), 80)));
            }
        }

        return sb.toString();
    }

    @Tool(name = "ListAllIkosKnowledge",
          description = "List all knowledge units in the IKOS store including risks, incidents, patterns, and recommendations.")
    public String listAllKnowledge() {
        List<KnowledgeUnit> all = storage.listKnowledgeUnits();
        if (all.isEmpty()) {
            return "No knowledge units stored. Use identity governance tools to start building organizational security memory.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══ IKOS KNOWLEDGE STORE ═══\n\n");
        sb.append(String.format("  %-14s %-22s %-22s %-8s %s%n", "ID", "TYPE", "STATE", "CONF", "STATEMENT"));
        sb.append("  " + "─".repeat(100) + "\n");

        for (KnowledgeUnit u : all) {
            String state = u.getState() != null ? u.getState().name() : "—";
            sb.append(String.format("  %-14s %-22s %-22s %-8.2f %s%n",
                    u.id(), u.type(), state, u.confidence(),
                    truncate(u.statement(), 40)));
        }

        sb.append("  " + "─".repeat(100) + "\n");
        sb.append("  Total: ").append(all.size()).append(" knowledge units\n");

        return sb.toString();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String generateRecommendation(KnowledgeUnit risk) {
        String stmt = risk.statement() != null ? risk.statement().toUpperCase() : "";
        if (stmt.contains("OFFBOARDING")) {
            return "1. Immediately disable the active account on the remaining platform.\n" +
                   "  2. Revoke all active API tokens and session keys.\n" +
                   "  3. Review Okta/SSO assignments for remaining active sessions.\n" +
                   "  4. Update offboarding runbook to include cross-platform verification.";
        }
        if (stmt.contains("CROSS-PLATFORM ADMIN")) {
            return "1. Review admin necessity on each platform — apply Least Privilege.\n" +
                   "  2. Replace permanent admin with JIT (Just-In-Time) privileged access.\n" +
                   "  3. Schedule quarterly cross-platform privilege review.\n" +
                   "  4. Enable MFA on all admin-level accounts.";
        }
        if (stmt.contains("DORMANT ADMIN")) {
            return "1. Disable the dormant admin account immediately.\n" +
                   "  2. Rotate any credentials/keys associated with the account.\n" +
                   "  3. Assign a review owner for reactivation if needed.\n" +
                   "  4. Set up automated alerts for 60-day inactivity warning.";
        }
        if (stmt.contains("ORPHANED")) {
            return "1. Identify and assign an owner for the orphaned account.\n" +
                   "  2. If no owner can be found, disable the account within 48 hours.\n" +
                   "  3. Audit all access granted to the account.\n" +
                   "  4. Add ownership verification to the account provisioning workflow.";
        }
        if (stmt.contains("PRIVILEGE CREEP")) {
            return "1. Conduct a comprehensive access review for this identity.\n" +
                   "  2. Remove all permissions not required for current job function.\n" +
                   "  3. Implement role-based access control (RBAC) to prevent accumulation.\n" +
                   "  4. Schedule quarterly privilege right-sizing reviews.";
        }
        return "Review the risk and determine appropriate remediation steps.";
    }

    private List<KnowledgeUnit> findSimilarIncidents(KnowledgeUnit risk) {
        return storage.listKnowledgeUnits().stream()
                .filter(u -> u.type() == KnowledgeType.SECURITY_INCIDENT
                        || u.type() == KnowledgeType.RISK_OBSERVATION)
                .filter(u -> !u.id().equals(risk.id()))
                .filter(u -> hasKeywordOverlap(risk.statement(), u.statement()))
                .limit(5)
                .toList();
    }

    private boolean hasKeywordOverlap(String a, String b) {
        if (a == null || b == null) return false;
        String[] tokensA = a.toLowerCase().split("\\W+");
        String[] tokensB = b.toLowerCase().split("\\W+");
        for (String ta : tokensA) {
            if (ta.length() <= 3) continue;
            for (String tb : tokensB) {
                if (ta.equals(tb)) return true;
            }
        }
        return false;
    }

    private String estimateRiskReduction(KnowledgeUnit risk) {
        double confidence = risk.confidence();
        if (confidence >= 0.9) return "85-95% estimated risk reduction";
        if (confidence >= 0.7) return "65-80% estimated risk reduction";
        if (confidence >= 0.5) return "40-60% estimated risk reduction";
        return "20-40% estimated risk reduction (low confidence — gather more evidence)";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
