package org.springaicommunity.agent.ikos.examples;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springaicommunity.agent.ikos.Ikos;
import org.springaicommunity.agent.ikos.audit.AuditLogger;
import org.springaicommunity.agent.ikos.audit.AuditLogger.AuditAction;
import org.springaicommunity.agent.ikos.audit.FileAuditLogger;
import org.springaicommunity.agent.ikos.connector.SimulatedDataSource;
import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.risk.RiskDeduplicationEngine;

import org.springaicommunity.agent.ikos.autoconfigure.IkosAutoConfiguration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * IKOS Risk Detection Demo — no LLM required.
 *
 * <p>Demonstrates the full risk detection pipeline including enterprise features:
 * <pre>
 *   Data Sources → Correlation → Risk Detection → Deduplication
 *   → Pattern Discovery → Behavioral Analysis → Alert Consolidation
 *   → Governance Review → Dashboard → Audit Trail
 * </pre>
 *
 * <pre>
 *   mvn spring-boot:run
 * </pre>
 */
@SpringBootApplication(exclude = IkosAutoConfiguration.class)
public class IkosRiskDetectionDemoApplication {

    static final String RESET = "\033[0m";
    static final String BOLD = "\033[1m";
    static final String CYAN = "\033[0;36m";
    static final String GREEN = "\033[0;32m";
    static final String YELLOW = "\033[0;33m";
    static final String RED = "\033[0;31m";
    static final String MAGENTA = "\033[0;35m";
    static final String DIM = "\033[2m";

    public static void main(String[] args) {
        SpringApplication.run(IkosRiskDetectionDemoApplication.class, args);
    }

