/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.report;

import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.risk.AlertConsolidationEngine;
import org.springaicommunity.agent.ikos.risk.AlertConsolidationEngine.ConsolidatedAlert;
import org.springaicommunity.agent.ikos.risk.AlertConsolidationEngine.ConsolidationResult;
import org.springaicommunity.agent.ikos.risk.BehavioralAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates an interactive HTML dashboard from the {@code dashboard-template.html} resource.
 * Populates template placeholders with live IKOS data including charts, tables, and drill-down data.
 *
 * @author Antigravity
 */
public class InteractiveDashboardGenerator {

    private final AlertConsolidationEngine consolidationEngine = new AlertConsolidationEngine();

    /**
     * Generates the complete interactive dashboard HTML.
     */
    public String generate(List<UnifiedIdentity> identities, List<KnowledgeUnit> risks,
                           List<OffboardingRecord> offboardings,
                           List<TemporaryAccessException> exceptions,
                           BehavioralAnalyzer.AnalysisSummary behavioral,
                           GroupHierarchy hierarchy, Map<String, String> dataStats) {

        String template = loadTemplate();

        // Consolidate alerts
        ConsolidationResult consolidation = consolidationEngine.consolidate(risks);

        // Stale exception risks
        List<KnowledgeUnit> staleExcRisks = new ArrayList<>();
        if (exceptions != null) {
            var riskEngine = new org.springaicommunity.agent.ikos.risk.DefaultRiskDetectionEngine();
            staleExcRisks = riskEngine.detectStaleExceptions(exceptions);
            risks = new ArrayList<>(risks);
            risks.addAll(staleExcRisks);
            consolidation = consolidationEngine.consolidate(risks);
        }

        long critCount = risks.stream().filter(r -> r.confidence() >= 0.9).count();
        long highCount = risks.stream().filter(r -> r.confidence() >= 0.7 && r.confidence() < 0.9).count();
        long offGaps = offboardings != null ? offboardings.stream().filter(OffboardingRecord::hasGap).count() : 0;
        int anomalies = behavioral != null ? behavioral.anomaliesDetected() : 0;
        long staleExc = exceptions != null ? exceptions.stream().filter(TemporaryAccessException::isStale).count() : 0;

        template = template.replace("{{TIMESTAMP}}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        template = template.replace("{{SUMMARY_CARDS}}", summaryCards(identities.size(),
                dataStats.getOrDefault("Total Accounts", "0"), critCount, highCount, offGaps, anomalies, risks.size(), staleExc, consolidation));
        template = template.replace("{{RISK_ROWS}}", riskRows(risks));
        template = template.replace("{{IDENTITY_ROWS}}", identityRows(identities, hierarchy));
        template = template.replace("{{OFFBOARDING_ROWS}}", offboardingRows(offboardings));
        template = template.replace("{{BEHAVIORAL_CARDS}}", behavioralCards(behavioral));
        template = template.replace("{{BEHAVIORAL_ROWS}}", behavioralRows(behavioral));
        template = template.replace("{{BEHAVIORAL_CHARTS}}", "");
        template = template.replace("{{HEATMAP_CELLS}}", heatmapCells(identities, hierarchy));
        template = template.replace("{{CONSOLIDATION_ROWS}}", consolidationRows(consolidation));
        template = template.replace("{{REDUCTION_PCT}}", String.format("%.0f", consolidation.reductionPercentage()));
        template = template.replace("{{ORIGINAL_COUNT}}", String.valueOf(consolidation.originalAlertCount()));
        template = template.replace("{{CONSOLIDATED_COUNT}}", String.valueOf(consolidation.consolidatedAlertCount()));
        template = template.replace("{{CONSOLIDATION_CHARTS}}", "");
        template = template.replace("{{OVERVIEW_CHARTS}}", overviewCharts(risks, identities, behavioral, consolidation, dataStats));
        template = template.replace("{{RISK_REPORT}}", riskReport(identities, risks, hierarchy));
        template = template.replace("{{DATA_STATS_ROWS}}", dataStatsRows(dataStats));
        template = template.replace("{{CHART_SCRIPTS}}", chartScripts(risks, identities, behavioral, consolidation));

