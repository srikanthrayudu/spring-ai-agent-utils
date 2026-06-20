/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory.report;

import org.springaicommunity.agent.memory.model.*;
import org.springaicommunity.agent.memory.risk.BehavioralAnalyzer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates an interactive HTML dashboard report for the IKOS risk analysis.
 *
 * @author Antigravity
 */
public class HtmlReportGenerator {

    /**
     * Generates the full HTML dashboard.
     */
    public String generate(List<UnifiedIdentity> identities,
                           List<KnowledgeUnit> risks,
                           List<OffboardingRecord> offboardings,
                           BehavioralAnalyzer.AnalysisSummary behavioral,
                           GroupHierarchy hierarchy,
                           Map<String, String> dataStats) {

        StringBuilder html = new StringBuilder();
        html.append(header());
        html.append(summaryCards(identities, risks, offboardings, behavioral, dataStats));
        html.append(riskTable(risks));
        html.append(identityRiskTable(identities, hierarchy));
        html.append(offboardingGaps(offboardings));
        html.append(behavioralAnomalies(behavioral));
        html.append(privilegeHeatmap(identities, hierarchy));
        html.append(complianceAlignment());
        html.append(dataDict());
        html.append(footer());

        return html.toString();
    }

    private String header() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>IKOS — Identity Risk Intelligence Dashboard</title>
<style>
:root{--bg:#0a0e17;--card:#111827;--border:#1f2937;--accent:#3b82f6;--critical:#ef4444;
--high:#f97316;--medium:#eab308;--low:#22c55e;--text:#e5e7eb;--muted:#9ca3af;--surface:#1e293b}
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Inter','Segoe UI',system-ui,sans-serif;background:var(--bg);color:var(--text);line-height:1.6}
.container{max-width:1400px;margin:0 auto;padding:24px}
.header{text-align:center;padding:40px 0 30px;border-bottom:1px solid var(--border)}
.header h1{font-size:2.2rem;background:linear-gradient(135deg,#3b82f6,#8b5cf6);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:8px}
.header p{color:var(--muted);font-size:0.95rem}
.badge{display:inline-block;padding:2px 10px;border-radius:12px;font-size:0.75rem;font-weight:600;text-transform:uppercase}
.badge-critical{background:rgba(239,68,68,0.15);color:#ef4444;border:1px solid rgba(239,68,68,0.3)}
.badge-high{background:rgba(249,115,22,0.15);color:#f97316;border:1px solid rgba(249,115,22,0.3)}
.badge-medium{background:rgba(234,179,8,0.15);color:#eab308;border:1px solid rgba(234,179,8,0.3)}
.badge-low{background:rgba(34,197,94,0.15);color:#22c55e;border:1px solid rgba(34,197,94,0.3)}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:16px;margin:30px 0}
.card{background:var(--card);border:1px solid var(--border);border-radius:12px;padding:20px;text-align:center}
.card .value{font-size:2.5rem;font-weight:700;margin:8px 0}
.card .label{color:var(--muted);font-size:0.85rem;text-transform:uppercase;letter-spacing:0.5px}
.card-critical .value{color:var(--critical)}
.card-high .value{color:var(--high)}
.card-medium .value{color:var(--medium)}
.card-low .value{color:var(--low)}
.card-accent .value{color:var(--accent)}
.section{margin:40px 0}
.section h2{font-size:1.4rem;margin-bottom:16px;padding-bottom:8px;border-bottom:2px solid var(--border);display:flex;align-items:center;gap:10px}
table{width:100%;border-collapse:collapse;background:var(--card);border-radius:12px;overflow:hidden;border:1px solid var(--border)}
th{background:var(--surface);color:var(--muted);font-size:0.8rem;text-transform:uppercase;letter-spacing:0.5px;padding:12px 16px;text-align:left}
td{padding:10px 16px;border-top:1px solid var(--border);font-size:0.9rem}
tr:hover{background:rgba(59,130,246,0.05)}
.heatmap{display:grid;grid-template-columns:200px repeat(5,1fr);gap:2px;margin:16px 0}
.heatmap-cell{padding:10px 8px;text-align:center;font-size:0.8rem;border-radius:4px}
.heatmap-header{background:var(--surface);color:var(--muted);font-weight:600}
.heat-0{background:rgba(34,197,94,0.1);color:#22c55e}
.heat-1{background:rgba(234,179,8,0.15);color:#eab308}
.heat-2{background:rgba(249,115,22,0.2);color:#f97316}
.heat-3{background:rgba(239,68,68,0.25);color:#ef4444}
.compliance-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px}
.compliance-card{background:var(--card);border:1px solid var(--border);border-radius:12px;padding:20px}
.compliance-card h3{font-size:1rem;margin-bottom:10px;color:var(--accent)}
.compliance-card ul{list-style:none;padding:0}
.compliance-card li{padding:4px 0;font-size:0.85rem;color:var(--muted)}
.compliance-card li::before{content:'✓ ';color:var(--low)}
.mitre{font-size:0.75rem;color:var(--muted);font-family:monospace}
.gen-time{text-align:center;color:var(--muted);font-size:0.8rem;padding:30px 0;border-top:1px solid var(--border);margin-top:40px}
</style>
</head>
<body>
<div class="container">
<div class="header">
<h1>⛨ IKOS — Identity Risk Intelligence Dashboard</h1>
<p>Identity Knowledge Operating System | Cross-Platform Identity Sprawl & Privileged Access Abuse Detection</p>
</div>
""";
    }

    private String summaryCards(List<UnifiedIdentity> identities, List<KnowledgeUnit> risks,
                                 List<OffboardingRecord> offboardings, BehavioralAnalyzer.AnalysisSummary behavioral,
                                 Map<String, String> dataStats) {
        long criticalCount = risks.stream().filter(r -> r.confidence() >= 0.9).count();
        long highCount = risks.stream().filter(r -> r.confidence() >= 0.7 && r.confidence() < 0.9).count();
        long offboardingGaps = offboardings.stream().filter(OffboardingRecord::hasGap).count();
        int behavioralAnomalies = behavioral != null ? behavioral.anomaliesDetected() : 0;

        return String.format("""
<div class="cards">
<div class="card card-accent"><div class="value">%s</div><div class="label">Unified Identities</div></div>
<div class="card card-accent"><div class="value">%s</div><div class="label">Total Accounts</div></div>
<div class="card card-critical"><div class="value">%d</div><div class="label">Critical Risks</div></div>
<div class="card card-high"><div class="value">%d</div><div class="label">High Risks</div></div>
<div class="card card-medium"><div class="value">%d</div><div class="label">Offboarding Gaps</div></div>
<div class="card card-high"><div class="value">%d</div><div class="label">Behavioral Anomalies</div></div>
<div class="card card-low"><div class="value">%d</div><div class="label">Total Risks</div></div>
</div>
""", identities.size(), dataStats.getOrDefault("Total Accounts", "0"),
                criticalCount, highCount, offboardingGaps, behavioralAnomalies, risks.size());
    }

    private String riskTable(List<KnowledgeUnit> risks) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
<div class="section">
<h2>🔴 Risk Observations</h2>
<table>
<thead><tr><th>Risk ID</th><th>Severity</th><th>MITRE</th><th>Statement</th><th>Confidence</th></tr></thead>
<tbody>
""");

        // Sort by confidence descending
        risks.stream()
                .sorted(Comparator.comparingDouble(KnowledgeUnit::confidence).reversed())
                .limit(30)
                .forEach(risk -> {
                    String sev = risk.confidence() >= 0.9 ? "CRITICAL" : risk.confidence() >= 0.7 ? "HIGH" : "MEDIUM";
                    String sevClass = sev.toLowerCase();
                    String mitreId = extractMitreFromContext(risk.context());
                    sb.append(String.format(
                            "<tr><td><code>%s</code></td><td><span class=\"badge badge-%s\">%s</span></td>" +
                            "<td class=\"mitre\">%s</td><td>%s</td><td>%.0f%%</td></tr>\n",
                            risk.id(), sevClass, sev, mitreId, truncate(risk.statement(), 90),
                            risk.confidence() * 100));
                });

        sb.append("</tbody></table></div>\n");
        return sb.toString();
    }

    private String identityRiskTable(List<UnifiedIdentity> identities, GroupHierarchy hierarchy) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
<div class="section">
<h2>👤 Identity Risk Rankings</h2>
<table>
<thead><tr><th>Identity</th><th>Platforms</th><th>Admin On</th><th>Permissions</th><th>Sensitive</th><th>Risk Score</th><th>Hidden Admin</th></tr></thead>
<tbody>
""");

        identities.stream()
                .sorted(Comparator.comparingDouble(UnifiedIdentity::computeRiskScore).reversed())
                .limit(30)
                .forEach(id -> {
                    EffectivePrivilegeProfile prof = EffectivePrivilegeProfile.fromIdentity(id, hierarchy);
                    String riskClass = prof.privilegeRiskScore() >= 0.6 ? "critical" :
                            prof.privilegeRiskScore() >= 0.3 ? "high" : "low";
                    String hidden = prof.hasHiddenAdmin() ? "⚠ " + prof.inheritanceChains().size() + " chain(s)" : "—";
                    sb.append(String.format(
                            "<tr><td><strong>%s</strong><br><small style='color:var(--muted)'>%s</small></td>" +
                            "<td>%d</td><td>%d</td><td>%d</td><td>%d</td>" +
                            "<td><span class=\"badge badge-%s\">%.0f%%</span></td><td>%s</td></tr>\n",
                            id.displayName(), id.unifiedId(),
                            id.platforms().size(), prof.adminPlatforms().size(),
                            prof.totalPermissionCount(), prof.sensitivePermissions().size(),
                            riskClass, prof.privilegeRiskScore() * 100, hidden));
                });

        sb.append("</tbody></table></div>\n");
        return sb.toString();
    }

    private String offboardingGaps(List<OffboardingRecord> offboardings) {
        List<OffboardingRecord> gaps = offboardings.stream()
                .filter(OffboardingRecord::hasGap)
                .sorted(Comparator.comparingLong(OffboardingRecord::daysSinceTermination).reversed())
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
<div class="section">
<h2>⚠ Offboarding Gaps (%d detected)</h2>
<table>
<thead><tr><th>Employee</th><th>Termination Date</th><th>Days Since</th><th>Type</th><th>Still Active On</th></tr></thead>
<tbody>
""", gaps.size()));

        gaps.stream().limit(20).forEach(r -> {
            sb.append(String.format(
                    "<tr><td><strong>%s</strong> (%s)</td><td>%s</td><td><span class=\"badge badge-critical\">%d days</span></td>" +
                    "<td>%s</td><td style='color:var(--critical)'>%s</td></tr>\n",
                    r.displayName(), r.employeeId(), r.terminationDate(), r.daysSinceTermination(),
                    r.terminationType(), String.join(", ", r.activePlatformsPostTermination())));
        });

        sb.append("</tbody></table></div>\n");
        return sb.toString();
    }

    private String behavioralAnomalies(BehavioralAnalyzer.AnalysisSummary summary) {
        if (summary == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
<div class="section">
<h2>📊 Behavioral Analysis (%d events analyzed)</h2>
<div class="cards">
<div class="card card-high"><div class="value">%d</div><div class="label">Privilege Spikes</div></div>
<div class="card card-medium"><div class="value">%d</div><div class="label">Off-Hours Access</div></div>
<div class="card card-critical"><div class="value">%d</div><div class="label">Cross-Platform Cascades</div></div>
<div class="card card-high"><div class="value">%d</div><div class="label">Token Misuse</div></div>
</div>
<table>
<thead><tr><th>Account</th><th>Events</th><th>Logins</th><th>Failures</th><th>Priv Changes</th><th>Off-Hours</th><th>Anomaly Score</th><th>Anomalies</th></tr></thead>
<tbody>
""", summary.totalEventsAnalyzed(), summary.privilegeSpikes(), summary.offHoursAccess(),
                summary.crossPlatformCascades(), summary.tokenMisuse()));

        summary.topRiskyProfiles().stream().limit(15).forEach(p -> {
            String sevClass = p.anomalyScore() >= 0.6 ? "critical" : p.anomalyScore() >= 0.3 ? "high" : "medium";
            sb.append(String.format(
                    "<tr><td><code>%s</code></td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td>" +
                    "<td><span class=\"badge badge-%s\">%.0f%%</span></td><td>%s</td></tr>\n",
                    p.accountId(), p.totalEvents(), p.loginSuccesses(), p.loginFailures(),
                    p.privilegeChanges(), p.offHoursEvents(), sevClass, p.anomalyScore() * 100,
                    String.join("; ", p.anomalies().stream().map(a -> truncate(a, 50)).toList())));
        });

        sb.append("</tbody></table></div>\n");
        return sb.toString();
    }

    private String privilegeHeatmap(List<UnifiedIdentity> identities, GroupHierarchy hierarchy) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
<div class="section">
<h2>🔥 Cross-Platform Privilege Heatmap</h2>
<div class="heatmap">
<div class="heatmap-cell heatmap-header">Identity</div>
<div class="heatmap-cell heatmap-header">AD</div>
<div class="heatmap-cell heatmap-header">AWS</div>
<div class="heatmap-cell heatmap-header">Okta</div>
<div class="heatmap-cell heatmap-header">Salesforce</div>
<div class="heatmap-cell heatmap-header">ServiceNow</div>
""");

        identities.stream()
                .sorted(Comparator.comparingDouble(UnifiedIdentity::computeRiskScore).reversed())
                .limit(20)
                .forEach(id -> {
                    EffectivePrivilegeProfile prof = EffectivePrivilegeProfile.fromIdentity(id, hierarchy);
                    sb.append(String.format("<div class=\"heatmap-cell\" style=\"text-align:left;font-weight:600\">%s</div>\n",
                            truncate(id.displayName(), 20)));

                    for (String platform : new String[]{"ActiveDirectory", "AWS_IAM", "Okta", "Salesforce", "ServiceNow"}) {
                        List<String> perms = prof.platformPrivileges().getOrDefault(platform, List.of());
                        boolean isAdmin = prof.adminPlatforms().contains(platform);
                        int heat = perms.isEmpty() ? -1 : (isAdmin ? 3 : (perms.size() > 5 ? 2 : (perms.size() > 0 ? 1 : 0)));
                        String cls = heat < 0 ? "heatmap-cell" : "heatmap-cell heat-" + heat;
                        String label = heat < 0 ? "—" : (isAdmin ? "ADMIN (" + perms.size() + ")" : perms.size() + " perms");
                        sb.append(String.format("<div class=\"%s\">%s</div>\n", cls, label));
                    }
                });

        sb.append("</div></div>\n");
        return sb.toString();
    }

    private String complianceAlignment() {
        return """
<div class="section">
<h2>📋 Framework Compliance Alignment</h2>
<div class="compliance-grid">
<div class="compliance-card">
<h3>NIST SP 800-53</h3>
<ul>
<li>AC-2: Account Management (lifecycle, dormancy, removal)</li>
<li>AC-5: Separation of Duties</li>
<li>AC-6: Least Privilege (minimum necessary access)</li>
<li>AC-6(5): Privileged Accounts</li>
<li>IA-4: Identifier Management (unique identity tracking)</li>
<li>IA-5(1): Password/Key Rotation</li>
</ul>
</div>
<div class="compliance-card">
<h3>MITRE ATT&CK</h3>
<ul>
<li>T1078: Valid Accounts (credential abuse)</li>
<li>T1098: Account Manipulation (privilege escalation)</li>
<li>T1136: Create Account (unauthorized accounts)</li>
<li>T1550: Use Alternate Authentication Material (token misuse)</li>
<li>T1531: Account Access Removal</li>
</ul>
</div>
<div class="compliance-card">
<h3>GDPR</h3>
<ul>
<li>Article 5: Data Minimisation (access scope ∝ purpose)</li>
<li>Article 32: Security of Processing (identity controls)</li>
</ul>
</div>
<div class="compliance-card">
<h3>CIS Controls</h3>
<ul>
<li>Control 5: Account Management</li>
<li>Control 6: Access Control Management</li>
</ul>
</div>
</div>
</div>
""";
    }

    private String dataDict() {
        return """
<div class="section">
<h2>📖 Data Dictionary</h2>
<table>
<thead><tr><th>Entity</th><th>Fields</th><th>Description</th></tr></thead>
<tbody>
<tr><td><code>IdentityAccount</code></td><td>accountId, platform, displayName, email, employeeId, status, roles, groups, isAdmin, lastLogin, createdAt, metadata</td><td>Single account on a specific identity platform</td></tr>
<tr><td><code>UnifiedIdentity</code></td><td>unifiedId, displayName, email, employeeId, department, accounts, metadata</td><td>Correlated identity across multiple platforms</td></tr>
<tr><td><code>AuditEvent</code></td><td>eventId, accountId, platform, eventType, description, timestamp, sourceIp, targetResource, success, metadata</td><td>Login, privilege change, resource access event</td></tr>
<tr><td><code>OffboardingRecord</code></td><td>employeeId, displayName, terminationDate, platformDisableStatus, terminationType</td><td>HR termination record with per-platform disable tracking</td></tr>
<tr><td><code>GroupHierarchy</code></td><td>groupName, platform, parentGroups, grantedPermissions, isAdminGroup</td><td>Nested group/role hierarchy for effective privilege resolution</td></tr>
<tr><td><code>EffectivePrivilegeProfile</code></td><td>identityId, platformPrivileges, adminPlatforms, sensitivePermissions, totalPermissionCount, privilegeRiskScore, inheritanceChains</td><td>Computed effective privilege after group traversal</td></tr>
<tr><td><code>KnowledgeUnit</code></td><td>id, statement, type, state, context, evidence, confidence, lastReviewed</td><td>Core knowledge store entity (risk, pattern, recommendation)</td></tr>
<tr><td><code>RiskIndicator</code></td><td>riskType, severity, description, riskScore</td><td>Risk signal with MITRE ATT&CK and NIST mapping</td></tr>
</tbody>
</table>
</div>
""";
    }

    private String footer() {
        return String.format("""
<div class="gen-time">
Generated by IKOS v2.0 (Identity Knowledge Operating System) | Powered by Spring AI Agent Utils<br>
Report generated: %s
</div>
</div>
</body>
</html>
""", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractMitreFromContext(Object context) {
        if (context == null) return "";
        String ctx = context.toString();
        for (String risk : new String[]{"OFFBOARDING_GAP", "CROSS_PLATFORM_ADMIN", "DORMANT_ADMIN",
                "PRIVILEGE_SPIKE", "TOKEN_ABUSE", "ORPHANED_ACCOUNT", "SOD_VIOLATION"}) {
            if (ctx.contains(risk)) {
                try {
                    return RiskType.valueOf(risk).mitreRef();
                } catch (Exception e) { /* ignore */ }
            }
        }
        return "";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
