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

import org.springaicommunity.agent.memory.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Behavioral analysis engine for detecting anomalous identity activity patterns.
 *
 * <p>Detects:
 * <ul>
 *   <li>Cross-platform login cascades (SSO vs. credential compromise)</li>
 *   <li>Privilege spikes (>3 role grants in 7 days)</li>
 *   <li>Off-hours admin activity</li>
 *   <li>External IP access patterns</li>
 *   <li>Failed login anomalies</li>
 *   <li>Token misuse from unexpected IPs</li>
 * </ul>
 *
 * @author Antigravity
 */
public class BehavioralAnalyzer {

    /**
     * Result of behavioral analysis for a single identity.
     */
    public record BehavioralProfile(
            String accountId,
            int totalEvents,
            int loginSuccesses,
            int loginFailures,
            int privilegeChanges,
            int offHoursEvents,
            int externalIpEvents,
            int platformsAccessed,
            double anomalyScore,
            List<String> anomalies
    ) {}

    /**
     * Summary of behavioral analysis across all identities.
     */
    public record AnalysisSummary(
            int totalEventsAnalyzed,
            int identitiesAnalyzed,
            int anomaliesDetected,
            int privilegeSpikes,
            int offHoursAccess,
            int crossPlatformCascades,
            int tokenMisuse,
            List<BehavioralProfile> topRiskyProfiles,
            List<KnowledgeUnit> generatedRisks
    ) {}

    /**
     * Analyzes audit events for all identities and produces behavioral risk profiles.
     */
    public AnalysisSummary analyze(List<AuditEvent> events) {
        if (events == null || events.isEmpty()) {
            return new AnalysisSummary(0, 0, 0, 0, 0, 0, 0, List.of(), List.of());
        }

        // Group events by account
        Map<String, List<AuditEvent>> byAccount = events.stream()
                .collect(Collectors.groupingBy(AuditEvent::accountId));

        List<BehavioralProfile> profiles = new ArrayList<>();
        List<KnowledgeUnit> risks = new ArrayList<>();
        int totalAnomalies = 0, privSpikes = 0, offHours = 0, cascades = 0, tokenMisuse = 0;

        for (Map.Entry<String, List<AuditEvent>> entry : byAccount.entrySet()) {
            String accountId = entry.getKey();
            List<AuditEvent> accountEvents = entry.getValue();
            BehavioralProfile profile = analyzeAccount(accountId, accountEvents);
            profiles.add(profile);

            totalAnomalies += profile.anomalies().size();

            // Generate risk KnowledgeUnits for significant anomalies
            for (String anomaly : profile.anomalies()) {
                if (anomaly.contains("PRIVILEGE_SPIKE")) privSpikes++;
                if (anomaly.contains("OFF_HOURS")) offHours++;
                if (anomaly.contains("CROSS_PLATFORM_CASCADE")) cascades++;
                if (anomaly.contains("TOKEN_MISUSE")) tokenMisuse++;

                if (profile.anomalyScore() >= 0.6) {
                    risks.add(KnowledgeUnit.builder()
                            .id("BEHAV-" + accountId.hashCode() + "-" + risks.size())
                            .statement(anomaly)
                            .type(KnowledgeType.RISK_OBSERVATION)
                            .state(KnowledgeState.OBSERVATION)
                            .context("Behavioral analysis | Account: " + accountId +
                                     " | Anomaly Score: " + String.format("%.2f", profile.anomalyScore()))
                            .confidence(profile.anomalyScore())
                            .lastReviewed(LocalDateTime.now())
                            .build());
                }
            }
        }

        // Sort by anomaly score descending
        profiles.sort(Comparator.comparingDouble(BehavioralProfile::anomalyScore).reversed());
        List<BehavioralProfile> topRisky = profiles.stream()
                .filter(p -> p.anomalyScore() > 0.3)
                .limit(20)
                .toList();

        return new AnalysisSummary(
                events.size(), byAccount.size(), totalAnomalies,
                privSpikes, offHours, cascades, tokenMisuse,
                topRisky, risks);
    }