        return template;
    }

    private String loadTemplate() {
        try (InputStream is = getClass().getResourceAsStream("/ikos/dashboard-template.html")) {
            if (is != null) return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { /* fallback */ }
        return "<html><body><h1>IKOS Dashboard</h1><p>Template not found</p></body></html>";
    }

    // ── Summary Cards ──

    private String summaryCards(int identities, String accounts, long crit, long high,
                                long offGaps, int anomalies, int totalRisks, long staleExc,
                                ConsolidationResult cons) {
        return card("v-acc", identities, "Unified Identities") +
               card("v-acc", accounts, "Total Accounts") +
               card("v-crit", crit, "Critical Risks") +
               card("v-high", high, "High Risks") +
               card("v-med", offGaps, "Offboarding Gaps") +
               card("v-high", anomalies, "Behavioral Anomalies") +
               card("v-crit", staleExc, "Stale Exceptions") +
               String.format("<div class='card'><div class='value v-low'>%.0f%%</div><div class='label'>Alert Noise Reduction</div></div>",
                       cons.reductionPercentage());
    }

    private String card(String cls, Object val, String label) {
        return String.format("<div class='card'><div class='value %s'>%s</div><div class='label'>%s</div></div>", cls, val, label);
    }

    // ── Risk Table ──

    private String riskRows(List<KnowledgeUnit> risks) {
        StringBuilder sb = new StringBuilder();
        risks.stream()
                .sorted(Comparator.comparingDouble(KnowledgeUnit::confidence).reversed())
                .limit(50)
                .forEach(r -> {
                    String sev = r.confidence() >= 0.9 ? "CRITICAL" : r.confidence() >= 0.7 ? "HIGH" : "MEDIUM";
                    String cls = sev.equals("CRITICAL") ? "b-crit" : sev.equals("HIGH") ? "b-high" : "b-med";
                    String mitre = extractMitre(r.context());
                    sb.append(String.format(
                            "<tr><td><code>%s</code></td><td><span class='badge %s'>%s</span></td>" +
                            "<td style='font-size:0.75rem;color:var(--muted);font-family:monospace'>%s</td>" +
                            "<td>%s</td><td>%.0f%%</td>" +
                            "<td><button class='btn' onclick=\"showDrill('%s')\">Detail</button></td></tr>\n",
                            r.id(), cls, sev, mitre, trunc(r.statement(), 80), r.confidence() * 100, r.id()));
                });
        return sb.toString();
    }

    // ── Identity Table ──

    private String identityRows(List<UnifiedIdentity> identities, GroupHierarchy hierarchy) {
        StringBuilder sb = new StringBuilder();
        identities.stream()
                .sorted(Comparator.comparingDouble(UnifiedIdentity::computeRiskScore).reversed())
                .limit(40)
                .forEach(id -> {
                    EffectivePrivilegeProfile p = EffectivePrivilegeProfile.fromIdentity(id, hierarchy);
                    String cls = p.privilegeRiskScore() >= 0.6 ? "b-crit" : p.privilegeRiskScore() >= 0.3 ? "b-high" : "b-low";
                    String hidden = p.hasHiddenAdmin() ? "⚠ " + p.inheritanceChains().size() + " chain(s)" : "—";
                    sb.append(String.format(
                            "<tr><td><strong>%s</strong><br><small style='color:var(--muted)'>%s</small></td>" +
                            "<td>%d</td><td>%d</td><td>%d</td><td>%d</td>" +
                            "<td><span class='badge %s'>%.0f%%</span></td><td>%s</td></tr>\n",
                            id.displayName(), id.unifiedId(), id.platforms().size(),
                            p.adminPlatforms().size(), p.totalPermissionCount(), p.sensitivePermissions().size(),
                            cls, p.privilegeRiskScore() * 100, hidden));
                });
        return sb.toString();
    }

    // ── Offboarding Table ──

    private String offboardingRows(List<OffboardingRecord> records) {
        if (records == null) return "";
        StringBuilder sb = new StringBuilder();
        records.stream().filter(OffboardingRecord::hasGap)
                .sorted(Comparator.comparingLong(OffboardingRecord::daysSinceTermination).reversed())
                .limit(25)
                .forEach(r -> sb.append(String.format(
                        "<tr><td><strong>%s</strong> (%s)</td><td>%s</td>" +
                        "<td><span class='badge b-crit'>%d days</span></td><td>%s</td>" +
                        "<td style='color:var(--critical)'>%s</td></tr>\n",
                        r.displayName(), r.employeeId(), r.terminationDate(), r.daysSinceTermination(),
                        r.terminationType(), String.join(", ", r.activePlatformsPostTermination()))));
        return sb.toString();
    }

    // ── Behavioral ──

    private String behavioralCards(BehavioralAnalyzer.AnalysisSummary s) {
        if (s == null) return "";
        return card("v-high", s.privilegeSpikes(), "Privilege Spikes") +
               card("v-med", s.offHoursAccess(), "Off-Hours Access") +
               card("v-crit", s.crossPlatformCascades(), "Platform Cascades") +
               card("v-high", s.tokenMisuse(), "Token Misuse");
    }

    private String behavioralRows(BehavioralAnalyzer.AnalysisSummary s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        s.topRiskyProfiles().stream().limit(20).forEach(p -> {
            String cls = p.anomalyScore() >= 0.6 ? "b-crit" : p.anomalyScore() >= 0.3 ? "b-high" : "b-med";
            sb.append(String.format(
                    "<tr><td><code>%s</code></td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td>" +
                    "<td><span class='badge %s'>%.0f%%</span></td><td style='font-size:0.8rem'>%s</td></tr>\n",
                    p.accountId(), p.totalEvents(), p.loginSuccesses(), p.loginFailures(),
                    p.privilegeChanges(), p.offHoursEvents(), cls, p.anomalyScore() * 100,
                    String.join("; ", p.anomalies().stream().map(a -> trunc(a, 45)).toList())));
        });
        return sb.toString();
    }

    // ── Heatmap ──

    private String heatmapCells(List<UnifiedIdentity> identities, GroupHierarchy hierarchy) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='hm-cell hm-hdr'>Identity</div>");
        for (String p : new String[]{"AD", "AWS", "Okta", "SF", "SN"})
            sb.append("<div class='hm-cell hm-hdr'>").append(p).append("</div>");

        identities.stream()
                .sorted(Comparator.comparingDouble(UnifiedIdentity::computeRiskScore).reversed())
                .limit(25)
                .forEach(id -> {
                    EffectivePrivilegeProfile prof = EffectivePrivilegeProfile.fromIdentity(id, hierarchy);
                    sb.append(String.format("<div class='hm-cell' style='text-align:left;font-weight:600'>%s</div>", trunc(id.displayName(), 18)));
                    for (String platform : new String[]{"ActiveDirectory", "AWS_IAM", "Okta", "Salesforce", "ServiceNow"}) {
                        List<String> perms = prof.platformPrivileges().getOrDefault(platform, List.of());
                        boolean admin = prof.adminPlatforms().contains(platform);
                        int h = perms.isEmpty() ? -1 : (admin ? 3 : (perms.size() > 5 ? 2 : 1));
                        String cls = h < 0 ? "h-" : "h" + h;
                        String lbl = h < 0 ? "—" : (admin ? "ADMIN(" + perms.size() + ")" : perms.size() + "");
                        sb.append(String.format("<div class='hm-cell %s'>%s</div>", cls, lbl));
                    }
                });
        return sb.toString();
    }

    // ── Consolidation ──

    private String consolidationRows(ConsolidationResult result) {
        StringBuilder sb = new StringBuilder();
        for (ConsolidatedAlert a : result.consolidatedAlerts()) {
            String cls = switch (a.severity()) {
                case "CRITICAL" -> "b-crit"; case "HIGH" -> "b-high"; case "MEDIUM" -> "b-med"; default -> "b-low";
            };
            sb.append(String.format(
                    "<tr><td><code>%s</code></td><td>%s</td><td><span class='badge %s'>%s</span></td>" +
                    "<td>%d</td><td>%s</td><td>%s</td><td style='font-size:0.82rem'>%s</td></tr>\n",
                    a.alertId(), a.category(), cls, a.severity(), a.riskCount(),
                    String.join(", ", a.affectedIdentities().stream().limit(3).toList()),
                    String.join(", ", a.affectedPlatforms()),
                    trunc(a.summary(), 70)));
        }
        return sb.toString();
    }

    // ── Risk Report ──

    private String riskReport(List<UnifiedIdentity> identities, List<KnowledgeUnit> risks, GroupHierarchy hierarchy) {
        StringBuilder sb = new StringBuilder();
        int rank = 0;

        List<UnifiedIdentity> top = identities.stream()
                .sorted(Comparator.comparingDouble(UnifiedIdentity::computeRiskScore).reversed())
                .limit(10).toList();

        for (UnifiedIdentity id : top) {
            rank++;
            EffectivePrivilegeProfile prof = EffectivePrivilegeProfile.fromIdentity(id, hierarchy);
            List<KnowledgeUnit> idRisks = risks.stream()
                    .filter(r -> r.statement() != null && r.statement().contains(id.displayName()))
                    .toList();

            String cls = prof.privilegeRiskScore() >= 0.6 ? "b-crit" : prof.privilegeRiskScore() >= 0.3 ? "b-high" : "b-med";
            sb.append(String.format("""
                <div style="background:var(--card);border:1px solid var(--border);border-radius:12px;padding:16px;margin:12px 0">
                <h3 style="font-size:1rem">#%d — %s <span class="badge %s">Risk: %.0f%%</span></h3>
                <p style="color:var(--muted);font-size:0.85rem;margin:4px 0">%s | Platforms: %s | Admin on: %d | Permissions: %d (sensitive: %d)</p>
                """, rank, id.displayName(), cls, prof.privilegeRiskScore() * 100,
                    id.unifiedId(), String.join(", ", id.platforms()),
                    prof.adminPlatforms().size(), prof.totalPermissionCount(), prof.sensitivePermissions().size()));

            if (!idRisks.isEmpty()) {
                sb.append("<p style='font-size:0.85rem;margin-top:8px'><strong>Detected Risks:</strong></p><ul style='padding-left:20px;font-size:0.82rem;color:var(--muted)'>");
                for (KnowledgeUnit r : idRisks.stream().limit(3).toList())
                    sb.append("<li>").append(trunc(r.statement(), 100)).append("</li>");
                sb.append("</ul>");
            }

            if (prof.hasHiddenAdmin()) {
                sb.append("<p style='font-size:0.82rem;color:var(--critical);margin-top:6px'>⚠ Hidden Admin Chains: ");
                sb.append(String.join(" | ", prof.inheritanceChains().stream().limit(2).toList()));
                sb.append("</p>");
            }

            sb.append("<p style='font-size:0.82rem;margin-top:8px'><strong>Remediation:</strong></p><ol style='padding-left:20px;font-size:0.82rem;color:var(--text)'>");
            for (String platform : prof.adminPlatforms())
                sb.append("<li>Revoke admin on ").append(platform).append(" — apply least privilege</li>");
            if (id.hasOffboardingGap())
                sb.append("<li>Disable accounts on: ").append(String.join(", ", id.activePlatforms())).append("</li>");
            sb.append("<li>Schedule quarterly access review</li></ol></div>");
        }
        return sb.toString();
    }

    // ── Overview Charts ──

    private String overviewCharts(List<KnowledgeUnit> risks, List<UnifiedIdentity> identities,
                                   BehavioralAnalyzer.AnalysisSummary behavioral,
                                   ConsolidationResult consolidation, Map<String, String> dataStats) {
        long crit = risks.stream().filter(r -> r.confidence() >= 0.9).count();
        long high = risks.stream().filter(r -> r.confidence() >= 0.7 && r.confidence() < 0.9).count();
        long med = risks.stream().filter(r -> r.confidence() < 0.7).count();

        // Count risk types
        long offboarding = risks.stream().filter(r -> r.statement() != null && r.statement().toUpperCase().contains("OFFBOARDING")).count();
        long sod = risks.stream().filter(r -> r.statement() != null && r.statement().toUpperCase().contains("SOD")).count();
        long dormant = risks.stream().filter(r -> r.statement() != null && r.statement().toUpperCase().contains("DORMANT")).count();
        long stale = risks.stream().filter(r -> r.statement() != null && (r.statement().toUpperCase().contains("STALE") || r.statement().toUpperCase().contains("ORPHAN"))).count();
        long crossAdmin = risks.stream().filter(r -> r.statement() != null && r.statement().toUpperCase().contains("CROSS")).count();
        long behavioral_ = risks.stream().filter(r -> r.statement() != null && (r.statement().toUpperCase().contains("PRIVILEGE SPIKE") || r.statement().toUpperCase().contains("OFF-HOURS") || r.statement().toUpperCase().contains("TOKEN"))).count();

        // Platform counts
        Map<String, Long> platformCounts = new LinkedHashMap<>();
        for (UnifiedIdentity id : identities) {
            for (String p : id.platforms()) {
                platformCounts.merge(p, 1L, Long::sum);
            }
        }

        StringBuilder sb = new StringBuilder();

        // ── Row 1: Risk Distribution + Platform Coverage ────────────────
        sb.append("<div class='chart-grid'>");

        // Risk Severity Distribution (Doughnut)
        sb.append("""
            <div class='chart-box'>
                <h3>📊 Risk Severity Distribution</h3>
                <canvas id='chart-risk-dist' height='260'></canvas>
            </div>
        """);

        // Platform Coverage (Bar)
        sb.append("""
            <div class='chart-box'>
                <h3>🏢 Platform Account Distribution</h3>
                <canvas id='chart-platform-dist' height='260'></canvas>
            </div>
        """);

        sb.append("</div>");

        // ── Row 2: Risk Type Breakdown + Alert Consolidation ─────────────
        sb.append("<div class='chart-grid'>");

        // Risk Type Breakdown (Horizontal Bar)
        sb.append("""
            <div class='chart-box'>
                <h3>🔍 Risk Type Breakdown</h3>
                <canvas id='chart-risk-types' height='260'></canvas>
            </div>
        """);

        // Alert Consolidation (Before/After)
        sb.append(String.format("""
            <div class='chart-box'>
                <h3>🔗 Alert Consolidation</h3>
                <canvas id='chart-consolidation' height='260'></canvas>
            </div>
        """));

        sb.append("</div>");

        // ── Row 3: Knowledge Evolution Pipeline + Agent Architecture ─────
        sb.append("<div class='chart-grid'>");

        // Knowledge Evolution Pipeline
        sb.append("""
            <div class='chart-box' style='grid-column: span 2'>
                <h3>🧠 Knowledge Evolution Pipeline</h3>
                <div style='display:flex;align-items:center;justify-content:center;gap:0;padding:20px 10px;flex-wrap:wrap'>
                    <div style='text-align:center;padding:16px 20px;background:rgba(99,102,241,0.12);border-radius:12px;border:1px solid rgba(99,102,241,0.25);min-width:140px'>
                        <div style='font-size:2rem;font-weight:800;color:#6366f1'>30%</div>
                        <div style='font-size:0.72rem;text-transform:uppercase;letter-spacing:0.5px;color:var(--muted);font-weight:600;margin-top:4px'>Observation</div>
                        <div style='font-size:0.7rem;color:var(--muted);margin-top:2px'>Raw detection signal</div>
                    </div>
                    <div style='font-size:1.5rem;color:var(--muted);padding:0 8px'>→</div>
                    <div style='text-align:center;padding:16px 20px;background:rgba(139,92,246,0.12);border-radius:12px;border:1px solid rgba(139,92,246,0.25);min-width:140px'>
                        <div style='font-size:2rem;font-weight:800;color:#8b5cf6'>50%</div>
                        <div style='font-size:0.72rem;text-transform:uppercase;letter-spacing:0.5px;color:var(--muted);font-weight:600;margin-top:4px'>Pattern Candidate</div>
                        <div style='font-size:0.7rem;color:var(--muted);margin-top:2px'>Auto-discovered cluster</div>
                    </div>
                    <div style='font-size:1.5rem;color:var(--muted);padding:0 8px'>→</div>
                    <div style='text-align:center;padding:16px 20px;background:rgba(249,115,22,0.12);border-radius:12px;border:1px solid rgba(249,115,22,0.25);min-width:140px'>
                        <div style='font-size:2rem;font-weight:800;color:#f97316'>70%</div>
                        <div style='font-size:0.72rem;text-transform:uppercase;letter-spacing:0.5px;color:var(--muted);font-weight:600;margin-top:4px'>Validated Pattern</div>
                        <div style='font-size:0.7rem;color:var(--muted);margin-top:2px'>Governance-approved</div>
                    </div>
                    <div style='font-size:1.5rem;color:var(--muted);padding:0 8px'>→</div>
                    <div style='text-align:center;padding:16px 20px;background:rgba(34,197,94,0.12);border-radius:12px;border:1px solid rgba(34,197,94,0.25);min-width:140px'>
                        <div style='font-size:2rem;font-weight:800;color:#22c55e'>90%</div>
                        <div style='font-size:0.72rem;text-transform:uppercase;letter-spacing:0.5px;color:var(--muted);font-weight:600;margin-top:4px'>Security Knowledge</div>
                        <div style='font-size:0.7rem;color:var(--muted);margin-top:2px'>Outcome-validated</div>
                    </div>
                </div>
                <div style='text-align:center;color:var(--muted);font-size:0.78rem;margin-top:8px'>
                    Confidence = <strong>Evidence × Consistency × Validation × Outcomes</strong> &nbsp;|&nbsp;
                    Feedback loop: remediation outcomes compound into organizational memory
                </div>
            </div>
        """);

        sb.append("</div>");

        // ── Row 4: Spring AI Agent Architecture ──────────────────────────
        sb.append("<div class='chart-grid'>");

        sb.append("""
            <div class='chart-box' style='grid-column: span 2'>
                <h3>🤖 Spring AI Agent Integration Architecture</h3>
                <div style='display:grid;grid-template-columns:1fr auto 1fr auto 1fr;gap:12px;align-items:center;padding:16px'>
                    <div style='background:rgba(99,102,241,0.12);border:1px solid rgba(99,102,241,0.25);border-radius:12px;padding:14px;text-align:center'>
                        <div style='font-size:1.2rem;margin-bottom:6px'>💬</div>
                        <div style='font-weight:700;font-size:0.85rem;color:#6366f1'>ChatClient</div>
                        <div style='font-size:0.7rem;color:var(--muted);margin-top:4px'>Spring AI 2.0</div>
                        <div style='font-size:0.68rem;color:var(--muted);margin-top:2px'>Autonomous agent loop</div>
                    </div>
                    <div style='font-size:1.2rem;color:var(--muted)'>→</div>
                    <div style='background:rgba(139,92,246,0.12);border:1px solid rgba(139,92,246,0.25);border-radius:12px;padding:14px;text-align:center'>
                        <div style='font-size:1.2rem;margin-bottom:6px'>🧠</div>
                        <div style='font-weight:700;font-size:0.85rem;color:#8b5cf6'>Advisors</div>
                        <div style='font-size:0.7rem;color:var(--muted);margin-top:4px'>KnowledgeEvolutionAdvisor</div>
                        <div style='font-size:0.68rem;color:var(--muted);margin-top:2px'>AutoMemoryToolsAdvisor</div>
                    </div>
                    <div style='font-size:1.2rem;color:var(--muted)'>→</div>
                    <div style='background:rgba(34,197,94,0.12);border:1px solid rgba(34,197,94,0.25);border-radius:12px;padding:14px;text-align:center'>
                        <div style='font-size:1.2rem;margin-bottom:6px'>🔧</div>
                        <div style='font-weight:700;font-size:0.85rem;color:#22c55e'>@Tool Methods</div>
                        <div style='font-size:0.7rem;color:var(--muted);margin-top:4px'>11 IKOS governance tools</div>
                        <div style='font-size:0.68rem;color:var(--muted);margin-top:2px'>+ agent-utils core tools</div>
                    </div>
                </div>
                <div style='display:flex;gap:8px;flex-wrap:wrap;justify-content:center;padding:0 16px 8px'>
                    <span style='padding:3px 10px;background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.2);border-radius:8px;font-size:0.7rem;color:#6366f1'>AnalyzeRisks</span>
                    <span style='padding:3px 10px;background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.2);border-radius:8px;font-size:0.7rem;color:#6366f1'>DetectOffboarding</span>
                    <span style='padding:3px 10px;background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.2);border-radius:8px;font-size:0.7rem;color:#6366f1'>ComplianceCheck</span>
                    <span style='padding:3px 10px;background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.2);border-radius:8px;font-size:0.7rem;color:#6366f1'>BlastRadius</span>
                    <span style='padding:3px 10px;background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.2);border-radius:8px;font-size:0.7rem;color:#6366f1'>IdentityGraph</span>
                    <span style='padding:3px 10px;background:rgba(139,92,246,0.1);border:1px solid rgba(139,92,246,0.2);border-radius:8px;font-size:0.7rem;color:#8b5cf6'>TodoWrite</span>
                    <span style='padding:3px 10px;background:rgba(139,92,246,0.1);border:1px solid rgba(139,92,246,0.2);border-radius:8px;font-size:0.7rem;color:#8b5cf6'>Skills</span>
                    <span style='padding:3px 10px;background:rgba(139,92,246,0.1);border:1px solid rgba(139,92,246,0.2);border-radius:8px;font-size:0.7rem;color:#8b5cf6'>Subagents</span>
                    <span style='padding:3px 10px;background:rgba(34,197,94,0.1);border:1px solid rgba(34,197,94,0.2);border-radius:8px;font-size:0.7rem;color:#22c55e'>AutoMemory</span>
                    <span style='padding:3px 10px;background:rgba(34,197,94,0.1);border:1px solid rgba(34,197,94,0.2);border-radius:8px;font-size:0.7rem;color:#22c55e'>Shell</span>
                    <span style='padding:3px 10px;background:rgba(34,197,94,0.1);border:1px solid rgba(34,197,94,0.2);border-radius:8px;font-size:0.7rem;color:#22c55e'>FileSystem</span>
                </div>
            </div>
        """);

        sb.append("</div>");

        // ── Chart.js Initialization Script ────────────────────────────────
        sb.append("<script>\n");

        // Risk Distribution Doughnut
        sb.append(String.format("""
            new Chart(document.getElementById('chart-risk-dist'), {
                type:'doughnut',
                data:{labels:['Critical','High','Medium'],datasets:[{data:[%d,%d,%d],
                    backgroundColor:['rgba(239,68,68,0.8)','rgba(249,115,22,0.8)','rgba(234,179,8,0.8)'],
                    borderColor:['rgba(239,68,68,1)','rgba(249,115,22,1)','rgba(234,179,8,1)'],borderWidth:2,hoverOffset:6}]},
                options:{responsive:true,plugins:{legend:{position:'bottom',labels:{color:'#9ca3af',font:{size:12}}},
                    tooltip:{callbacks:{label:function(c){return c.label+': '+c.raw+' risks ('+Math.round(c.raw/%d*100)+'%%)';}}}}}
            });
        """, crit, high, med, Math.max(risks.size(), 1)));

        // Platform Distribution Bar
        sb.append("new Chart(document.getElementById('chart-platform-dist'),{type:'bar',data:{labels:[");
        sb.append(platformCounts.keySet().stream().map(p -> "'" + p.replace("_", " ") + "'").collect(Collectors.joining(",")));
        sb.append("],datasets:[{label:'Accounts',data:[");
        sb.append(platformCounts.values().stream().map(String::valueOf).collect(Collectors.joining(",")));
        sb.append("],backgroundColor:['rgba(99,102,241,0.7)','rgba(249,115,22,0.7)','rgba(6,182,212,0.7)','rgba(139,92,246,0.7)','rgba(236,72,153,0.7)'],borderColor:['rgba(99,102,241,1)','rgba(249,115,22,1)','rgba(6,182,212,1)','rgba(139,92,246,1)','rgba(236,72,153,1)'],borderWidth:2,borderRadius:6}]},options:{responsive:true,indexAxis:'y',plugins:{legend:{display:false}},scales:{x:{grid:{color:'rgba(31,41,55,0.6)'},ticks:{color:'#9ca3af'}},y:{grid:{display:false},ticks:{color:'#e5e7eb',font:{weight:'600'}}}}}});\n");

        // Risk Type Breakdown (Horizontal Bar)
        sb.append(String.format("""
            new Chart(document.getElementById('chart-risk-types'),{type:'bar',data:{labels:['Offboarding Gap','SoD Violation','Dormant/Stale','Cross-Platform Admin','Behavioral','Other'],
                datasets:[{label:'Risks',data:[%d,%d,%d,%d,%d,%d],
                    backgroundColor:['rgba(239,68,68,0.7)','rgba(249,115,22,0.7)','rgba(234,179,8,0.7)','rgba(139,92,246,0.7)','rgba(6,182,212,0.7)','rgba(107,114,128,0.5)'],
                    borderColor:['rgba(239,68,68,1)','rgba(249,115,22,1)','rgba(234,179,8,1)','rgba(139,92,246,1)','rgba(6,182,212,1)','rgba(107,114,128,1)'],
                    borderWidth:2,borderRadius:6}]},
                options:{responsive:true,plugins:{legend:{display:false}},
                    scales:{y:{grid:{color:'rgba(31,41,55,0.6)'},ticks:{color:'#9ca3af'}},x:{grid:{display:false},ticks:{color:'#e5e7eb'}}}}});
        """, offboarding, sod, stale, crossAdmin, behavioral_,
                risks.size() - offboarding - sod - stale - crossAdmin - behavioral_));

        // Alert Consolidation Comparison
        sb.append(String.format("""
            new Chart(document.getElementById('chart-consolidation'),{type:'bar',
                data:{labels:['Before','After'],datasets:[{label:'Alerts',data:[%d,%d],
                    backgroundColor:['rgba(239,68,68,0.6)','rgba(34,197,94,0.6)'],
                    borderColor:['rgba(239,68,68,1)','rgba(34,197,94,1)'],borderWidth:2,borderRadius:8}]},
                options:{responsive:true,plugins:{legend:{display:false},
                    title:{display:true,text:'%.0f%% Noise Reduction',color:'#22c55e',font:{size:16,weight:'700'}}},
                    scales:{y:{grid:{color:'rgba(31,41,55,0.6)'},ticks:{color:'#9ca3af'}},x:{grid:{display:false},ticks:{color:'#e5e7eb',font:{weight:'600'}}}}}});
        """, consolidation.originalAlertCount(), consolidation.consolidatedAlertCount(),
                consolidation.reductionPercentage()));

        sb.append("</script>");
        return sb.toString();
    }

    // ── Data Stats ──

    private String dataStatsRows(Map<String, String> stats) {
        StringBuilder sb = new StringBuilder();
        stats.forEach((k, v) -> sb.append(String.format("<tr><td>%s</td><td><strong>%s</strong></td></tr>\n", k, v)));
        return sb.toString();
    }

    // ── Chart Scripts ──

    private String chartScripts(List<KnowledgeUnit> risks, List<UnifiedIdentity> identities,
                                 BehavioralAnalyzer.AnalysisSummary behavioral, ConsolidationResult cons) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nwindow.IKOS_DRILL_DATA = {\n");
        for (KnowledgeUnit r : risks) {
            String sev = r.confidence() >= 0.9 ? "CRITICAL" : r.confidence() >= 0.7 ? "HIGH" : "MEDIUM";
            String sevCls = sev.equals("CRITICAL") ? "crit" : sev.equals("HIGH") ? "high" : "med";
            List<String> ev = r.evidence() != null ? r.evidence().stream().map(e -> jsEsc(e.description())).toList() : List.of();
            String stmt = r.statement() != null ? r.statement().toUpperCase() : "";
            List<String> rem = new ArrayList<>();
            if (stmt.contains("OFFBOARDING")) rem.addAll(List.of("Disable active accounts immediately", "Revoke API tokens and sessions", "Audit data access since termination"));
            else if (stmt.contains("SOD")) rem.addAll(List.of("Split admin roles across identities", "Implement break-glass procedure", "Enable enhanced monitoring"));
            else if (stmt.contains("DORMANT")) rem.addAll(List.of("Disable dormant admin account", "Rotate all credentials", "Set automated inactivity alerts"));
            else if (stmt.contains("STALE")) rem.addAll(List.of("Revoke expired temporary access", "Rotate credentials", "Update exception tracker"));
            else rem.addAll(List.of("Review risk and assess impact", "Apply least privilege", "Schedule access review"));

            sb.append(String.format("'%s':{id:'%s',severity:'%s',sevClass:'%s',statement:'%s',evidence:[%s],remediation:[%s],policies:'%s'},\n",
                    r.id(), r.id(), sev, sevCls, jsEsc(r.statement()),
                    ev.stream().map(e -> "'" + e + "'").collect(Collectors.joining(",")),
                    rem.stream().map(e -> "'" + jsEsc(e) + "'").collect(Collectors.joining(",")),
                    jsEsc(r.context() != null ? r.context().toString() : "")));
        }
        sb.append("};\n");
        return sb.toString();
    }

    // ── Helpers ──

    private String extractMitre(Object ctx) {
        if (ctx == null) return "";
        String s = ctx.toString();
        for (String risk : new String[]{"OFFBOARDING_GAP", "CROSS_PLATFORM_ADMIN", "DORMANT_ADMIN", "TOKEN_ABUSE", "ORPHANED_ACCOUNT", "SOD_VIOLATION", "STALE_EXCEPTION"}) {
            if (s.contains(risk)) {
                try { return RiskType.valueOf(risk).mitreRef(); } catch (Exception e) { /* ignore */ }
            }
        }
        return "";
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static String jsEsc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", "");
    }
}
