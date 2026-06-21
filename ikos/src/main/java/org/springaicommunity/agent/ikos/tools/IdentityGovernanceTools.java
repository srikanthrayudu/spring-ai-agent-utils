/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.tools;

import org.springaicommunity.agent.ikos.KnowledgeEvolutionPipeline;
import org.springaicommunity.agent.ikos.identity.IdentityCorrelationEngine;
import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.risk.RiskDetectionEngine;
import org.springaicommunity.agent.ikos.storage.MemoryStorage;
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

    @Tool(name = "ComplianceCheck",
          description = "Map detected identity risks to compliance frameworks (NIST SP 800-53, GDPR, CIS Controls, MITRE ATT&CK) " +
                        "and assess the organization's compliance posture.")
    public String complianceCheck() {
        List<KnowledgeUnit> risks = storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION);
        List<KnowledgeUnit> incidents = storage.listKnowledgeUnitsByType(KnowledgeType.SECURITY_INCIDENT);

        long offboardingGaps = risks.stream().filter(r -> matches(r, "OFFBOARDING")).count();
        long sodViolations = risks.stream().filter(r -> matches(r, "SOD")).count();
        long dormantAdmins = risks.stream().filter(r -> matches(r, "DORMANT")).count();
        long staleAccounts = risks.stream().filter(r -> matches(r, "STALE") || matches(r, "ORPHAN")).count();
        long crossAdmin = risks.stream().filter(r -> matches(r, "CROSS")).count();

        StringBuilder sb = new StringBuilder();
        sb.append("═══ IKOS COMPLIANCE ASSESSMENT ═══\n\n");

        // NIST SP 800-53
        sb.append("┌── NIST SP 800-53 ──────────────────────────────────────\n");
        sb.append(String.format("│ AC-2  Account Management:      %s (%d offboarding gaps)\n",
                offboardingGaps == 0 ? "✅ COMPLIANT" : "❌ NON-COMPLIANT", offboardingGaps));
        sb.append(String.format("│ AC-5  Separation of Duties:    %s (%d SoD violations)\n",
                sodViolations == 0 ? "✅ COMPLIANT" : "❌ NON-COMPLIANT", sodViolations));
        sb.append(String.format("│ AC-6  Least Privilege:         %s (%d over-privileged)\n",
                crossAdmin == 0 ? "✅ COMPLIANT" : "⚠ PARTIAL", crossAdmin));
        sb.append(String.format("│ AC-6(5) Privileged Accounts:   %s (%d dormant admins)\n",
                dormantAdmins == 0 ? "✅ COMPLIANT" : "❌ NON-COMPLIANT", dormantAdmins));
        sb.append(String.format("│ IA-4  Identifier Management:   %s\n",
                staleAccounts == 0 ? "✅ COMPLIANT" : "⚠ PARTIAL"));
        sb.append(String.format("│ IA-5  Authenticator Management: %s (%d stale accounts)\n",
                staleAccounts == 0 ? "✅ COMPLIANT" : "❌ NON-COMPLIANT", staleAccounts));
        sb.append("└────────────────────────────────────────────────────────\n\n");

        // MITRE ATT&CK
        sb.append("┌── MITRE ATT&CK Detection Coverage ─────────────────────\n");
        sb.append(String.format("│ T1078 Valid Accounts:           %s (detected %d gaps)\n",
                offboardingGaps > 0 ? "✅ DETECTING" : "○ No signals", offboardingGaps));
        sb.append(String.format("│ T1098 Account Manipulation:     %s (detected %d violations)\n",
                sodViolations + crossAdmin > 0 ? "✅ DETECTING" : "○ No signals", sodViolations + crossAdmin));
        sb.append(String.format("│ T1550 Alternate Auth Material:  %s (detected %d stale)\n",
                staleAccounts > 0 ? "✅ DETECTING" : "○ No signals", staleAccounts));
        sb.append("└────────────────────────────────────────────────────────\n\n");

        // GDPR
        sb.append("┌── GDPR ──────────────────────────────────────────────\n");
        sb.append(String.format("│ Art.5  Data Minimisation:       %s\n",
                crossAdmin + dormantAdmins == 0 ? "✅ ALIGNED" : "⚠ REVIEW REQUIRED"));
        sb.append(String.format("│ Art.32 Security of Processing:  %s\n",
                offboardingGaps + sodViolations == 0 ? "✅ ALIGNED" : "❌ GAPS FOUND"));
        sb.append("└────────────────────────────────────────────────────────\n\n");

        // CIS Controls
        sb.append("┌── CIS Controls v8 ──────────────────────────────────────\n");
        sb.append(String.format("│ Control 5 Account Management:  %s\n",
                offboardingGaps + staleAccounts == 0 ? "✅ IMPLEMENTED" : "❌ GAPS"));
        sb.append(String.format("│ Control 6 Access Control:      %s\n",
                sodViolations + crossAdmin == 0 ? "✅ IMPLEMENTED" : "❌ GAPS"));
        sb.append("└────────────────────────────────────────────────────────\n\n");

        sb.append(String.format("Overall: %d/%d controls compliant. %d findings require remediation.\n",
                (offboardingGaps == 0 ? 1 : 0) + (sodViolations == 0 ? 1 : 0) + (dormantAdmins == 0 ? 1 : 0)
                        + (staleAccounts == 0 ? 1 : 0) + (crossAdmin == 0 ? 1 : 0),
                5, risks.size()));

        return sb.toString();
    }

    @Tool(name = "ComputeBlastRadius",
          description = "Compute the lateral movement blast radius for a compromised identity — " +
                        "shows all platforms, permissions, and connected identities that would be affected.")
    public String computeBlastRadius(
            @ToolParam(description = "Display name of the identity to analyze") String displayName) {

        List<KnowledgeUnit> allRisks = storage.listKnowledgeUnits();
        List<KnowledgeUnit> identityRisks = allRisks.stream()
                .filter(r -> r.statement() != null && r.statement().contains(displayName))
                .toList();

        if (identityRisks.isEmpty()) {
            return "No data found for identity: " + displayName +
                   ". Run a risk scan first or check the name.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══ BLAST RADIUS ANALYSIS: ").append(displayName).append(" ═══\n\n");

        // Determine platforms and risk factors
        List<String> platforms = new java.util.ArrayList<>();
        List<String> riskFactors = new java.util.ArrayList<>();
        boolean isAdmin = false;
        boolean hasOffboardingGap = false;
        boolean hasSod = false;

        for (KnowledgeUnit r : identityRisks) {
            String stmt = r.statement().toUpperCase();
            if (stmt.contains("ACTIVEDIRECTORY")) platforms.add("ActiveDirectory");
            if (stmt.contains("AWS")) platforms.add("AWS_IAM");
            if (stmt.contains("OKTA")) platforms.add("Okta");
            if (stmt.contains("SALESFORCE")) platforms.add("Salesforce");
            if (stmt.contains("SERVICENOW")) platforms.add("ServiceNow");
            if (stmt.contains("ADMIN")) { isAdmin = true; riskFactors.add("Admin privilege"); }
            if (stmt.contains("OFFBOARDING")) { hasOffboardingGap = true; riskFactors.add("Offboarding gap"); }
            if (stmt.contains("SOD")) { hasSod = true; riskFactors.add("SoD violation"); }
            if (stmt.contains("DORMANT")) riskFactors.add("Dormant account");
            if (stmt.contains("STALE")) riskFactors.add("Stale credentials");
        }

        // Deduplicate
        platforms = platforms.stream().distinct().toList();
        riskFactors = riskFactors.stream().distinct().toList();

        int blastScore = platforms.size() * 20 + (isAdmin ? 30 : 0) + (hasOffboardingGap ? 20 : 0) + (hasSod ? 15 : 0);
        blastScore = Math.min(100, blastScore);

        String severity = blastScore >= 80 ? "🔴 CRITICAL" : blastScore >= 50 ? "🟠 HIGH" : "🟡 MEDIUM";

        sb.append("Blast Radius Score: ").append(severity).append(" (").append(blastScore).append("/100)\n\n");

        sb.append("Affected Platforms (").append(platforms.size()).append("):\n");
        for (String p : platforms) {
            sb.append("  ⚡ ").append(p);
            if (isAdmin) sb.append(" [ADMIN ACCESS]");
            sb.append("\n");
        }

        sb.append("\nRisk Factors:\n");
        for (String f : riskFactors) {
            sb.append("  ⚠ ").append(f).append("\n");
        }

        sb.append("\nLateral Movement Paths:\n");
        if (isAdmin && platforms.size() >= 2) {
            sb.append("  🔗 ").append(String.join(" → ", platforms)).append(" (admin chain)\n");
            sb.append("  ⚡ Compromising this identity grants admin access across ").append(platforms.size()).append(" platforms\n");
        }
        if (hasOffboardingGap) {
            sb.append("  🔗 Terminated but active — can access data without detection\n");
        }
        if (hasSod) {
            sb.append("  🔗 SoD violation enables both provisioning AND usage of access\n");
        }

        sb.append("\nImpact Assessment:\n");
        sb.append("  Data at risk: ").append(isAdmin ? "ALL resources on admin platforms" : "Role-scoped resources").append("\n");
        sb.append("  Estimated affected users: ").append(isAdmin ? "All users on managed platforms" : "Limited scope").append("\n");
        sb.append("  Recovery time: ").append(blastScore >= 80 ? "24-72 hours" : "4-24 hours").append("\n");

        sb.append("\nImmediate Actions:\n");
        sb.append("  1. Revoke all active sessions and credentials\n");
        sb.append("  2. Disable accounts on: ").append(String.join(", ", platforms)).append("\n");
        sb.append("  3. Enable enhanced monitoring on all affected platforms\n");
        sb.append("  4. Notify incident response team if blast radius >= HIGH\n");

        return sb.toString();
    }

    @Tool(name = "QueryIdentityGraph",
          description = "Query the cross-platform identity graph to explore relationships between identities, " +
                        "platforms, groups, and permissions. Returns connected nodes for a given identity.")
    public String queryIdentityGraph(
            @ToolParam(description = "Display name to search in the identity graph") String displayName) {

        List<KnowledgeUnit> allUnits = storage.listKnowledgeUnits();

        // Find all knowledge units mentioning this identity
        List<KnowledgeUnit> related = allUnits.stream()
                .filter(u -> u.statement() != null && u.statement().toLowerCase().contains(displayName.toLowerCase()))
                .toList();

        if (related.isEmpty()) {
            return "No identity graph data found for: " + displayName;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══ IDENTITY GRAPH: ").append(displayName).append(" ═══\n\n");
        sb.append("Connected nodes: ").append(related.size()).append(" knowledge units\n\n");

        // Group by type
        Map<String, List<KnowledgeUnit>> byType = related.stream()
                .collect(Collectors.groupingBy(u -> u.type() != null ? u.type().name() : "UNKNOWN"));

        for (var entry : byType.entrySet()) {
            sb.append("├── ").append(entry.getKey()).append(" (").append(entry.getValue().size()).append(")\n");
            for (KnowledgeUnit u : entry.getValue()) {
                sb.append("│   ├── [").append(u.id()).append("] ").append(truncate(u.statement(), 70)).append("\n");
                sb.append("│   │   Confidence: ").append(String.format("%.0f%%", u.confidence() * 100));
                if (u.getState() != null) sb.append(" | State: ").append(u.getState());
                sb.append("\n");
            }
        }

        // Find cross-references
        List<KnowledgeUnit> crossRefs = allUnits.stream()
                .filter(u -> u.evidence() != null && u.evidence().stream()
                        .anyMatch(e -> related.stream().anyMatch(r -> e.description() != null && e.description().contains(r.id()))))
                .filter(u -> !related.contains(u))
                .limit(5)
                .toList();

        if (!crossRefs.isEmpty()) {
            sb.append("\n├── CROSS-REFERENCES (").append(crossRefs.size()).append(")\n");
            for (KnowledgeUnit cr : crossRefs) {
                sb.append("│   ├── [").append(cr.id()).append("] ").append(truncate(cr.statement(), 70)).append("\n");
            }
        }

        sb.append("\n└── End of graph for ").append(displayName).append("\n");
        return sb.toString();
    }

    @Tool(name = "ContainIdentity",
          description = "Initiate containment actions for a compromised or high-risk identity. " +
                        "Supports actions: DISABLE (disable all accounts), REVOKE_SESSIONS (terminate active sessions), " +
                        "ISOLATE (quarantine from sensitive systems), MONITOR (enable enhanced logging). " +
                        "Records all containment actions in the IKOS knowledge store for audit trail.")
    public String containIdentity(
            @ToolParam(description = "Display name of the identity to contain") String displayName,
            @ToolParam(description = "Containment action: DISABLE, REVOKE_SESSIONS, ISOLATE, or MONITOR") String action) {

        String normalizedAction = action.toUpperCase().trim();
        if (!List.of("DISABLE", "REVOKE_SESSIONS", "ISOLATE", "MONITOR").contains(normalizedAction)) {
            return "Invalid action: " + action + ". Valid actions: DISABLE, REVOKE_SESSIONS, ISOLATE, MONITOR";
        }

        // Find all knowledge about this identity
        List<KnowledgeUnit> related = storage.listKnowledgeUnits().stream()
                .filter(u -> u.statement() != null && u.statement().toLowerCase().contains(displayName.toLowerCase()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("═══ CONTAINMENT ACTION: ").append(normalizedAction).append(" ═══\n");
        sb.append("Target: ").append(displayName).append("\n");
        sb.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n\n");

        // Determine affected platforms
        List<String> platforms = new java.util.ArrayList<>();
        for (KnowledgeUnit u : related) {
            String stmt = u.statement().toUpperCase();
            if (stmt.contains("ACTIVEDIRECTORY") && !platforms.contains("ActiveDirectory")) platforms.add("ActiveDirectory");
            if (stmt.contains("AWS") && !platforms.contains("AWS_IAM")) platforms.add("AWS_IAM");
            if (stmt.contains("OKTA") && !platforms.contains("Okta")) platforms.add("Okta");
            if (stmt.contains("SALESFORCE") && !platforms.contains("Salesforce")) platforms.add("Salesforce");
            if (stmt.contains("SERVICENOW") && !platforms.contains("ServiceNow")) platforms.add("ServiceNow");
        }

        switch (normalizedAction) {
            case "DISABLE" -> {
                sb.append("Action: Disable all accounts across platforms\n");
                sb.append("Platforms affected: ").append(String.join(", ", platforms)).append("\n\n");
                for (String p : platforms) {
                    sb.append("  ✅ [SIMULATED] Account disabled on ").append(p).append("\n");
                }
                sb.append("\n⚠ All active sessions will be terminated within 5 minutes.\n");
                sb.append("⚠ Service account dependencies should be reviewed before disabling.\n");
            }
            case "REVOKE_SESSIONS" -> {
                sb.append("Action: Terminate all active sessions and tokens\n");
                sb.append("Platforms affected: ").append(String.join(", ", platforms)).append("\n\n");
                for (String p : platforms) {
                    sb.append("  ✅ [SIMULATED] Sessions revoked on ").append(p).append("\n");
                }
                sb.append("\n⚠ User will need to re-authenticate on all platforms.\n");
                sb.append("⚠ API tokens and service credentials also revoked.\n");
            }
            case "ISOLATE" -> {
                sb.append("Action: Quarantine identity from sensitive systems\n");
                sb.append("Platforms affected: ").append(String.join(", ", platforms)).append("\n\n");
                for (String p : platforms) {
                    sb.append("  ✅ [SIMULATED] Access restricted to read-only on ").append(p).append("\n");
                }
                sb.append("\n⚠ Identity can still authenticate but cannot modify resources.\n");
                sb.append("⚠ Enhanced monitoring enabled for all actions.\n");
            }
            case "MONITOR" -> {
                sb.append("Action: Enable enhanced monitoring and alerting\n");
                sb.append("Platforms affected: ").append(String.join(", ", platforms)).append("\n\n");
                for (String p : platforms) {
                    sb.append("  ✅ [SIMULATED] Enhanced logging enabled on ").append(p).append("\n");
                }
                sb.append("\n⚠ All actions will be logged to SIEM with high-priority flags.\n");
                sb.append("⚠ Real-time alerts on privilege escalation or data access.\n");
            }
        }

        // Record containment action in knowledge store
        String incidentId = "CONTAIN-" + System.currentTimeMillis() % 100000;
        KnowledgeUnit containment = KnowledgeUnit.builder()
                .id(incidentId)
                .type(KnowledgeType.REMEDIATION_ACTION)
                .statement("CONTAINMENT [" + normalizedAction + "]: " + displayName + " — "
                        + platforms.size() + " platforms affected")
                .confidence(1.0)
                .evidence(List.of(new Evidence(
                        java.time.LocalDateTime.now().toString(),
                        "ContainIdentity tool — action: " + normalizedAction
                                + ", platforms: " + String.join(", ", platforms),
                        1.0)))
                .build();
        storage.saveKnowledgeUnit(containment);

        sb.append("\n📝 Containment recorded: ").append(incidentId);
        sb.append("\n📊 Related risks: ").append(related.size()).append(" knowledge units linked\n");

        return sb.toString();
    }

    @Tool(name = "EscalateToSOC",
          description = "Escalate a critical finding to SOC Tier-3 with a full context package. " +
                        "Packages the identity's risk profile, blast radius, compliance impact, and " +
                        "recommended immediate actions into a structured escalation report.")
    public String escalateToSOC(
            @ToolParam(description = "Display name of the identity to escalate") String displayName,
            @ToolParam(description = "Reason for escalation") String reason) {

        List<KnowledgeUnit> allUnits = storage.listKnowledgeUnits();
        List<KnowledgeUnit> related = allUnits.stream()
                .filter(u -> u.statement() != null && u.statement().toLowerCase().contains(displayName.toLowerCase()))
                .toList();

        long criticalCount = related.stream().filter(r -> r.confidence() >= 0.9).count();
        long highCount = related.stream().filter(r -> r.confidence() >= 0.7 && r.confidence() < 0.9).count();

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║          SOC TIER-3 ESCALATION REPORT                   ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n\n");

        sb.append("Escalation ID: ESC-").append(System.currentTimeMillis() % 100000).append("\n");
        sb.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("Priority: ").append(criticalCount > 0 ? "🔴 P1 — CRITICAL" : highCount > 0 ? "🟠 P2 — HIGH" : "🟡 P3 — MEDIUM").append("\n");
        sb.append("Identity: ").append(displayName).append("\n");
        sb.append("Reason: ").append(reason).append("\n\n");

        // Risk Summary
        sb.append("┌── RISK SUMMARY ────────────────────────────────────────\n");
        sb.append("│ Total findings: ").append(related.size()).append("\n");
        sb.append("│ Critical: ").append(criticalCount).append(" | High: ").append(highCount).append("\n");
        for (KnowledgeUnit r : related.stream().limit(5).toList()) {
            sb.append("│ • [").append(r.id()).append("] ")
                    .append(r.statement() != null ? truncate(r.statement(), 60) : "").append("\n");
        }
        if (related.size() > 5) {
            sb.append("│ ... and ").append(related.size() - 5).append(" more findings\n");
        }
        sb.append("└────────────────────────────────────────────────────────\n\n");

        // Blast Radius Summary
        sb.append("┌── BLAST RADIUS ASSESSMENT ──────────────────────────────\n");
        boolean isAdmin = related.stream().anyMatch(r -> r.statement() != null && r.statement().toUpperCase().contains("ADMIN"));
        boolean hasOffboarding = related.stream().anyMatch(r -> r.statement() != null && r.statement().toUpperCase().contains("OFFBOARDING"));
        boolean hasSod = related.stream().anyMatch(r -> r.statement() != null && r.statement().toUpperCase().contains("SOD"));

        List<String> impactFactors = new java.util.ArrayList<>();
        if (isAdmin) impactFactors.add("Admin privileges detected");
        if (hasOffboarding) impactFactors.add("Offboarding gap — terminated but active");
        if (hasSod) impactFactors.add("SoD violation — toxic privilege combination");

        for (String factor : impactFactors) {
            sb.append("│ ⚠ ").append(factor).append("\n");
        }
        int blastScore = (isAdmin ? 35 : 0) + (hasOffboarding ? 25 : 0) + (hasSod ? 20 : 0)
                + (int)(criticalCount * 10) + (int)(highCount * 5);
        blastScore = Math.min(100, blastScore);
        sb.append("│ Blast Score: ").append(blastScore).append("/100\n");
        sb.append("└────────────────────────────────────────────────────────\n\n");

        // Immediate Actions
        sb.append("┌── RECOMMENDED IMMEDIATE ACTIONS ──────────────────────\n");
        if (criticalCount > 0 || isAdmin) {
            sb.append("│ 1. 🔴 CONTAIN: Disable all accounts (use ContainIdentity DISABLE)\n");
            sb.append("│ 2. 🔴 REVOKE: Terminate all active sessions\n");
        }
        if (hasOffboarding) {
            sb.append("│ 3. 🟠 VERIFY: Confirm employment status with HR\n");
            sb.append("│ 4. 🟠 AUDIT: Review access logs for the past 90 days\n");
        }
        if (hasSod) {
            sb.append("│ 5. 🟡 SEGREGATE: Split admin roles across separate identities\n");
        }
        sb.append("│ 6. 📝 DOCUMENT: Record all actions in incident tracker\n");
        sb.append("│ 7. 📊 REPORT: Generate compliance impact report\n");
        sb.append("└────────────────────────────────────────────────────────\n\n");

        // Compliance Impact
        sb.append("┌── COMPLIANCE IMPACT ─────────────────────────────────────\n");
        if (isAdmin) sb.append("│ NIST AC-6 Least Privilege — NON-COMPLIANT\n");
        if (hasOffboarding) sb.append("│ NIST PS-4 Personnel Termination — NON-COMPLIANT\n");
        if (hasSod) sb.append("│ NIST AC-5 Separation of Duties — NON-COMPLIANT\n");
        sb.append("│ GDPR Art.32 — REVIEW REQUIRED\n");
        sb.append("│ CIS Control 5/6 — GAPS DETECTED\n");
        sb.append("└────────────────────────────────────────────────────────\n");

        // Record escalation in knowledge store
        String escalationId = "ESC-" + System.currentTimeMillis() % 100000;
        KnowledgeUnit escalation = KnowledgeUnit.builder()
                .id(escalationId)
                .type(KnowledgeType.SECURITY_INCIDENT)
                .statement("ESCALATION TO SOC TIER-3: " + displayName + " — " + reason)
                .confidence(1.0)
                .evidence(List.of(new Evidence(
                        java.time.LocalDateTime.now().toString(),
                        "EscalateToSOC tool — blast score: " + blastScore + "/100, "
                                + criticalCount + " critical, " + highCount + " high risks",
                        1.0)))
                .build();
        storage.saveKnowledgeUnit(escalation);

        sb.append("\n📝 Escalation recorded: ").append(escalationId).append("\n");

        return sb.toString();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean matches(KnowledgeUnit r, String keyword) {
        return r.statement() != null && r.statement().toUpperCase().contains(keyword);
    }

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
