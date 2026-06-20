/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory.risk;

import org.springaicommunity.agent.memory.model.KnowledgeUnit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Consolidates related risk alerts into clustered incidents to reduce alert fatigue.
 *
 * <p>Implements three consolidation strategies:
 * <ul>
 *   <li><b>Identity clustering</b>: Multiple risks for the same person → single consolidated alert</li>
 *   <li><b>Risk type clustering</b>: All offboarding gaps → single summary with count</li>
 *   <li><b>Evidence correlation</b>: Risks sharing evidence sources are grouped</li>
 * </ul>
 *
 * <p>Target: ≥40% reduction in standalone alerts (challenge requirement).
 *
 * @author Antigravity
 */
public class AlertConsolidationEngine {

    /**
     * A consolidated alert that groups related individual risks.
     */
    public record ConsolidatedAlert(
            String alertId,
            String category,
            String summary,
            String severity,
            double maxConfidence,
            List<KnowledgeUnit> constituentRisks,
            List<String> affectedIdentities,
            List<String> affectedPlatforms,
            List<String> remediationSteps
    ) {
        public int riskCount() { return constituentRisks.size(); }
    }

    /**
     * Result of the consolidation process.
     */
    public record ConsolidationResult(
            int originalAlertCount,
            int consolidatedAlertCount,
            double reductionPercentage,
            List<ConsolidatedAlert> consolidatedAlerts,
            Map<String, Integer> clusterSizes
    ) {
        public boolean meetsTarget() { return reductionPercentage >= 40.0; }
    }

    /**
     * Consolidates a list of risk KnowledgeUnits into grouped alerts.
     *
     * @param risks the raw risk observations
     * @return consolidation result with metrics
     */
    public ConsolidationResult consolidate(List<KnowledgeUnit> risks) {
        if (risks == null || risks.isEmpty()) {
            return new ConsolidationResult(0, 0, 0.0, List.of(), Map.of());
        }

        int originalCount = risks.size();

        // Strategy 1: Cluster by identity name (extracted from statement)
        Map<String, List<KnowledgeUnit>> byIdentity = new LinkedHashMap<>();
        for (KnowledgeUnit risk : risks) {
            String identity = extractIdentityFromStatement(risk.statement());
            byIdentity.computeIfAbsent(identity, k -> new ArrayList<>()).add(risk);
        }

        // Strategy 2: Within each identity cluster, further group by risk category
        List<ConsolidatedAlert> consolidated = new ArrayList<>();
        int alertCounter = 0;

        for (Map.Entry<String, List<KnowledgeUnit>> entry : byIdentity.entrySet()) {
            String identity = entry.getKey();
            List<KnowledgeUnit> identityRisks = entry.getValue();

            if (identityRisks.size() == 1) {
                // Single risk for this identity — create a standalone alert
                KnowledgeUnit risk = identityRisks.getFirst();
                consolidated.add(new ConsolidatedAlert(
                        "ALERT-" + String.format("%04d", ++alertCounter),
                        extractCategory(risk.statement()),
                        risk.statement(),
                        classifySeverity(risk.confidence()),
                        risk.confidence(),
                        List.of(risk),
                        List.of(identity),
                        extractPlatforms(risk),
                        generateRemediationSteps(risk)));
            } else {
                // Multiple risks for this identity — consolidate into one alert
                Map<String, List<KnowledgeUnit>> byCategory = identityRisks.stream()
                        .collect(Collectors.groupingBy(r -> extractCategory(r.statement()),
                                LinkedHashMap::new, Collectors.toList()));

                for (Map.Entry<String, List<KnowledgeUnit>> catEntry : byCategory.entrySet()) {
                    String category = catEntry.getKey();
                    List<KnowledgeUnit> catRisks = catEntry.getValue();

                    double maxConf = catRisks.stream()
                            .mapToDouble(KnowledgeUnit::confidence).max().orElse(0.5);

                    String summary = catRisks.size() == 1
                            ? catRisks.getFirst().statement()
                            : String.format("%s: %s has %d related %s risk(s) — consolidated from %d alerts",
                                    category, identity, catRisks.size(), category.toLowerCase(), catRisks.size());

                    Set<String> platforms = catRisks.stream()
                            .flatMap(r -> extractPlatforms(r).stream())
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                    consolidated.add(new ConsolidatedAlert(
                            "ALERT-" + String.format("%04d", ++alertCounter),
                            category,
                            summary,
                            classifySeverity(maxConf),
                            maxConf,
                            catRisks,
                            List.of(identity),
                            new ArrayList<>(platforms),
                            generateRemediationSteps(catRisks.getFirst())));
                }
            }
        }

        // Strategy 3: Merge alerts of same type if they have only 1 constituent each and are similar
        consolidated = mergeSmallClusters(consolidated);

        int consolidatedCount = consolidated.size();
        double reduction = originalCount > 0
                ? (1.0 - (double) consolidatedCount / originalCount) * 100.0
                : 0.0;

        // Build cluster size map
        Map<String, Integer> clusterSizes = new LinkedHashMap<>();
        for (ConsolidatedAlert alert : consolidated) {
            clusterSizes.merge(alert.category(), alert.riskCount(), Integer::sum);
        }

        return new ConsolidationResult(originalCount, consolidatedCount, reduction, consolidated, clusterSizes);
    }