    @Bean
    CommandLineRunner demo() {
        return args -> {
            String storagePath = System.getProperty("user.home") + "/.ikos-risk-demo";

            System.out.println();
            System.out.println(RED + BOLD + "  ⛨  IKOS Risk Detection Demo" + RESET);
            System.out.println(RED + "  ─────────────────────────────────────────────────" + RESET);
            System.out.println(DIM + "  Enterprise Edition — Audit Trail, Dedup, Data Sources, REST API" + RESET);
            System.out.println();

            // ── Single builder call wires ALL components ─────────────────
            Ikos ikos = Ikos.builder()
                    .storagePath(storagePath)
                    .auditEnabled(true)
                    .dataSources(List.of(new SimulatedDataSource(200)))
                    .build();

            // ── Step 1: Enterprise Data Sources ──────────────────────────
            step("1", "Connecting identity data sources");
            var sources = ikos.dataAggregator().registeredSources();
            for (String src : sources) {
                System.out.println("    " + GREEN + "⬤" + RESET + " " + src);
            }
            ikos.auditLogger().log("IKOS-Demo", AuditAction.SCAN_STARTED, "demo-scan",
                    Map.of("sources", sources.size()));

            // ── Step 2: Fetch accounts from data sources ─────────────────
            step("2", "Fetching identities from " + sources.size() + " data source(s)");
            var accounts = ikos.dataAggregator().fetchAllAccounts();
            System.out.println("    " + BOLD + accounts.size() + RESET + " accounts fetched");
            ikos.auditLogger().log("IKOS-Demo", AuditAction.TOOL_CALL, "fetchAccounts",
                    Map.of("accountCount", accounts.size()));

            // ── Step 3: Correlate identities ─────────────────────────────
            step("3", "Correlating cross-platform identities");
            List<UnifiedIdentity> identities = ikos.correlationEngine().correlate(accounts);
            System.out.println("    " + BOLD + identities.size() + RESET + " unified identities resolved from "
                    + accounts.size() + " accounts");

            // ── Step 4: Detect risks ─────────────────────────────────────
            step("4", "Scanning for security risks");
            List<KnowledgeUnit> risks = ikos.riskEngine().detectRisks(identities);
            System.out.println("    " + RED + BOLD + risks.size() + RESET + " raw risks detected");
            for (KnowledgeUnit risk : risks) {
                ikos.storage().saveKnowledgeUnit(risk);
                ikos.pipeline().createObservation(risk.id() + "-OBS", risk.statement(),
                        risk.context() != null ? risk.context().toString() : "", "RiskDetectionEngine");
                System.out.println("    " + RED + "⚠" + RESET + " " + truncate(risk.statement(), 70));
            }
            ikos.auditLogger().log("IKOS-Demo", AuditAction.RISK_DETECTED, "scan",
                    Map.of("rawRisks", risks.size()));

            // ── Step 5: Enterprise — Deduplication ───────────────────────
            step("5", MAGENTA + "Enterprise" + RESET + BOLD + " — Risk deduplication (fingerprint-based)");
            // Simulate running detection twice (common in enterprise scheduled scans)
            List<KnowledgeUnit> secondScan = ikos.riskEngine().detectRisks(identities);
            var combined = new java.util.ArrayList<>(risks);
            combined.addAll(secondScan);
            var dedupResult = ikos.deduplicationEngine().deduplicate(combined);
            System.out.printf("    %s%.0f%%%s duplicate reduction (%d raw from 2 scans → %s%d unique%s)%n",
                    GREEN, dedupResult.reductionPercentage(), RESET,
                    dedupResult.originalCount(),
                    BOLD, dedupResult.deduplicatedCount(), RESET);
            System.out.println("    " + DIM + "Fingerprint: SHA-256(identity + risk_type + platforms)" + RESET);

            // ── Step 6: Privilege analysis ───────────────────────────────
            // Use simulated data for group hierarchy
            var simData = new SimulatedDataSource(200).getData();
            step("6", "Analyzing effective privileges via nested group traversal");
            int highRiskCount = 0;
            for (UnifiedIdentity uid : identities) {
                EffectivePrivilegeProfile prof = EffectivePrivilegeProfile.fromIdentity(uid, simData.groupHierarchy());
                if (prof.privilegeRiskScore() >= 0.5) {
                    highRiskCount++;
                    if (highRiskCount <= 5) { // Show top 5
                        System.out.printf("    %s● %s%-25s%s Risk: %s%.0f%%%s  Admin: %d platform(s)%n",
                                RED, BOLD, uid.displayName(), RESET,
                                RED, prof.privilegeRiskScore() * 100, RESET,
                                prof.adminPlatforms().size());
                    }
                }
            }
            if (highRiskCount > 5) {
                System.out.println("    " + DIM + "... and " + (highRiskCount - 5) + " more" + RESET);
            }

            // ── Step 7: Auto-discover patterns ───────────────────────────
            step("7", "Auto-discovering patterns from observations");
            ikos.pipeline().autoDiscoverPatterns();
            List<KnowledgeUnit> allPatterns = ikos.pipeline().getPatternCandidates();
            System.out.println("    " + BOLD + allPatterns.size() + RESET + " pattern candidate(s) discovered");

            // ── Step 8: Behavioral analysis ──────────────────────────────
            step("8", "Behavioral analysis on " + simData.auditEvents().size() + " audit events");
            var behavioral = ikos.behavioralAnalyzer().analyze(simData.auditEvents());
            System.out.println("    Anomalies: " + RED + behavioral.anomaliesDetected() + RESET
                    + " | Privilege spikes: " + YELLOW + behavioral.privilegeSpikes() + RESET
                    + " | Off-hours: " + YELLOW + behavioral.offHoursAccess() + RESET);

            // ── Step 9: Alert consolidation ──────────────────────────────
            step("9", "Consolidating " + dedupResult.deduplicatedCount() + " unique alerts");
            var consolidation = ikos.consolidationEngine().consolidate(dedupResult.uniqueRisks());
            System.out.printf("    %s%.0f%%%s alert noise reduction (%d unique → %d consolidated)%n",
                    GREEN, consolidation.reductionPercentage(), RESET,
                    consolidation.originalAlertCount(), consolidation.consolidatedAlertCount());

            // ── Step 10: Auto-promote patterns ──────────────────────────
            step("10", "Governance review — auto-promoting validated patterns");
            int promoted = 0;
            for (KnowledgeUnit pattern : allPatterns) {
                if (pattern.confidence() >= 0.5) {
                    var candidate = ikos.promotionEngine().nominate(pattern, "IKOS-AutoReview");
                    ikos.promotionEngine().approve(candidate, "IKOS-GovernanceEngine");
                    ikos.auditLogger().log("IKOS-GovernanceEngine", AuditAction.PROMOTE,
                            pattern.id(), Map.of("confidence", pattern.confidence()));
                    promoted++;
                }
            }
            System.out.println("    " + GREEN + promoted + RESET + " pattern(s) promoted to Security Knowledge");

            // ── Step 11: Generate HTML dashboard ─────────────────────────
            step("11", "Generating interactive HTML dashboard");
            String html = ikos.dashboardGenerator().generate(
                    identities, dedupResult.uniqueRisks(),
                    simData.offboardingRecords(), simData.temporaryExceptions(),
                    behavioral, simData.groupHierarchy(), simData.dataStats());
            String reportPath = storagePath + "/ikos-dashboard.html";
            try (FileWriter fw = new FileWriter(reportPath)) {
                fw.write(html);
                System.out.println("    " + GREEN + "✓" + RESET + " Dashboard: " + BOLD + "file://" + reportPath + RESET);
            } catch (IOException e) {
                System.out.println("    " + RED + "✗" + RESET + " Failed: " + e.getMessage());
            }

            // ── Step 12: Enterprise — Audit Trail Summary ────────────────
            ikos.auditLogger().log("IKOS-Demo", AuditAction.SCAN_COMPLETED, "demo-scan",
                    Map.of("risks", dedupResult.deduplicatedCount(), "patterns", allPatterns.size(),
                            "promoted", promoted));

            step("12", MAGENTA + "Enterprise" + RESET + BOLD + " — Audit trail summary");
            var auditEntries = ikos.auditLogger().recent(20);
            System.out.println("    " + BOLD + auditEntries.size() + RESET + " audit entries recorded this session");
            for (var entry : auditEntries.reversed()) {
                String icon = switch (entry.action()) {
                    case SCAN_STARTED -> "🔍";
                    case SCAN_COMPLETED -> "✅";
                    case RISK_DETECTED -> "⚠️ ";
                    case PROMOTE -> "⬆️ ";
                    case TOOL_CALL -> "🔧";
                    default -> "📝";
                };
                System.out.printf("    %s %s%-18s%s %s → %s%s%s%n",
                        icon, DIM, entry.timestamp().toLocalTime().withNano(0), RESET,
                        entry.action(), CYAN, entry.targetId(), RESET);
            }
            if (ikos.auditLogger() instanceof FileAuditLogger fal) {
                System.out.println("    " + DIM + "Persisted to: " + storagePath + "/audit/audit-trail.jsonl" + RESET);
            }

            // ── Final Summary ────────────────────────────────────────────
            System.out.println();
            System.out.println("  " + GREEN + BOLD + "═══════════════════════════════════════════════════" + RESET);
            System.out.println("  " + GREEN + BOLD + "  ✓ IKOS Risk Detection complete — 12 stages!" + RESET);
            System.out.println("  " + GREEN + BOLD + "═══════════════════════════════════════════════════" + RESET);
            System.out.println();
            System.out.printf("  %sData Sources:%s  %d connected%n", BOLD, RESET, sources.size());
            System.out.printf("  %sAccounts:%s      %d fetched%n", BOLD, RESET, accounts.size());
            System.out.printf("  %sIdentities:%s    %d unified%n", BOLD, RESET, identities.size());
            System.out.printf("  %sRaw Risks:%s     %d detected%n", BOLD, RESET, risks.size());
            System.out.printf("  %sAfter Dedup:%s   %s%d unique%s (%.0f%% reduction)%n",
                    BOLD, RESET, GREEN, dedupResult.deduplicatedCount(), RESET,
                    dedupResult.reductionPercentage());
            System.out.printf("  %sConsolidated:%s  %d actionable alerts%n", BOLD, RESET,
                    consolidation.consolidatedAlertCount());
            System.out.printf("  %sPatterns:%s      %d discovered, %d promoted%n",
                    BOLD, RESET, allPatterns.size(), promoted);
            System.out.printf("  %sAudit Trail:%s   %d entries persisted%n", BOLD, RESET, auditEntries.size());
            System.out.println();
        };
    }

    private static void step(String num, String msg) {
        System.out.println();
        System.out.println("  " + CYAN + "Step " + num + RESET + " — " + BOLD + msg + RESET);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

}