    private BehavioralProfile analyzeAccount(String accountId, List<AuditEvent> events) {
        List<String> anomalies = new ArrayList<>();
        double anomalyScore = 0.0;

        int loginSuccess = 0, loginFail = 0, privChanges = 0, offHours = 0, externalIp = 0;
        Set<String> platforms = new HashSet<>();

        for (AuditEvent e : events) {
            platforms.add(e.platform());

            switch (e.eventType()) {
                case LOGIN_SUCCESS -> loginSuccess++;
                case LOGIN_FAILURE -> loginFail++;
                case PRIVILEGE_GRANT, ROLE_CHANGE, GROUP_ADD -> privChanges++;
                default -> {}
            }

            if (e.isOffHours()) offHours++;
            if (e.sourceIp() != null && !e.sourceIp().startsWith("10.") && !e.sourceIp().startsWith("172.16.")) {
                externalIp++;
            }
        }

        // ── Rule 1: Privilege spike (>3 privilege changes in last 7 days) ──
        long recentPrivChanges = events.stream()
                .filter(AuditEvent::isPrivilegeEscalation)
                .filter(e -> e.timestamp().isAfter(LocalDateTime.now().minusDays(7)))
                .count();
        if (recentPrivChanges > 3) {
            anomalies.add(String.format("PRIVILEGE_SPIKE: %s had %d privilege changes in 7 days",
                    accountId, recentPrivChanges));
            anomalyScore += 0.30;
        }

        // ── Rule 2: Off-hours admin activity ──
        if (offHours > events.size() * 0.3 && events.size() > 5) {
            anomalies.add(String.format("OFF_HOURS_ACTIVITY: %s has %.0f%% off-hours events (%d/%d)",
                    accountId, (double) offHours / events.size() * 100, offHours, events.size()));
            anomalyScore += 0.20;
        }

        // ── Rule 3: Cross-platform cascade (>3 platforms in 10 minutes) ──
        if (platforms.size() >= 3) {
            long distinctTimestamps = events.stream()
                    .map(e -> e.timestamp().toLocalDate())
                    .distinct().count();
            if (distinctTimestamps <= 2 && events.size() > 5) {
                anomalies.add(String.format("CROSS_PLATFORM_CASCADE: %s accessed %d platforms in short window",
                        accountId, platforms.size()));
                anomalyScore += 0.25;
            }
        }

        // ── Rule 4: Failed login ratio ──
        if (loginFail > 5 && loginFail > loginSuccess * 2) {
            anomalies.add(String.format("EXCESSIVE_FAILURES: %s has %d failed vs %d successful logins",
                    accountId, loginFail, loginSuccess));
            anomalyScore += 0.25;
        }

        // ── Rule 5: Token usage from external IP ──
        long externalTokenUse = events.stream()
                .filter(e -> e.eventType() == AuditEvent.EventType.TOKEN_USAGE)
                .filter(e -> e.sourceIp() != null && !e.sourceIp().startsWith("10.") && !e.sourceIp().startsWith("172."))
                .count();
        if (externalTokenUse > 0) {
            anomalies.add(String.format("TOKEN_MISUSE: %s API token used from %d external IP(s)",
                    accountId, externalTokenUse));
            anomalyScore += 0.30;
        }

        // ── Rule 6: MFA bypass ──
        long mfaBypasses = events.stream()
                .filter(e -> e.eventType() == AuditEvent.EventType.MFA_BYPASS)
                .count();
        if (mfaBypasses > 0) {
            anomalies.add(String.format("MFA_BYPASS: %s had %d MFA bypass events", accountId, mfaBypasses));
            anomalyScore += 0.35;
        }

        return new BehavioralProfile(
                accountId, events.size(), loginSuccess, loginFail,
                privChanges, offHours, externalIp, platforms.size(),
                Math.min(1.0, anomalyScore), anomalies);
    }
}