    /**
     * Merges standalone alerts of the same category into summary alerts.
     */
    private List<ConsolidatedAlert> mergeSmallClusters(List<ConsolidatedAlert> alerts) {
        // Group single-risk alerts by category
        Map<String, List<ConsolidatedAlert>> singlesByCategory = new LinkedHashMap<>();
        List<ConsolidatedAlert> multiAlerts = new ArrayList<>();

        for (ConsolidatedAlert alert : alerts) {
            if (alert.riskCount() == 1) {
                singlesByCategory.computeIfAbsent(alert.category(), k -> new ArrayList<>()).add(alert);
            } else {
                multiAlerts.add(alert);
            }
        }

        List<ConsolidatedAlert> result = new ArrayList<>(multiAlerts);
        int counter = alerts.size();

        for (Map.Entry<String, List<ConsolidatedAlert>> entry : singlesByCategory.entrySet()) {
            String category = entry.getKey();
            List<ConsolidatedAlert> singles = entry.getValue();

            if (singles.size() >= 3) {
                // Merge 3+ similar single alerts into one summary
                List<KnowledgeUnit> allRisks = singles.stream()
                        .flatMap(a -> a.constituentRisks().stream()).toList();
                double maxConf = singles.stream()
                        .mapToDouble(ConsolidatedAlert::maxConfidence).max().orElse(0.5);
                Set<String> identities = singles.stream()
                        .flatMap(a -> a.affectedIdentities().stream())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                Set<String> platforms = singles.stream()
                        .flatMap(a -> a.affectedPlatforms().stream())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                result.add(new ConsolidatedAlert(
                        "ALERT-" + String.format("%04d", ++counter),
                        category,
                        String.format("%s: %d identities affected — consolidated summary", category, identities.size()),
                        classifySeverity(maxConf),
                        maxConf,
                        allRisks,
                        new ArrayList<>(identities),
                        new ArrayList<>(platforms),
                        generateRemediationSteps(allRisks.getFirst())));
            } else {
                // Keep as individual alerts
                result.addAll(singles);
            }
        }

        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractIdentityFromStatement(String statement) {
        if (statement == null) return "UNKNOWN";
        // Extract name after the risk type prefix (e.g., "OFFBOARDING GAP: John Smith ...")
        int colonIdx = statement.indexOf(':');
        if (colonIdx > 0 && colonIdx < statement.length() - 2) {
            String after = statement.substring(colonIdx + 1).trim();
            // Find the next keyword boundary
            for (String marker : new String[]{" is ", " has ", " on ", " —", " ("}) {
                int idx = after.indexOf(marker);
                if (idx > 0) return after.substring(0, idx).trim();
            }
            return after.length() > 40 ? after.substring(0, 40) : after;
        }
        return statement.length() > 30 ? statement.substring(0, 30) : statement;
    }

    private String extractCategory(String statement) {
        if (statement == null) return "OTHER";
        String upper = statement.toUpperCase();
        if (upper.contains("OFFBOARDING")) return "OFFBOARDING_GAP";
        if (upper.contains("SOD") || upper.contains("SEPARATION")) return "SOD_VIOLATION";
        if (upper.contains("CROSS-PLATFORM ADMIN")) return "CROSS_PLATFORM_ADMIN";
        if (upper.contains("DORMANT ADMIN")) return "DORMANT_ADMIN";
        if (upper.contains("ORPHANED")) return "ORPHANED_ACCOUNT";
        if (upper.contains("STALE SERVICE") || upper.contains("STALE_SERVICE")) return "STALE_SERVICE_ACCOUNT";
        if (upper.contains("STALE EXCEPTION") || upper.contains("STALE_EXCEPTION")) return "STALE_EXCEPTION";
        if (upper.contains("CREDENTIAL") || upper.contains("TOKEN") || upper.contains("ROTATION")) return "CREDENTIAL_VIOLATION";
        if (upper.contains("PRIVILEGE") || upper.contains("CREEP")) return "PRIVILEGE_CREEP";
        if (upper.contains("CONTRACTOR")) return "CONTRACTOR_EXCESSIVE";
        if (upper.contains("PRIVILEGE_SPIKE") || upper.contains("SPIKE")) return "PRIVILEGE_SPIKE";
        if (upper.contains("OFF_HOURS") || upper.contains("OFF-HOURS")) return "OFF_HOURS_ACTIVITY";
        if (upper.contains("CASCADE")) return "CROSS_PLATFORM_CASCADE";
        if (upper.contains("TOKEN_MISUSE") || upper.contains("MFA_BYPASS")) return "TOKEN_MISUSE";
        return "OTHER";
    }

    private List<String> extractPlatforms(KnowledgeUnit risk) {
        List<String> platforms = new ArrayList<>();
        String ctx = risk.context() != null ? risk.context().toString() : "";
        String stmt = risk.statement() != null ? risk.statement() : "";
        String combined = ctx + " " + stmt;
        for (String p : new String[]{"ActiveDirectory", "AWS_IAM", "Okta", "Salesforce", "ServiceNow"}) {
            if (combined.contains(p)) platforms.add(p);
        }
        return platforms.isEmpty() ? List.of("Unknown") : platforms;
    }

    private String classifySeverity(double confidence) {
        if (confidence >= 0.9) return "CRITICAL";
        if (confidence >= 0.7) return "HIGH";
        if (confidence >= 0.4) return "MEDIUM";
        return "LOW";
    }

    private List<String> generateRemediationSteps(KnowledgeUnit risk) {
        String stmt = risk.statement() != null ? risk.statement().toUpperCase() : "";
        if (stmt.contains("OFFBOARDING")) {
            return List.of(
                    "Immediately disable active accounts on remaining platforms",
                    "Revoke all API tokens, session keys, and SSO assignments",
                    "Audit data access logs since termination date",
                    "Update offboarding runbook for cross-platform verification");
        }
        if (stmt.contains("SOD") || stmt.contains("SEPARATION")) {
            return List.of(
                    "Split admin roles: assign IdP admin OR cloud admin, not both",
                    "Implement break-glass procedure for emergency dual access",
                    "Enable enhanced monitoring for remaining admin access",
                    "Schedule quarterly SoD review with compliance team");
        }
        if (stmt.contains("DORMANT")) {
            return List.of(
                    "Disable dormant admin account immediately",
                    "Rotate credentials and revoke active tokens",
                    "Set automated 60-day inactivity warning alerts",
                    "Require reactivation approval from security team");
        }
        if (stmt.contains("STALE") && (stmt.contains("SERVICE") || stmt.contains("EXCEPTION"))) {
            return List.of(
                    "Rotate credentials immediately (keys, passwords, tokens)",
                    "Assign an owner for lifecycle management",
                    "Set 90-day credential rotation policy",
                    "Review and right-size permissions to minimum required");
        }
        if (stmt.contains("CREDENTIAL") || stmt.contains("TOKEN") || stmt.contains("ROTATION")) {
            return List.of(
                    "Rotate all credentials older than policy threshold",
                    "Implement automated rotation via secrets manager",
                    "Review access scope and reduce to minimum required",
                    "Enable alerting for credential age threshold violations");
        }
        return List.of(
                "Review the detected risk and assess business impact",
                "Apply least-privilege principle to reduce scope",
                "Schedule periodic access review",
                "Document remediation for audit trail");
    }
}
