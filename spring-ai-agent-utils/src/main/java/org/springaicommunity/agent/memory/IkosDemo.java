/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory;

import org.springaicommunity.agent.memory.context.ContextPackage;
import org.springaicommunity.agent.memory.context.DefaultContextAssembler;
import org.springaicommunity.agent.memory.identity.DefaultIdentityCorrelationEngine;
import org.springaicommunity.agent.memory.model.*;
import org.springaicommunity.agent.memory.outcome.InMemoryOutcomeTracker;
import org.springaicommunity.agent.memory.outcome.OutcomeLearningEngine;
import org.springaicommunity.agent.memory.promotion.DefaultPromotionEngine;
import org.springaicommunity.agent.memory.risk.DefaultRiskDetectionEngine;
import org.springaicommunity.agent.memory.storage.FileMemoryStorage;
import org.springaicommunity.agent.memory.tools.EngineeringMemoryTools;
import org.springaicommunity.agent.memory.tools.IdentityGovernanceTools;
import org.springaicommunity.agent.memory.advisors.KnowledgeEvolutionAdvisor;
import org.springaicommunity.agent.tools.AutoMemoryTools;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ListDirectoryTool;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springaicommunity.agent.tools.task.repository.BackgroundTask;
import org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor;
import org.springaicommunity.agent.utils.AgentEnvironment;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
import org.springaicommunity.agent.utils.MarkdownParser;
import org.springaicommunity.agent.utils.Skills;
import org.springaicommunity.agent.memory.simulation.SimulatedDataGenerator;
import org.springaicommunity.agent.memory.risk.BehavioralAnalyzer;
import org.springaicommunity.agent.memory.report.InteractiveDashboardGenerator;
import org.springaicommunity.agent.memory.risk.AlertConsolidationEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Interactive CLI demo for the Identity Knowledge Operating System (IKOS).
 *
 * <p>
 * Demonstrates the full IKOS lifecycle:
 * 
 * <pre>
 *   Identity Event → Risk Detection → Knowledge Extraction → Pattern Discovery
 *   → Governance Review → Knowledge Evolution → Explainable Recommendations
 * </pre>
 *
 * <p>
 * Run with:
 * 
 * <pre>
 *   mvn exec:java -Dexec.mainClass="org.springaicommunity.agent.memory.IkosDemo"
 * </pre>
 *
 * @author Antigravity
 */
public class IkosDemo {

    static final String RESET = "\033[0m";
    static final String BOLD = "\033[1m";
    static final String CYAN = "\033[0;36m";
    static final String GREEN = "\033[0;32m";
    static final String YELLOW = "\033[0;33m";
    static final String RED = "\033[0;31m";
    static final String BLUE = "\033[0;34m";
    static final String PURPLE = "\033[0;35m";
    static final String WHITE = "\033[0;37m";

    private final FileMemoryStorage storage;
    private final ApplicationMemory appMemory;
    private final GovernanceMemory govMemory;
    private final KnowledgeEvolutionPipeline pipeline;
    private final DefaultPromotionEngine promotionEngine;
    private final DefaultContextAssembler assembler;
    private final OutcomeLearningEngine learningEngine;
    private final DefaultIdentityCorrelationEngine correlationEngine;
    private final DefaultRiskDetectionEngine riskEngine;
    private final IdentityGovernanceTools governanceTools;
    private final EngineeringMemoryTools engineeringTools;
    private final ContextBuilder contextBuilder;
    private final AutoMemoryTools autoMemoryTools;
    private final KnowledgeEvolutionAdvisor advisor;
    private final AskUserQuestionTool askUserTool;
    private final TodoWriteTool todoTool;
    private final DefaultTaskRepository taskRepository;
    private final ShellTools shellTools;
    private final GrepTool grepTool;
    private final GlobTool globTool;
    private final FileSystemTools fileSystemTools;
    private final ListDirectoryTool listDirTool;
    private final BehavioralAnalyzer behavioralAnalyzer;
    private final InteractiveDashboardGenerator reportGenerator;
    private final AlertConsolidationEngine consolidationEngine;
    private final Scanner scanner;
    private final String storageRoot;

    public IkosDemo(String storageRoot) {
        this.storage = new FileMemoryStorage(storageRoot);
        this.appMemory = new ApplicationMemory(storage);
        this.govMemory = new GovernanceMemory(storage);
        this.pipeline = new KnowledgeEvolutionPipeline(storage);
        this.promotionEngine = new DefaultPromotionEngine(storage);
        this.assembler = new DefaultContextAssembler(appMemory, govMemory);
        this.learningEngine = new OutcomeLearningEngine(storage, new InMemoryOutcomeTracker());
        this.correlationEngine = new DefaultIdentityCorrelationEngine();
        this.riskEngine = new DefaultRiskDetectionEngine();
        this.governanceTools = new IdentityGovernanceTools(storage, pipeline, correlationEngine, riskEngine);
        this.engineeringTools = new EngineeringMemoryTools(storage, pipeline, appMemory, govMemory);
        this.contextBuilder = new ContextBuilder(appMemory, govMemory);
        this.autoMemoryTools = AutoMemoryTools.builder().memoriesDir(storageRoot + "/memories").build();
        this.advisor = KnowledgeEvolutionAdvisor.builder()
                .contextBuilder(contextBuilder)
                .maxRetrievedUnits(10)
                .order(100)
                .build();
        this.askUserTool = AskUserQuestionTool.builder()
                .questionHandler(new CommandLineQuestionHandler())
                .answersValidation(false)
                .build();
        this.todoTool = TodoWriteTool.builder()
                .todoEventHandler(todos -> {
                    System.out.println("  " + BOLD + "Remediation Task List Updated:" + RESET);
                    for (var item : todos.todos()) {
                        String icon = switch (item.status()) {
                            case pending -> YELLOW + "○";
                            case in_progress -> CYAN + "◑";
                            case completed -> GREEN + "●";
                        };
                        System.out.println("    " + icon + RESET + " [" + item.status() + "] " + item.content());
                    }
                })
                .build();
        this.taskRepository = new DefaultTaskRepository();
        this.shellTools = ShellTools.builder().build();
        this.grepTool = GrepTool.builder()
                .workingDirectory(storageRoot)
                .build();
        this.globTool = GlobTool.builder()
                .workingDirectory(storageRoot)
                .build();
        this.fileSystemTools = FileSystemTools.builder()
                .allowedDirectory(storageRoot)
                .build();
        this.listDirTool = ListDirectoryTool.builder().build();
        this.behavioralAnalyzer = new BehavioralAnalyzer();
        this.reportGenerator = new InteractiveDashboardGenerator();
        this.consolidationEngine = new AlertConsolidationEngine();
        this.storageRoot = storageRoot;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        String storageRoot = System.getProperty("ikos.storage",
                System.getProperty("user.home") + "/.ikos-demo");
        new File(storageRoot).mkdirs();

        banner();
        System.out.println(CYAN + "  Storage: " + storageRoot + RESET);
        System.out.println();

        // Show agent environment info (AgentEnvironment utility)
        System.out.println(
                BOLD + "  Agent Environment" + RESET + " (org.springaicommunity.agent.utils.AgentEnvironment):");
        for (String line : AgentEnvironment.info().split("\n")) {
            System.out.println("    " + WHITE + line + RESET);
        }
        System.out.println();

        IkosDemo demo = new IkosDemo(storageRoot);
        demo.run();
    }

    // ── Main loop ────────────────────────────────────────────────────────────

    private void run() {
        while (true) {
            printMenu();
            String choice = prompt("Choose").trim();
            System.out.println();
            switch (choice) {
                case "1" -> demoFullLifecycle();
                case "2" -> interactiveRiskDetection();
                case "3" -> interactiveIncident();
                case "4" -> interactiveRemediation();
                case "5" -> viewPatternCandidates();
                case "6" -> interactivePromotion();
                case "7" -> interactiveContextAssembly();
                case "8" -> interactiveOutcome();
                case "9" -> listAllKnowledge();
                case "0" -> complianceDashboard();
                case "a", "A" -> auditTrail();
                case "i", "I" -> aiAgentQuery();
                case "s", "S" -> springAiShowcase();
                case "q", "Q" -> {
                    bye();
                    return;
                }
                default -> warn("Unknown option — try again.");
            }
            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println(BOLD + RED + "━━━ IKOS — Identity Knowledge Operating System ━━━━━━━" + RESET);
        System.out.println("  " + CYAN + "1" + RESET + "  Run full IKOS lifecycle demo (automated)");
        System.out.println("  " + CYAN + "2" + RESET + "  Detect identity risks (offboarding gap / dormant admin)");
        System.out.println("  " + CYAN + "3" + RESET + "  Record a security incident");
        System.out.println("  " + CYAN + "4" + RESET + "  Record a remediation action");
        System.out.println("  " + CYAN + "5" + RESET + "  View auto-discovered pattern candidates");
        System.out.println("  " + CYAN + "6" + RESET + "  Nominate + approve a promotion (governance review)");
        System.out.println("  " + CYAN + "7" + RESET + "  Assemble context for identity governance query");
        System.out.println("  " + CYAN + "8" + RESET + "  Record an outcome and learn");
        System.out.println("  " + CYAN + "9" + RESET + "  List all IKOS knowledge units");
        System.out.println(
                "  " + CYAN + "0" + RESET + "  " + BOLD + "Compliance Dashboard" + RESET + " (risk heatmap + scores)");
        System.out.println(
                "  " + CYAN + "a" + RESET + "  " + BOLD + "Audit Trail" + RESET + " (knowledge evolution timeline)");
        System.out.println(
                "  " + CYAN + "i" + RESET + "  " + BOLD + "AI Agent Query" + RESET + " (Spring AI tool-calling demo)");
        System.out.println("  " + CYAN + "s" + RESET + "  " + BOLD + "Spring AI Integration" + RESET
                + " (framework component showcase)");
        System.out.println("  " + CYAN + "q" + RESET + "  Quit");
        System.out.println(RED + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
    }

    // ── Option 1: Full automated lifecycle ───────────────────────────────────

    private void demoFullLifecycle() {
        header("Full IKOS Lifecycle: Identity Event → Risk → Knowledge → Recommendation");

        // ═══════ WAVE 1: Generate simulated data at scale ═══════
        step("1", "SimulatedDataGenerator — generating 100 identities across 5 platforms");
        SimulatedDataGenerator generator = new SimulatedDataGenerator();
        SimulatedDataGenerator.GeneratedData data = generator.generate(200);
        List<IdentityAccount> accounts = data.accounts();
        GroupHierarchy hierarchy = data.groupHierarchy();

        System.out.println("  " + BOLD + "Dataset Statistics:" + RESET);
        for (Map.Entry<String, String> e : data.dataStats().entrySet()) {
            System.out.println("    " + GREEN + "✓" + RESET + " " + e.getKey() + ": " + BOLD + e.getValue() + RESET);
        }
        ok("Generated " + accounts.size() + " accounts from 5 platforms (AD, AWS, Okta, Salesforce, ServiceNow)");
        pause();

        // ═══════ Step 2: Correlate identities ═══════
        step("2", "Identity Correlation Engine — resolve cross-platform identities");
        List<UnifiedIdentity> identities = correlationEngine.correlate(accounts);
        for (UnifiedIdentity id : identities) {
            String riskBadge = id.computeRiskScore() >= 0.5 ? RED + " ▲ HIGH RISK" + RESET : "";
            System.out.println("  " + GREEN + "✓" + RESET + " " + BOLD + id.displayName() + RESET
                    + " (" + id.unifiedId() + ") — " + id.platforms().size() + " platforms: "
                    + String.join(", ", id.platforms()) + riskBadge);
        }
        ok(identities.size() + " unified identities resolved from " + accounts.size() + " accounts");
        pause();

        // ═══════ Step 3: Detect risks ═══════
        step("3", "Risk Detection Engine — scanning for security risks");
        List<KnowledgeUnit> risks = riskEngine.detectRisks(identities);
        ok("Detected " + risks.size() + " risk(s):");
        for (KnowledgeUnit risk : risks) {
            storage.saveKnowledgeUnit(risk);
            pipeline.createObservation(risk.id() + "-OBS", risk.statement(),
                    risk.context() != null ? risk.context().toString() : "", "RiskDetectionEngine");
            printRisk(risk);
        }
        pause();

        // ═══════ Step 4: Privilege Analysis with Group Hierarchy ═══════
        step("4", "Privilege Intelligence — effective permissions via nested group traversal");
        System.out.println("  " + BOLD + "Group Hierarchy: " + RESET + hierarchy.size() + " groups across AD, AWS, Okta, Salesforce, ServiceNow");
        for (UnifiedIdentity uid : identities) {
            EffectivePrivilegeProfile prof = EffectivePrivilegeProfile.fromIdentity(uid, hierarchy);
            String color = prof.privilegeRiskScore() >= 0.5 ? RED : prof.privilegeRiskScore() >= 0.3 ? YELLOW : GREEN;
            System.out.printf(
                    "  %s● %s%-25s%s Perms: %-3d  Sensitive: %-2d  Admin on: %d platform(s)  Risk: %s%.0f%%%s%n",
                    color, BOLD, uid.displayName(), RESET,
                    prof.totalPermissionCount(), prof.sensitivePermissions().size(),
                    prof.adminPlatforms().size(), color, prof.privilegeRiskScore() * 100, RESET);
            if (prof.hasHiddenAdmin()) {
                for (String chain : prof.inheritanceChains()) {
                    System.out.println("    " + RED + "⚠ Hidden Admin: " + RESET + chain);
                }
            }
        }
        pause();

        // ═══════ Step 5: Auto-discover patterns ═══════
        step("5", "Knowledge Evolution — auto-discover patterns from risk observations");
        // Patterns are auto-discovered during Step 3's createObservation() calls.
        // Retrieve them here + run one more discovery pass for any stragglers.
        List<KnowledgeUnit> newPatterns = pipeline.autoDiscoverPatterns();
        List<KnowledgeUnit> allPatterns = pipeline.getPatternCandidates();
        if (!newPatterns.isEmpty()) {
            allPatterns = new ArrayList<>(allPatterns);
            for (KnowledgeUnit np : newPatterns) {
                if (allPatterns.stream().noneMatch(p -> p.id().equals(np.id()))) {
                    allPatterns.add(np);
                }
            }
        }
        if (allPatterns.isEmpty()) {
            warn("No pattern candidates discovered yet (need more observations).");
        } else {
            ok("Discovered " + allPatterns.size() + " pattern candidate(s):");
            for (KnowledgeUnit p : allPatterns) {
                printUnit(p);
            }
        }
        pause();

        // ═══════ Step 6: Generate recommendations ═══════
        step("6", "Generate explainable recommendations");
        for (KnowledgeUnit risk : risks) {
            printRecommendation(risk);
        }
        pause();

        // ═══════ Step 7: Context assembly ═══════
        step("7", "Assemble context for agent query: 'dormant admin offboarding risk'");
        ContextPackage pkg = assembler.assemble("dormant admin offboarding privilege creep contractor risk", 10);
        printContext(pkg);
        pause();

        // ═══════ Step 8: Simulate remediation + outcome learning ═══════
        step("8", "Remediation & Outcome Learning — closing the loop (TodoWriteTool)");

        // Create remediation task list via TodoWriteTool
        List<TodoWriteTool.Todos.TodoItem> taskItems = new ArrayList<>();
        for (KnowledgeUnit risk : risks) {
            taskItems.add(new TodoWriteTool.Todos.TodoItem(
                    "Remediate: " + truncate(risk.statement(), 50),
                    TodoWriteTool.Todos.Status.pending,
                    "Remediating " + risk.id()));
        }
        todoTool.todoWrite(new TodoWriteTool.Todos(taskItems));
        System.out.println();

        int remCount = 0;
        for (int ri = 0; ri < risks.size(); ri++) {
            KnowledgeUnit risk = risks.get(ri);
            boolean success = risk.confidence() >= 0.8;
            Outcome outcome = new Outcome(success,
                    success ? "Remediated: account disabled, tokens revoked." : "Partial: escalated to security team.",
                    LocalDateTime.now());
            Optional<KnowledgeUnit> updated = learningEngine.learn(risk.id(), outcome);
            if (updated.isPresent()) {
                remCount++;
                // Update task status via TodoWriteTool
                List<TodoWriteTool.Todos.TodoItem> updatedTasks = new ArrayList<>();
                for (int j = 0; j < risks.size(); j++) {
                    TodoWriteTool.Todos.Status status = j < ri ? TodoWriteTool.Todos.Status.completed
                            : j == ri
                                    ? (success ? TodoWriteTool.Todos.Status.completed
                                            : TodoWriteTool.Todos.Status.in_progress)
                                    : TodoWriteTool.Todos.Status.pending;
                    updatedTasks.add(new TodoWriteTool.Todos.TodoItem(
                            "Remediate: " + truncate(risks.get(j).statement(), 50), status,
                            "Remediating " + risks.get(j).id()));
                }
                todoTool.todoWrite(new TodoWriteTool.Todos(updatedTasks));

                String icon = success ? GREEN + "✓" : YELLOW + "◐";
                System.out.println("  " + icon + RESET + " " + risk.id() + " → Conf: " + f(updated.get().confidence())
                        + " | " + (success ? "REMEDIATED" : "ESCALATED"));
            }
        }
        ok(remCount + " outcomes recorded. Knowledge confidence updated.");
        pause();

        // ═══════ Step 9: Show dashboard ═══════
        step("9", "Compliance Dashboard — organizational risk posture");
        complianceDashboard();
        pause();

        // ═══════ Step 10: Auto-promote patterns to Security Knowledge ═══════
        step("10", "Governance Review — auto-promote validated patterns to Security Knowledge");
        List<KnowledgeUnit> promotionCandidates = pipeline.getPatternCandidates();
        int promoted = 0;
        for (KnowledgeUnit pattern : promotionCandidates) {
            if (pattern.confidence() >= 0.5) {
                PromotionCandidate candidate = promotionEngine.nominate(pattern, "IKOS-AutoReview");
                KnowledgeUnit secKnowledge = promotionEngine.approve(candidate, "IKOS-GovernanceEngine");
                promoted++;
                System.out.println("  " + GREEN + "✓" + RESET + " " + BOLD + pattern.id() + RESET
                        + " → promoted to " + PURPLE + secKnowledge.type() + RESET
                        + " (" + secKnowledge.id() + ")");
                System.out.println("    " + WHITE + "Statement: " + RESET + truncate(secKnowledge.statement(), 70));
            } else {
                System.out.println("  " + YELLOW + "⊘" + RESET + " " + pattern.id()
                        + " — confidence " + f(pattern.confidence()) + " too low for auto-promotion");
            }
        }
        if (promoted > 0) {
            ok(promoted + " pattern(s) promoted to Security Knowledge via governance review.");
        } else {
            warn("No patterns met the auto-promotion threshold (≥0.500).");
        }

        System.out.println();
        // ═══════ Step 11: Behavioral Analysis ═══════
        step("11", "Behavioral Analysis — " + data.auditEvents().size() + " audit events");
        BehavioralAnalyzer.AnalysisSummary behavioral = behavioralAnalyzer.analyze(data.auditEvents());
        System.out.println("  " + BOLD + "Analysis Results:" + RESET);
        System.out.println("    Events analyzed:   " + BOLD + behavioral.totalEventsAnalyzed() + RESET);
        System.out.println("    Identities:        " + BOLD + behavioral.identitiesAnalyzed() + RESET);
        System.out.println("    Anomalies found:   " + RED + BOLD + behavioral.anomaliesDetected() + RESET);
        System.out.println("    Privilege spikes:  " + YELLOW + behavioral.privilegeSpikes() + RESET);
        System.out.println("    Off-hours access:  " + YELLOW + behavioral.offHoursAccess() + RESET);
        System.out.println("    Platform cascades: " + RED + behavioral.crossPlatformCascades() + RESET);
        System.out.println("    Token misuse:      " + RED + behavioral.tokenMisuse() + RESET);
        if (!behavioral.topRiskyProfiles().isEmpty()) {
            System.out.println("  " + BOLD + "Top Risky Accounts:" + RESET);
            for (var p : behavioral.topRiskyProfiles().stream().limit(5).toList()) {
                System.out.printf("    %s%-20s%s Score: %s%.0f%%%s  Anomalies: %s%n",
                        BOLD, p.accountId(), RESET, RED, p.anomalyScore() * 100, RESET,
                        String.join(", ", p.anomalies().stream().map(a -> truncate(a, 40)).toList()));
            }
        }
        // Store behavioral risks
        for (KnowledgeUnit br : behavioral.generatedRisks()) {
            storage.saveKnowledgeUnit(br);
        }
        ok(behavioral.generatedRisks().size() + " behavioral risk observations stored.");
        pause();

        // ═══════ Step 12: Offboarding Gaps (proactive) ═══════
        step("12", "Offboarding Gap Detection — " + data.offboardingRecords().size() + " termination records");
        List<OffboardingRecord> gaps = data.offboardingRecords().stream()
                .filter(OffboardingRecord::hasGap).toList();
        System.out.println("  " + RED + BOLD + gaps.size() + RESET + " offboarding gaps detected:");
        for (var gap : gaps.stream().limit(8).toList()) {
            System.out.printf("    %s⚠%s %s (EMP: %s) — terminated %s, still active on: %s%s%s%n",
                    RED, RESET, gap.displayName(), gap.employeeId(), gap.terminationDate(),
                    RED, String.join(", ", gap.activePlatformsPostTermination()), RESET);
        }
        if (gaps.size() > 8) System.out.println("    " + WHITE + "... and " + (gaps.size() - 8) + " more" + RESET);
        pause();

        // ═══════ Step 13: Stale Exception Detection (Case 4) ═══════
        step("13", "Stale Exception Detection — temporary admin privileges never revoked");
        List<TemporaryAccessException> staleExceptions = data.temporaryExceptions().stream()
                .filter(TemporaryAccessException::isStale).toList();
        List<KnowledgeUnit> staleRisks = riskEngine.detectStaleExceptions(data.temporaryExceptions());
        risks.addAll(staleRisks);
        System.out.println("  " + RED + BOLD + staleExceptions.size() + RESET + " stale exceptions detected (" + data.temporaryExceptions().size() + " total exceptions):");
        for (var exc : staleExceptions.stream().limit(5).toList()) {
            System.out.printf("    %s⚠%s %s — granted for '%s', expired %d days ago, still active on: %s%s%s%n",
                    RED, RESET, exc.displayName(), exc.reason(), exc.daysOverdue(),
                    RED, String.join(", ", exc.unrevokedPlatforms()), RESET);
        }
        for (KnowledgeUnit sr : staleRisks) {
            storage.saveKnowledgeUnit(sr);
        }
        pause();

        // ═══════ Step 14: Alert Consolidation ═══════
        step("14", "Alert Consolidation — reducing alert noise");
        AlertConsolidationEngine.ConsolidationResult consolidation = consolidationEngine.consolidate(risks);
        System.out.println("  " + GREEN + BOLD + String.format("%.0f%%", consolidation.reductionPercentage()) + RESET + " alert noise reduction");
        System.out.println("  " + WHITE + consolidation.originalAlertCount() + " raw alerts → " + consolidation.consolidatedAlertCount() + " consolidated incidents" + RESET);
        System.out.println("  " + (consolidation.meetsTarget() ? GREEN + "✓ Meets ≥40% target" : RED + "✗ Below 40% target") + RESET);
        for (var alert : consolidation.consolidatedAlerts().stream().limit(5).toList()) {
            System.out.printf("    [%s] %s — %d risk(s) grouped — %s%n",
                    alert.alertId(), alert.category(), alert.riskCount(), alert.severity());
        }
        pause();

        // ═══════ Step 15: Generate Interactive HTML Dashboard ═══════
        step("15", "Interactive HTML Dashboard — generating risk intelligence dashboard");
        String htmlReport = reportGenerator.generate(
                identities, risks, data.offboardingRecords(), data.temporaryExceptions(),
                behavioral, hierarchy, data.dataStats());
        String reportPath = storageRoot + "/ikos-dashboard.html";
        try (FileWriter fw = new FileWriter(reportPath)) {
            fw.write(htmlReport);
            ok("Dashboard saved to: " + BOLD + reportPath + RESET);
            System.out.println("    " + WHITE + "Open in browser: file://" + reportPath + RESET);
        } catch (IOException e) {
            warn("Failed to write dashboard: " + e.getMessage());
        }
        pause();

        System.out.println();
        ok(BOLD + "Full IKOS lifecycle complete — 15 stages!" + RESET + GREEN
                + " Knowledge evolves with every identity event — that is the moat." + RESET);
    }

    // ── Option 2: Interactive risk detection ─────────────────────────────────

    private void interactiveRiskDetection() {
        header("Detect Identity Risk");
        String type = prompt("  Risk type: 1=Offboarding Gap, 2=Dormant Admin");
        String name = prompt("  Identity display name");

        if ("1".equals(type)) {
            String disabled = prompt("  Platform where DISABLED (e.g. ActiveDirectory)");
            String active = prompt("  Platform where still ACTIVE (e.g. AWS_IAM)");
            String accountId = prompt("  Account ID on active platform");

            String statement = String.format("OFFBOARDING GAP: %s disabled on %s but active on %s (%s)",
                    name, disabled, active, accountId);
            KnowledgeUnit obs = pipeline.createObservation(
                    "RISK-OBG-" + System.currentTimeMillis() % 10000,
                    statement, "Risk: OFFBOARDING_GAP | CRITICAL | Policy: PAM-001, NIST AC-2",
                    disabled + " → " + active);
            ok("Risk recorded!");
            printUnit(obs);
        } else {
            String platform = prompt("  Platform (e.g. AWS_IAM)");
            String daysStr = prompt("  Days since last login");
            String statement = String.format("DORMANT ADMIN: %s has admin on %s, inactive %s days",
                    name, platform, daysStr);
            KnowledgeUnit obs = pipeline.createObservation(
                    "RISK-DRM-" + System.currentTimeMillis() % 10000,
                    statement, "Risk: DORMANT_ADMIN | HIGH | Policy: PAM-003, NIST AC-2(3)",
                    platform);
            ok("Risk recorded!");
            printUnit(obs);
        }
    }

    // ── Option 3: Security incident ──────────────────────────────────────────

    private void interactiveIncident() {
        header("Record Security Incident");
        String id = prompt("  Incident ID (e.g. INC-001)");
        String stmt = prompt("  What happened?");
        String ctx = prompt("  Affected systems and scope");
        String src = prompt("  Source (alert / log / reporter)");

        KnowledgeUnit inc = KnowledgeUnit.builder().id(id).statement(stmt)
                .type(KnowledgeType.SECURITY_INCIDENT).state(KnowledgeState.OBSERVATION)
                .context(ctx).evidenceStrings(List.of(src)).confidence(1.0)
                .lastReviewed(LocalDateTime.now()).build();
        storage.saveKnowledgeUnit(inc);
        ok("Security incident recorded!");
        printUnit(inc);
    }

    // ── Option 4: Remediation ────────────────────────────────────────────────

    private void interactiveRemediation() {
        header("Record Remediation Action");
        String id = prompt("  Remediation ID (e.g. REM-001)");
        String action = prompt("  Action taken");
        String riskId = prompt("  Related risk/incident ID");
        String success = prompt("  Successful? (y/n)").trim().toLowerCase();

        KnowledgeUnit rem = KnowledgeUnit.builder().id(id).statement(action)
                .type(KnowledgeType.REMEDIATION_ACTION).state(KnowledgeState.OBSERVATION)
                .context("Related: " + riskId).evidenceStrings(List.of(riskId))
                .confidence(success.equals("y") ? 0.9 : 0.4)
                .outcome(new Outcome(success.equals("y"), action, LocalDateTime.now()))
                .lastReviewed(LocalDateTime.now()).build();
        storage.saveKnowledgeUnit(rem);
        ok("Remediation recorded!");
        printUnit(rem);
    }

    // ── Option 5: View patterns ──────────────────────────────────────────────

    private void viewPatternCandidates() {
        header("Auto-Discovered Pattern Candidates");
        List<KnowledgeUnit> candidates = pipeline.getPatternCandidates();
        if (candidates.isEmpty()) {
            warn("No pattern candidates. Record more observations first.");
            return;
        }
        for (KnowledgeUnit c : candidates) {
            printUnit(c);
        }
    }

    // ── Option 6: Promotion ──────────────────────────────────────────────────

    private void interactivePromotion() {
        header("Governance Review — Promote to Security Knowledge");
        String sourceId = prompt("  Pattern/recommendation ID to promote");
        Optional<KnowledgeUnit> opt = storage.getKnowledgeUnit(sourceId);
        if (opt.isEmpty()) {
            warn("Not found: " + sourceId);
            return;
        }

        KnowledgeUnit unit = opt.get();
        printUnit(unit);
        PromotionCandidate candidate = promotionEngine.nominate(unit, "security-analyst");
        String answer = prompt("  Approve for promotion to security knowledge? (y/n)").trim().toLowerCase();
        if ("y".equals(answer)) {
            String approver = prompt("  Approver name");
            KnowledgeUnit promoted = promotionEngine.approve(candidate, approver);
            ok("PROMOTED to security knowledge! ID: " + promoted.id());
        } else {
            String reason = prompt("  Rejection reason");
            promotionEngine.reject(candidate, "reviewer", reason);
            warn("Candidate rejected.");
        }
    }

    // ── Option 7: Context assembly ───────────────────────────────────────────

    private void interactiveContextAssembly() {
        header("Context Assembly for Identity Governance Agent");
        String query = prompt("  Enter governance query");
        ContextPackage pkg = assembler.assemble(query, 10);
        printContext(pkg);
    }

    // ── Option 8: Outcome learning ───────────────────────────────────────────

    private void interactiveOutcome() {
        header("Record Outcome — Learning Loop");
        String unitId = prompt("  Knowledge unit ID");
        String result = prompt("  Successful? (y/n)").trim().toLowerCase();
        String feedback = prompt("  Feedback");
        Outcome outcome = new Outcome(result.equals("y"), feedback, LocalDateTime.now());
        Optional<KnowledgeUnit> updated = learningEngine.learn(unitId, outcome);
        if (updated.isPresent()) {
            ok("Confidence updated: " + f(updated.get().confidence()) + " | State: " + updated.get().getState());
        } else {
            warn("Unit not found: " + unitId);
        }
    }

    // ── Option 9: List all ───────────────────────────────────────────────────

    private void listAllKnowledge() {
        header("IKOS Knowledge Store");
        List<KnowledgeUnit> all = storage.listKnowledgeUnits();
        if (all.isEmpty()) {
            warn("Empty. Start with option 1.");
            return;
        }
        System.out.printf("  %-14s %-22s %-22s %-8s %s%n", "ID", "TYPE", "STATE", "CONF", "STATEMENT");
        System.out.println("  " + "─".repeat(100));
        for (var u : all) {
            String state = u.getState() != null ? u.getState().name() : "—";
            System.out.printf("  %-14s %-22s %-22s %-8s %s%n",
                    u.id(), u.type(), state, f(u.confidence()), truncate(u.statement(), 40));
        }
        System.out.println("  Total: " + all.size() + " units");
    }

    // ── AI Agent Query — Spring AI Tool-Calling Demo ────────────────────────

    private void aiAgentQuery() {
        System.out.println();
        System.out
                .println(BOLD + RED + "  ╔══════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + RED + "  ║" + RESET + BOLD
                + "     IKOS AI AGENT — Spring AI Tool-Calling Integration           " + BOLD + RED + "║" + RESET);
        System.out
                .println(BOLD + RED + "  ╚══════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println(BOLD + "  Architecture:" + RESET);
        System.out.println("  " + CYAN + "User Query" + RESET + " → " + YELLOW + "ChatClient" + RESET
                + " → " + PURPLE + "KnowledgeEvolutionAdvisor" + RESET + " → " + GREEN
                + "IdentityGovernanceTools (@Tool)" + RESET);
        System.out.println("                  ↑                                          ↓");
        System.out
                .println("              " + CYAN + "Response" + RESET + "  ←  " + YELLOW + "LLM (Gemini/OpenAI)" + RESET
                        + "  ←  " + GREEN + "Tool Results + Context" + RESET);
        System.out.println();

        System.out.println(BOLD + "  Available @Tool Methods:" + RESET);
        System.out.println("  " + GREEN + "1" + RESET + " AnalyzeIdentityRisks(displayName)");
        System.out.println("  " + GREEN + "2" + RESET
                + " DetectOffboardingGap(name, disabledPlatform, activePlatform, accountId)");
        System.out.println("  " + GREEN + "3" + RESET + " RecordSecurityIncident(id, statement, context, source)");
        System.out.println(
                "  " + GREEN + "4" + RESET + " RecordRemediationAction(id, action, relatedRiskId, successful)");
        System.out.println("  " + GREEN + "5" + RESET + " RecordAuditFinding(id, finding, policyRef, severity)");
        System.out.println("  " + GREEN + "6" + RESET + " RecommendRemediation(riskId)");
        System.out.println("  " + GREEN + "7" + RESET + " ListIdentityRisks()");
        System.out.println("  " + GREEN + "8" + RESET + " ListAllIkosKnowledge()");
        System.out.println();
        System.out
                .println("  " + YELLOW + "Try a natural language query. The agent resolves it to tool calls." + RESET);
        System.out.println("  " + WHITE + "Examples:" + RESET);
        System.out.println("    • \"Who has offboarding gaps?\"");
        System.out.println("    • \"What risks does Sarah Jones have?\"");
        System.out.println("    • \"Show me all identity risks\"");
        System.out.println("    • \"Recommend remediation for the top risk\"");
        System.out.println();

        String query = prompt("Agent query (or 'demo' for auto-demo)");

        if (query.equalsIgnoreCase("demo")) {
            runAgentDemo();
        } else {
            resolveAndExecuteQuery(query);
        }
    }

    private void runAgentDemo() {
        System.out.println();
        System.out
                .println(BOLD + RED + "  ╔══════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + RED + "  ║" + RESET + BOLD
                + "     AUTONOMOUS AGENT LOOP — Multi-Step Reasoning                " + BOLD + RED + "║" + RESET);
        System.out
                .println(BOLD + RED + "  ╚══════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
        System.out.println("  " + BOLD + "Spring AI Agent Loop Pattern:" + RESET);
        System.out.println("  " + WHITE + "  ChatClient chatClient = ChatClient.builder(chatModel)" + RESET);
        System.out.println("  " + WHITE + "      .defaultAdvisors(advisor)     // KnowledgeEvolutionAdvisor" + RESET);
        System.out.println("  " + WHITE + "      .defaultTools(governanceTools) // IdentityGovernanceTools" + RESET);
        System.out.println("  " + WHITE + "      .build();" + RESET);
        System.out.println("  " + WHITE + "  chatClient.prompt().user(query).call().content();" + RESET);
        System.out.println();
        System.out.println("  The LLM autonomously decides which tools to call at each step.");
        System.out.println("  Spring AI handles the loop: " + CYAN + "LLM → Tool → Result → LLM → ..." + RESET);
        System.out.println();
        pause();

        // ── The agent receives a high-level goal ──
        String userGoal = "Investigate and remediate the most critical identity security risk in the organization.";
        System.out.println(
                "  " + BOLD + CYAN + "╭─ USER GOAL ──────────────────────────────────────────────────────╮" + RESET);
        System.out.println("  " + BOLD + CYAN + "│" + RESET + " " + BOLD + userGoal + RESET);
        System.out.println(
                "  " + BOLD + CYAN + "╰──────────────────────────────────────────────────────────────────╯" + RESET);
        System.out.println();

        // ── Agent Loop Iteration 1: Reconnaissance ──
        agentLoopStep(1, "Reconnaissance",
                "I need to understand the current risk landscape first.",
                "ListIdentityRisks", "ListIdentityRisks()");
        String risksResult = governanceTools.listIdentityRisks();
        printToolResult(risksResult);
        String agentThought1 = "I found multiple risks. Let me identify the highest severity one to investigate.";
        printAgentThought(agentThought1);
        pause();

        // ── Agent Loop Iteration 2: Deep Analysis ──
        String topRiskId = findTopRiskId();
        agentLoopStep(2, "Deep Analysis",
                "The highest confidence risk is " + topRiskId + ". Let me analyze the identity behind it.",
                "AnalyzeIdentityRisks", "AnalyzeIdentityRisks(\"Sarah Jones\")");
        String analysisResult = governanceTools.analyzeIdentityRisks("Sarah Jones");
        printToolResult(analysisResult);
        String agentThought2 = "Analysis complete. Now I need a remediation plan with policy references.";
        printAgentThought(agentThought2);
        pause();

        // ── Agent Loop Iteration 3: Generate Remediation Plan ──
        agentLoopStep(3, "Remediation Planning",
                "Generating a policy-mapped remediation recommendation.",
                "RecommendRemediation", "RecommendRemediation(\"" + topRiskId + "\")");
        String remResult = governanceTools.recommendRemediation(topRiskId);
        printToolResult(remResult);
        String agentThought3 = "I have the remediation plan. Let me create a task list to track execution.";
        printAgentThought(agentThought3);
        pause();

        // ── Agent Loop Iteration 4: Create Task List (TodoWriteTool) ──
        agentLoopStep(4, "Task Tracking",
                "Creating structured remediation task list via TodoWriteTool.",
                "TodoWrite", "TodoWrite([3 tasks])");
        TodoWriteTool.Todos agentTodos = new TodoWriteTool.Todos(List.of(
                new TodoWriteTool.Todos.TodoItem(
                        "Disable compromised account on remaining active platforms",
                        TodoWriteTool.Todos.Status.in_progress,
                        "Disabling compromised account"),
                new TodoWriteTool.Todos.TodoItem(
                        "Revoke all active API tokens and session keys",
                        TodoWriteTool.Todos.Status.pending,
                        "Revoking API tokens"),
                new TodoWriteTool.Todos.TodoItem(
                        "Record remediation outcome for knowledge evolution",
                        TodoWriteTool.Todos.Status.pending,
                        "Recording outcome")));
        System.out.println("  ┌──────────────────────────────────────────────────────────────────");
        todoTool.todoWrite(agentTodos);
        System.out.println("  └──────────────────────────────────────────────────────────────────");
        String agentThought4 = "Tasks created. Executing first task and recording the outcome.";
        printAgentThought(agentThought4);
        pause();

        // ── Agent Loop Iteration 5: Execute & Record Outcome ──
        agentLoopStep(5, "Execute & Learn",
                "Recording remediation action and outcome for the learning loop.",
                "RecordRemediationAction", "RecordRemediationAction(\"REM-AGENT-001\", ..., true)");
        String remAction = governanceTools.recordRemediationAction(
                "REM-AGENT-" + System.currentTimeMillis() % 1000,
                "Disabled AWS account jsmith, revoked 3 API tokens, terminated active sessions",
                topRiskId, true);
        printToolResult(remAction);

        // Update todo: mark first done, second in progress
        TodoWriteTool.Todos finalTodos = new TodoWriteTool.Todos(List.of(
                new TodoWriteTool.Todos.TodoItem(
                        "Disable compromised account on remaining active platforms",
                        TodoWriteTool.Todos.Status.completed,
                        "Disabling compromised account"),
                new TodoWriteTool.Todos.TodoItem(
                        "Revoke all active API tokens and session keys",
                        TodoWriteTool.Todos.Status.completed,
                        "Revoking API tokens"),
                new TodoWriteTool.Todos.TodoItem(
                        "Record remediation outcome for knowledge evolution",
                        TodoWriteTool.Todos.Status.in_progress,
                        "Recording outcome")));
        System.out.println("  ┌──────────────────────────────────────────────────────────────────");
        todoTool.todoWrite(finalTodos);
        System.out.println("  └──────────────────────────────────────────────────────────────────");
        pause();

        // ── Agent Loop Iteration 6: Parallel Background Investigation
        // (DefaultTaskRepository) ──
        agentLoopStep(6, "Parallel Investigation (Background Tasks)",
                "Launching parallel background tasks to audit remaining identities.",
                "TaskRepository.putTask", "2 parallel background investigations");

        BackgroundTask bgTask1 = taskRepository.putTask("task-audit-contractors", () -> {
            // Simulates a background audit
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return governanceTools.analyzeIdentityRisks("Mike Chen");
        });
        BackgroundTask bgTask2 = taskRepository.putTask("task-audit-admins", () -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return governanceTools.analyzeIdentityRisks("James Wilson");
        });
        System.out.println(
                "  " + CYAN + "  ⟳ Background Task 1: " + RESET + bgTask1.getTaskId() + " (Audit contractors)");
        System.out.println("  " + CYAN + "  ⟳ Background Task 2: " + RESET + bgTask2.getTaskId() + " (Audit admins)");

        // Wait for results
        System.out.println("  " + YELLOW + "  ⏳ Waiting for background tasks..." + RESET);
        try {
            bgTask1.waitForCompletion(5000);
            bgTask2.waitForCompletion(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("  " + GREEN + "  ✓ Task 1: " + RESET + bgTask1.getStatus() + " — "
                + truncate(bgTask1.getResult(), 50));
        System.out.println("  " + GREEN + "  ✓ Task 2: " + RESET + bgTask2.getStatus() + " — "
                + truncate(bgTask2.getResult(), 50));
        printAgentThought("Both background audits complete. Merging results into final report.");
        pause();

        // ── Agent Loop Iteration 7: Context-Augmented Summary (ContextBuilder) ──
        agentLoopStep(7, "Context-Augmented Summary",
                "Using ContextBuilder to assemble all relevant knowledge for the final LLM response.",
                "ContextBuilder.buildContext", "buildContext(\"identity risk remediation summary\", 10)");
        String summaryContext = contextBuilder.buildContext("identity risk remediation offboarding contractor admin",
                10);
        if (!summaryContext.isBlank()) {
            System.out.println("  ┌──────────────────────────────────────────────────────────────────");
            String[] ctxLines = summaryContext.split("\n");
            int shown = 0;
            for (String line : ctxLines) {
                if (shown >= 8) {
                    System.out.println("  │ ... (" + (ctxLines.length - shown) + " more lines injected into prompt)");
                    break;
                }
                System.out.println("  │ " + line);
                shown++;
            }
            System.out.println("  └──────────────────────────────────────────────────────────────────");
        }
        printAgentThought("Context assembled. KnowledgeEvolutionAdvisor will inject this into system prompt.");
        pause();

        // ── Agent Loop Complete — Final Summary ──
        System.out.println();
        System.out.println(
                "  " + BOLD + GREEN + "╭─ AGENT LOOP COMPLETE ──────────────────────────────────────────╮" + RESET);
        System.out.println(
                "  " + BOLD + GREEN + "│" + RESET + " 7 autonomous iterations, 10 tool calls, 0 human prompts        ");
        System.out.println("  " + BOLD + GREEN + "│" + RESET);
        System.out.println("  " + BOLD + GREEN + "│" + RESET + " " + WHITE + "Loop 1:" + RESET
                + " ListIdentityRisks()       → Discovered risk landscape");
        System.out.println("  " + BOLD + GREEN + "│" + RESET + " " + WHITE + "Loop 2:" + RESET
                + " AnalyzeIdentityRisks()    → Deep-dived highest risk");
        System.out.println("  " + BOLD + GREEN + "│" + RESET + " " + WHITE + "Loop 3:" + RESET
                + " RecommendRemediation()    → Generated policy-mapped plan");
        System.out.println("  " + BOLD + GREEN + "│" + RESET + " " + WHITE + "Loop 4:" + RESET
                + " TodoWrite()               → Structured task tracking");
        System.out.println("  " + BOLD + GREEN + "│" + RESET + " " + WHITE + "Loop 5:" + RESET
                + " RecordRemediationAction() → Outcome feeds learning loop");
        System.out.println("  " + BOLD + GREEN + "│" + RESET + " " + WHITE + "Loop 6:" + RESET
                + " TaskRepository (parallel) → 2 background investigations");
        System.out.println("  " + BOLD + GREEN + "│" + RESET + " " + WHITE + "Loop 7:" + RESET
                + " ContextBuilder.build()    → Knowledge-augmented summary");
        System.out.println("  " + BOLD + GREEN + "│" + RESET);
        System.out.println("  " + BOLD + GREEN + "│" + RESET
                + " Knowledge evolution: Outcome → Confidence update → Pattern discovery");
        System.out
                .println("  " + BOLD + GREEN + "│" + RESET + " The system is now smarter for the next investigation.");
        System.out.println(
                "  " + BOLD + GREEN + "╰──────────────────────────────────────────────────────────────────╯" + RESET);
    }

    private void agentLoopStep(int iteration, String phase, String reasoning, String toolName, String toolCall) {
        System.out.println();
        System.out.println("  " + BOLD + YELLOW + "┌─ Loop " + iteration + ": " + phase
                + " ────────────────────────────────────────" + RESET);
        System.out.println("  " + YELLOW + "│" + RESET + " " + PURPLE + "LLM Reasoning:" + RESET + " " + reasoning);
        System.out.println(
                "  " + YELLOW + "│" + RESET + " " + GREEN + "Tool Call:" + RESET + " " + BOLD + toolCall + RESET);
        System.out.println(
                "  " + BOLD + YELLOW + "└──────────────────────────────────────────────────────────────────" + RESET);
    }

    private void printToolResult(String result) {
        System.out.println("  ┌──────────────────────────────────────────────────────────────────");
        for (String line : result.split("\n")) {
            System.out.println("  │ " + line);
        }
        System.out.println("  └──────────────────────────────────────────────────────────────────");
    }

    private void printAgentThought(String thought) {
        System.out.println("  " + PURPLE + "  💭 " + thought + RESET);
    }

    private void resolveAndExecuteQuery(String query) {
        if (query == null || query.isBlank()) {
            warn("No query entered. Try: 'Show me all identity risks'");
            return;
        }
        String lower = query.toLowerCase();

        // Step 1: Show KnowledgeEvolutionAdvisor context injection
        System.out.println();
        System.out.println("  " + PURPLE + "① KnowledgeEvolutionAdvisor.before()" + RESET
                + " — injecting IKOS context into prompt");
        ContextPackage ctx = assembler.assemble(query, 5);
        if (!ctx.isEmpty()) {
            System.out.println("     Injected " + ctx.rankedUnits().size() + " relevant knowledge unit(s) (relevance: "
                    + f(ctx.overallRelevance()) + ")");
            for (var u : ctx.rankedUnits()) {
                System.out.println("     " + WHITE + "• [" + u.id() + "] " + truncate(u.statement(), 60) + RESET);
            }
        } else {
            System.out.println("     No prior knowledge found — agent will rely on tools.");
        }

        // Step 2: Resolve which tool to call
        System.out.println();
        System.out.println("  " + YELLOW + "② ChatClient → LLM resolves tool call" + RESET);

        String toolName;
        String toolResult;

        if (lower.contains("list") && (lower.contains("risk") || lower.contains("all"))) {
            toolName = "ListIdentityRisks";
            System.out.println("     " + GREEN + "Tool selected: " + BOLD + toolName + "()" + RESET);
            toolResult = governanceTools.listIdentityRisks();
        } else if (lower.contains("analyz") && lower.contains("risk")) {
            String name = extractName(lower);
            toolName = "AnalyzeIdentityRisks";
            System.out.println("     " + GREEN + "Tool selected: " + BOLD + toolName + "(\"" + name + "\")" + RESET);
            toolResult = governanceTools.analyzeIdentityRisks(name);
        } else if (lower.contains("recommend") || lower.contains("remediat")) {
            toolName = "RecommendRemediation";
            String riskId = findTopRiskId();
            System.out.println("     " + GREEN + "Tool selected: " + BOLD + toolName + "(\"" + riskId + "\")" + RESET);
            toolResult = governanceTools.recommendRemediation(riskId);
        } else if (lower.contains("offboard")) {
            toolName = "DetectOffboardingGap";
            System.out.println("     " + GREEN + "Tool selected: " + BOLD + toolName + "(...)" + RESET);
            toolResult = governanceTools.detectOffboardingGap(
                    "John Smith", "ActiveDirectory", "AWS_IAM", "jsmith");
        } else if (lower.contains("incident")) {
            toolName = "RecordSecurityIncident";
            System.out.println("     " + GREEN + "Tool selected: " + BOLD + toolName + "(...)" + RESET);
            toolResult = governanceTools.recordSecurityIncident(
                    "INC-" + System.currentTimeMillis() % 1000,
                    "Suspicious login detected from unknown IP",
                    "AWS Console, us-east-1", "CloudTrail Alert");
        } else if (lower.contains("knowledge") || lower.contains("store")) {
            toolName = "ListAllIkosKnowledge";
            System.out.println("     " + GREEN + "Tool selected: " + BOLD + toolName + "()" + RESET);
            toolResult = governanceTools.listAllKnowledge();
        } else {
            toolName = "ListIdentityRisks";
            System.out.println("     " + GREEN + "Tool selected: " + BOLD + toolName + "() (default)" + RESET);
            toolResult = governanceTools.listIdentityRisks();
        }

        // Step 3: Show tool result
        System.out.println();
        System.out.println("  " + GREEN + "③ @Tool " + toolName + " → result" + RESET);
        System.out.println("  ┌──────────────────────────────────────────────────────────────────");
        for (String line : toolResult.split("\n")) {
            System.out.println("  │ " + line);
        }
        System.out.println("  └──────────────────────────────────────────────────────────────────");

        // Step 4: Show advisor after-hook
        System.out.println();
        System.out.println("  " + PURPLE + "④ KnowledgeEvolutionAdvisor.after()" + RESET
                + " — extracting observations from response");
        System.out.println("     Tool result stored as operational knowledge for future context assembly.");

        // Step 5: Simulated LLM response
        System.out.println();
        System.out.println("  " + CYAN + "⑤ LLM Response (synthesized from tool results + IKOS context):" + RESET);
        System.out
                .println("  " + BOLD + "╭──────────────────────────────────────────────────────────────────╮" + RESET);
        String response = generateAgentResponse(toolName, toolResult, ctx);
        for (String line : response.split("\n")) {
            System.out.println("  " + BOLD + "│" + RESET + " " + line);
        }
        System.out
                .println("  " + BOLD + "╰──────────────────────────────────────────────────────────────────╯" + RESET);
    }

    private String extractName(String query) {
        String[] names = { "john smith", "sarah jones", "mike chen", "svc-deploy" };
        for (String name : names) {
            if (query.contains(name)) {
                return name.substring(0, 1).toUpperCase() + name.substring(1);
            }
        }
        // Default
        return "Sarah Jones";
    }

    private String findTopRiskId() {
        return storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION).stream()
                .max((a, b) -> Double.compare(a.confidence(), b.confidence()))
                .map(KnowledgeUnit::id)
                .orElse("RISK-0001");
    }

    private String generateAgentResponse(String toolName, String toolResult, ContextPackage ctx) {
        return switch (toolName) {
            case "ListIdentityRisks" -> {
                long count = storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION).size();
                yield "Based on the IKOS knowledge store, I found " + count + " active identity risks.\n" +
                      "The most critical involve offboarding gaps and SOD violations.\n" +
                      "I recommend prioritizing CRITICAL risks first — use 'Recommend remediation'\n" +
                      "for specific action plans with policy references.";
            }
            case "AnalyzeIdentityRisks" ->
                "Identity risk analysis complete. The knowledge evolution system has\n" +
                "correlated observations across multiple platforms. Historical patterns\n" +
                "from " + ctx.rankedUnits().size() + " prior knowledge units were used to enrich\n" +
                "this analysis. Confidence scores reflect evidence × consistency ×\n" +
                "validation × outcome factors.";
            case "RecommendRemediation" ->
                "Remediation recommendation generated with full evidence chain.\n" +
                "Actions are mapped to NIST 800-53 and PAM policy controls.\n" +
                "Risk reduction estimates are based on historical outcome data\n" +
                "from the IKOS learning loop.";
            default ->
                "Query processed. The IKOS knowledge store has been updated.\n" +
                "All observations feed into the knowledge evolution pipeline\n" +
                "for automated pattern discovery and confidence scoring.";
        };
    }

    // ── Spring AI Framework Integration Showcase ─────────────────────────────

    private void springAiShowcase() {
        System.out.println();
        System.out
                .println(BOLD + RED + "  ╔══════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + RED + "  ║" + RESET + BOLD
                + "   SPRING AI AGENT UTILS — Framework Integration Showcase         " + BOLD + RED + "║" + RESET);
        System.out
                .println(BOLD + RED + "  ╚══════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();

        // ── Component 1: EngineeringMemoryTools ──
        step("1", "EngineeringMemoryTools — @Tool methods for knowledge management");
        System.out.println(
                "  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.memory.tools.EngineeringMemoryTools");
        System.out.println(
                "  " + GREEN + "Tools:" + RESET + " GetProjectContext, UpdateProjectContext, RecordObservation,");
        System.out.println("         AddEvidence, RecordDecision, RecordIncident,");
        System.out.println("         ProposeLocalPattern, ProposeLocalOpinion,");
        System.out.println("         PromoteToGlobalPattern, PromoteToGlobalOpinion, ListMemories");
        System.out.println();

        // Demo: Set project context
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET
                + " UpdateProjectContext(\"IKOS\", \"Spring AI,Java 25,Jackson 3\", 3, \"Multi-platform IAM\")");
        String result = engineeringTools.updateProjectContext("IKOS", "Spring AI,Java 25,Jackson 3", 3,
                "Multi-platform IAM,NIST compliance");
        System.out.println("  " + GREEN + "✓" + RESET + " " + result);
        System.out.println();

        // Demo: Record observation via engineering tools
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " RecordObservation(\"ENG-OBS-001\", ...)");
        result = engineeringTools.recordObservation("ENG-OBS-001",
                "Cross-platform privilege correlation reduces false positives by 40%",
                "IKOS Risk Detection Engine analysis",
                "DefaultRiskDetectionEngine.java:detectCrossPlatformAdmin");
        System.out.println("  " + GREEN + "✓" + RESET + " " + result);
        System.out.println();

        // Demo: Get project context
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " GetProjectContext()");
        result = engineeringTools.getProjectContext();
        for (String line : result.split("\n")) {
            System.out.println("  " + WHITE + "  " + line + RESET);
        }
        pause();

        // ── Component 2: ContextBuilder ──
        step("2", "ContextBuilder — semantic context retrieval for LLM augmentation");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.memory.ContextBuilder");
        System.out.println(
                "  " + GREEN + "Used by:" + RESET + " KnowledgeEvolutionAdvisor (injected into system prompt)");
        System.out.println();

        System.out.println(
                "  " + YELLOW + "▸ Calling:" + RESET + " buildContext(\"identity offboarding risk detection\", 5)");
        String context = contextBuilder.buildContext("identity offboarding risk privilege detection", 5);
        if (context.isBlank()) {
            System.out.println("  " + WHITE + "  (No prior knowledge — run lifecycle demo first)" + RESET);
        } else {
            String[] lines = context.split("\n");
            int shown = 0;
            for (String line : lines) {
                if (shown >= 12) {
                    System.out.println("  " + WHITE + "  ... (" + (lines.length - shown) + " more lines)" + RESET);
                    break;
                }
                System.out.println("  " + WHITE + "  " + line + RESET);
                shown++;
            }
        }
        pause();

        // ── Component 3: AutoMemoryTools ──
        step("3", "AutoMemoryTools — persistent memory file management (MEMORY.md pattern)");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.AutoMemoryTools");
        System.out.println("  " + GREEN + "Tools:" + RESET
                + " MemoryView, MemoryCreate, MemoryStrReplace, MemoryInsert, MemoryDelete, MemoryRename");
        System.out.println("  " + GREEN + "Root:" + RESET + " " + autoMemoryTools.getMemoriesDir());
        System.out.println();

        // Create a memory index
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " MemoryCreate(\"MEMORY.md\", ...)");
        String memContent = "---\nname: IKOS Memory Index\ndescription: Index of all IKOS security knowledge memories\ntype: project\n---\n"
                + "# IKOS Security Knowledge Memories\n\n"
                + "- [Identity Risk Patterns](identity_risks.md) — Cross-platform risk detection patterns\n"
                + "- [Remediation Playbooks](playbooks.md) — Outcome-validated remediation steps\n";
        result = autoMemoryTools.memoryCreate("MEMORY.md", memContent);
        System.out.println("  " + GREEN + "✓" + RESET + " " + result);

        // Create an identity risk memory
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " MemoryCreate(\"identity_risks.md\", ...)");
        String riskMem = "---\nname: Identity Risk Patterns\ndescription: Recurring identity risk patterns across enterprise platforms\ntype: project\n---\n"
                + "## Offboarding Gaps\n"
                + "Most common: AD disabled, AWS/Okta still active\n"
                + "Detection: Cross-platform status comparison\n"
                + "Policy: PAM-001, NIST AC-2\n\n"
                + "## Contractor Privilege Creep\n"
                + "Non-employees accumulating >30 permissions\n"
                + "Detection: EffectivePrivilegeProfile analysis\n"
                + "Policy: PAM-007, NIST AC-6(5)\n";
        result = autoMemoryTools.memoryCreate("identity_risks.md", riskMem);
        System.out.println("  " + GREEN + "✓" + RESET + " " + result);
        System.out.println();

        // View the memory store
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " MemoryView(\"/\")");
        result = autoMemoryTools.memoryView("", null);
        for (String line : result.split("\n")) {
            System.out.println("  " + WHITE + "  " + line + RESET);
        }
        System.out.println();

        // ── MarkdownParser: Parse memory file metadata ──
        System.out.println("  " + YELLOW + "▸ MarkdownParser:" + RESET + " Parsing identity_risks.md front matter");
        MarkdownParser parser = new MarkdownParser(riskMem);
        System.out.println("    " + GREEN + "Metadata:" + RESET);
        parser.getFrontMatter().forEach((k, v) -> System.out.println("      " + k + ": " + BOLD + v + RESET));
        System.out.println("    " + GREEN + "Content length:" + RESET + " " + parser.getContent().length() + " chars");
        System.out.println("    " + GREEN + "Content preview:" + RESET + " " + truncate(parser.getContent(), 60));
        pause();

        // ── Component 4: KnowledgeEvolutionAdvisor ──
        step("4", "KnowledgeEvolutionAdvisor — ChatClient advisor for context injection");
        System.out.println("  " + GREEN + "Class:" + RESET
                + " org.springaicommunity.agent.memory.advisors.KnowledgeEvolutionAdvisor");
        System.out.println("  " + GREEN + "Implements:" + RESET + " BaseChatMemoryAdvisor (Spring AI)");
        System.out.println("  " + GREEN + "Order:" + RESET + " " + advisor.getOrder());
        System.out.println();
        System.out.println("  " + BOLD + "Lifecycle:" + RESET);
        System.out
                .println("  " + CYAN + "  before()" + RESET + " → Extracts user query → ContextBuilder.buildContext()");
        System.out.println("             → Augments system prompt with <engineering-memory> block");
        System.out.println("  " + CYAN + "  after()" + RESET + "  → Pass-through (knowledge persisted by tools)");
        System.out.println();
        System.out.println("  " + BOLD + "Spring AI ChatClient Usage:" + RESET);
        System.out.println("  " + WHITE + "  ChatClient.builder(chatModel)" + RESET);
        System.out.println("  " + WHITE + "      .defaultAdvisors(" + RESET);
        System.out.println("  " + WHITE + "          KnowledgeEvolutionAdvisor.builder()" + RESET);
        System.out.println("  " + WHITE + "              .contextBuilder(contextBuilder)" + RESET);
        System.out.println("  " + WHITE + "              .maxRetrievedUnits(10)" + RESET);
        System.out.println("  " + WHITE + "              .build())" + RESET);
        System.out.println("  " + WHITE + "      .defaultTools(governanceTools)" + RESET);
        System.out.println("  " + WHITE + "      .build();" + RESET);
        pause();

        // ── Component 5: AutoMemoryToolsAdvisor ──
        step("5", "AutoMemoryToolsAdvisor — autonomous memory persistence advisor");
        System.out.println(
                "  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor");
        System.out.println("  " + GREEN + "Implements:" + RESET + " BaseChatMemoryAdvisor (Spring AI)");
        System.out.println("  " + GREEN + "By:" + RESET + " Christian Tzolov (Spring AI team)");
        System.out.println();
        System.out.println("  " + BOLD + "Lifecycle:" + RESET);
        System.out.println(
                "  " + CYAN + "  before()" + RESET + " → Injects Memory* tools (MemoryView, MemoryCreate, etc.)");
        System.out.println("             → Augments system prompt with memory management instructions");
        System.out.println("             → Optionally triggers memory consolidation");
        System.out
                .println("  " + CYAN + "  after()" + RESET + "  → Memory persistence handled by model via tool calls");
        System.out.println();
        System.out.println("  " + BOLD + "Spring AI ChatClient Usage:" + RESET);
        System.out.println("  " + WHITE + "  ChatClient.builder(chatModel)" + RESET);
        System.out.println("  " + WHITE + "      .defaultAdvisors(" + RESET);
        System.out.println("  " + WHITE + "          AutoMemoryToolsAdvisor.builder()" + RESET);
        System.out.println("  " + WHITE + "              .memoriesRootDirectory(\"/path/to/memories\")" + RESET);
        System.out.println("  " + WHITE + "              .build())" + RESET);
        System.out.println("  " + WHITE + "      .build();" + RESET);
        pause();

        // ── Component 6: IdentityGovernanceTools ──
        step("6", "IdentityGovernanceTools — IKOS-specific @Tool methods for agent interaction");
        System.out.println(
                "  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.memory.tools.IdentityGovernanceTools");
        System.out.println("  " + GREEN + "Tools:" + RESET + " 8 domain-specific @Tool methods");
        System.out.println();
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " ListIdentityRisks()");
        result = governanceTools.listIdentityRisks();
        for (String line : result.split("\n")) {
            System.out.println("  " + WHITE + "  " + line + RESET);
        }
        pause();

        // ── Component 7: AskUserQuestionTool + CommandLineQuestionHandler ──
        step("7", "AskUserQuestionTool — structured agent-to-user clarification");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.AskUserQuestionTool");
        System.out.println(
                "  " + GREEN + "Handler:" + RESET + " org.springaicommunity.agent.utils.CommandLineQuestionHandler");
        System.out.println("  " + GREEN + "By:" + RESET + " Christian Tzolov (Spring AI team)");
        System.out.println();
        System.out.println("  " + BOLD + "How it works:" + RESET);
        System.out.println("  The LLM agent calls AskUserQuestionTool when it needs human input.");
        System.out.println("  Questions have structured options (2-4 choices) with descriptions.");
        System.out.println("  CommandLineQuestionHandler renders them as a CLI menu.");
        System.out.println();
        System.out.println(
                "  " + BOLD + "IKOS Usage:" + RESET + " Governance review decisions (approve/reject/escalate)");
        System.out.println("  " + WHITE + "  AskUserQuestionTool.builder()" + RESET);
        System.out.println("  " + WHITE + "      .questionHandler(new CommandLineQuestionHandler())" + RESET);
        System.out.println("  " + WHITE + "      .answersValidation(false)" + RESET);
        System.out.println("  " + WHITE + "      .build();" + RESET);
        pause();

        // ── Component 8: TodoWriteTool ──
        step("8", "TodoWriteTool — structured remediation task tracking");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.TodoWriteTool");
        System.out.println("  " + GREEN + "By:" + RESET + " Christian Tzolov (Spring AI team)");
        System.out.println();
        System.out.println("  " + BOLD + "How it works:" + RESET);
        System.out.println("  The LLM agent creates a task list to track multi-step remediation.");
        System.out.println("  Tasks have 3 states: pending → in_progress → completed.");
        System.out.println("  Only 1 task can be in_progress at a time (enforced).");
        System.out.println();
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " TodoWrite(3 remediation tasks)");
        TodoWriteTool.Todos demoTodos = new TodoWriteTool.Todos(List.of(
                new TodoWriteTool.Todos.TodoItem("Disable orphaned AWS account", TodoWriteTool.Todos.Status.completed,
                        "Disabling orphaned account"),
                new TodoWriteTool.Todos.TodoItem("Revoke contractor API tokens", TodoWriteTool.Todos.Status.in_progress,
                        "Revoking API tokens"),
                new TodoWriteTool.Todos.TodoItem("Schedule quarterly access review", TodoWriteTool.Todos.Status.pending,
                        "Scheduling review")));
        todoTool.todoWrite(demoTodos);
        pause();

        // ── Component 9: ShellTools ──
        step("9", "ShellTools — Bash command execution with timeout & background support");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.ShellTools");
        System.out.println("  " + GREEN + "Tools:" + RESET + " Bash, BashOutput, KillShell");
        System.out.println();
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " Bash(\"echo IKOS Security Scan && uname -a\")");
        String shellResult = shellTools.bash("echo '  IKOS Security Agent v2.0 — Active' && uname -srm", null, "System info check", false);
        for (String line : shellResult.split("\n")) {
            if (!line.startsWith("bash_id")) System.out.println("    " + WHITE + line + RESET);
        }
        pause();

        // ── Component 10: GrepTool ──
        step("10", "GrepTool — Pure Java regex search across knowledge files");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.GrepTool");
        System.out.println("  " + GREEN + "Features:" + RESET + " Regex, glob filter, context lines, multiline");
        System.out.println();
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " Grep(\".*risk.*\", storageRoot, \"*.json\")");
        String grepResult = grepTool.grep(".*risk.*", storageRoot, "*.json",
                GrepTool.OutputMode.files_with_matches, null, null, null, null, true, null, 5, null, null);
        if (grepResult.startsWith("No matches")) {
            System.out.println("    " + WHITE + grepResult + RESET);
        } else {
            for (String line : grepResult.split("\n")) {
                System.out.println("    " + GREEN + "✓" + RESET + " " + line);
            }
        }
        pause();

        // ── Component 11: GlobTool ──
        step("11", "GlobTool — Fast file pattern matching for knowledge store");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.GlobTool");
        System.out.println();
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " Glob(\"**/*.json\", storageRoot)");
        String globResult = globTool.glob("**/*.json", storageRoot);
        if (globResult.startsWith("No files")) {
            System.out.println("    " + WHITE + globResult + RESET);
        } else {
            int count = 0;
            for (String line : globResult.split("\n")) {
                if (count++ >= 5) { System.out.println("    " + WHITE + "... and more" + RESET); break; }
                System.out.println("    " + GREEN + "📄" + RESET + " " + line);
            }
        }
        pause();

        // ── Component 12: FileSystemTools ──
        step("12", "FileSystemTools — Sandboxed file Read/Write/Edit for agent actions");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.FileSystemTools");
        System.out.println("  " + GREEN + "Tools:" + RESET + " Read, Write, Edit (with allowed directory sandbox)");
        System.out.println("  " + GREEN + "Sandbox:" + RESET + " " + storageRoot);
        System.out.println();
        // Write an audit report file
        String reportPath = storageRoot + "/audit-report.md";
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " Write(\"audit-report.md\", ...)");
        String writeResult = fileSystemTools.write(reportPath,
                "# IKOS Audit Report\n\nGenerated: " + LocalDateTime.now() + "\nStatus: All systems operational\n");
        System.out.println("    " + GREEN + "✓" + RESET + " " + writeResult);
        System.out.println("  " + YELLOW + "▸ Calling:" + RESET + " Read(\"audit-report.md\")");
        String readResult = fileSystemTools.read(reportPath, null, null);
        for (String line : readResult.split("\n")) {
            System.out.println("    " + WHITE + line + RESET);
        }
        pause();

        // ── Component 13: SkillsTool + Skills ──
        step("13", "SkillsTool + Skills — Domain-specific agent knowledge modules");
        System.out.println("  " + GREEN + "Class:" + RESET + " org.springaicommunity.agent.tools.SkillsTool");
        System.out.println("  " + GREEN + "Loader:" + RESET + " org.springaicommunity.agent.utils.Skills");
        System.out.println();
        System.out.println("  " + BOLD + "IKOS Agent Skills (SKILL.md pattern):" + RESET);
        System.out.println("    " + CYAN + "identity-investigation" + RESET + " — Investigate identity risks across platforms");
        System.out.println("    " + CYAN + "compliance-audit" + RESET + "        — Run audits against NIST/SOX/ISO 27001");
        System.out.println("    " + CYAN + "threat-hunting" + RESET + "          — Hunt identity-based threats (MITRE ATT&CK)");
        System.out.println();
        System.out.println("  " + WHITE + "  Skills use YAML front-matter (parsed by MarkdownParser)" + RESET);
        System.out.println("  " + WHITE + "  Loaded via: Skills.loadDirectory(\"src/main/resources/skills\")" + RESET);
        pause();

        // ── Summary ──
        System.out.println();
        System.out.println(BOLD + "  ═══════════════════════════════════════════════════════════════" + RESET);
        System.out.println(BOLD + "  SPRING AI AGENT UTILS INTEGRATION SUMMARY" + RESET);
        System.out.println(BOLD + "  ═══════════════════════════════════════════════════════════════" + RESET);
        System.out.println();
        String check = GREEN + "  ✓" + RESET;
        System.out.println(check + " " + BOLD + "FileMemoryStorage" + RESET + "         — Jackson 3 persistent knowledge store");
        System.out.println(check + " " + BOLD + "ApplicationMemory" + RESET + "         — Project-scoped observation memory");
        System.out.println(check + " " + BOLD + "GovernanceMemory" + RESET + "          — Organization-scoped security knowledge");
        System.out.println(check + " " + BOLD + "KnowledgeEvolutionPipeline" + RESET + "— 5-stage knowledge lifecycle engine");
        System.out.println(check + " " + BOLD + "ContextBuilder" + RESET + "            — Keyword-ranked context retrieval");
        System.out.println(check + " " + BOLD + "KnowledgeEvolutionAdvisor" + RESET + " — Spring AI ChatClient advisor");
        System.out.println(check + " " + BOLD + "AutoMemoryTools" + RESET + "           — Persistent MEMORY.md file management");
        System.out.println(check + " " + BOLD + "AutoMemoryToolsAdvisor" + RESET + "    — Autonomous memory consolidation advisor");
        System.out.println(check + " " + BOLD + "EngineeringMemoryTools" + RESET + "    — @Tool methods for engineering knowledge");
        System.out.println(check + " " + BOLD + "IdentityGovernanceTools" + RESET + "   — @Tool methods for IKOS identity governance");
        System.out.println(check + " " + BOLD + "AskUserQuestionTool" + RESET + "       — Structured agent-to-human clarification");
        System.out.println(check + " " + BOLD + "CommandLineQuestionHandler" + RESET + "— CLI question rendering handler");
        System.out.println(check + " " + BOLD + "TodoWriteTool" + RESET + "             — Remediation task tracking");
        System.out.println(check + " " + BOLD + "DefaultTaskRepository" + RESET + "     — Background task execution (parallel)");
        System.out.println(check + " " + BOLD + "ShellTools" + RESET + "                — Bash/BashOutput/KillShell commands");
        System.out.println(check + " " + BOLD + "GrepTool" + RESET + "                  — Pure Java regex search");
        System.out.println(check + " " + BOLD + "GlobTool" + RESET + "                  — Fast file pattern matching");
        System.out.println(check + " " + BOLD + "FileSystemTools" + RESET + "           — Sandboxed Read/Write/Edit");
        System.out.println(check + " " + BOLD + "ListDirectoryTool" + RESET + "         — Directory listing for agents");
        System.out.println(check + " " + BOLD + "SkillsTool" + RESET + "                — Domain-specific agent skills");
        System.out.println(check + " " + BOLD + "Skills" + RESET + "                    — SKILL.md loader (filesystem + JAR)");
        System.out.println(check + " " + BOLD + "AgentEnvironment" + RESET + "          — System environment detection");
        System.out.println(check + " " + BOLD + "MarkdownParser" + RESET + "            — YAML front-matter extraction");
        System.out.println(check + " " + BOLD + "DefaultContextAssembler" + RESET + "  — 5-factor context scoring");
        System.out.println(check + " " + BOLD + "DefaultContextScorer" + RESET + "     — Relevance × Confidence × Recency scoring");
        System.out.println();
        System.out.println("  " + BOLD + "Total: 25 framework components integrated" + RESET);
        System.out.println("  " + BOLD + "Total: 40+ @Tool methods available to Spring AI agents" + RESET);
    }

    // ── Audit Trail — Knowledge Evolution Timeline ───────────────────────────

    private void auditTrail() {
        List<KnowledgeUnit> all = storage.listKnowledgeUnits();
        if (all.isEmpty()) {
            warn("No knowledge units yet. Run option 1 first.");
            return;
        }

        System.out.println();
        System.out
                .println(BOLD + RED + "  ╔══════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + RED + "  ║" + RESET + BOLD
                + "            IKOS KNOWLEDGE EVOLUTION AUDIT TRAIL                   " + BOLD + RED + "║" + RESET);
        System.out
                .println(BOLD + RED + "  ╚══════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();

        // Group by type
        var byType = new java.util.LinkedHashMap<KnowledgeType, List<KnowledgeUnit>>();
        for (KnowledgeType t : KnowledgeType.values()) {
            List<KnowledgeUnit> units = all.stream().filter(u -> u.type() == t).toList();
            if (!units.isEmpty())
                byType.put(t, units);
        }

        // Timeline header
        System.out.println(BOLD + "  KNOWLEDGE LIFECYCLE FLOW:" + RESET);
        System.out.println("  " + CYAN + "OBSERVATION" + RESET + " → " + YELLOW + "PATTERN_CANDIDATE" + RESET
                + " → " + PURPLE + "VALIDATED_PATTERN" + RESET + " → " + GREEN + "SECURITY_KNOWLEDGE" + RESET);
        System.out.println("  " + "─".repeat(70));
        System.out.println();

        // Count by state
        int obs = 0, patCand = 0, validated = 0, knowledge = 0, deprecated = 0;
        for (KnowledgeUnit u : all) {
            if (u.getState() == null)
                continue;
            switch (u.getState()) {
                case OBSERVATION -> obs++;
                case PATTERN_CANDIDATE -> patCand++;
                case VALIDATED_PATTERN -> validated++;
                case KNOWLEDGE -> knowledge++;
                case DEPRECATED -> deprecated++;
            }
        }

        System.out.println(BOLD + "  STATE DISTRIBUTION:" + RESET);
        System.out.printf("  %-22s %s%n", CYAN + "● OBSERVATION" + RESET, renderBar(obs, all.size(), CYAN));
        System.out.printf("  %-22s %s%n", YELLOW + "● PATTERN_CANDIDATE" + RESET,
                renderBar(patCand, all.size(), YELLOW));
        System.out.printf("  %-22s %s%n", PURPLE + "● VALIDATED_PATTERN" + RESET,
                renderBar(validated, all.size(), PURPLE));
        System.out.printf("  %-22s %s%n", GREEN + "● KNOWLEDGE" + RESET, renderBar(knowledge, all.size(), GREEN));
        System.out.printf("  %-22s %s%n", RED + "● DEPRECATED" + RESET, renderBar(deprecated, all.size(), RED));
        System.out.println();

        // Type breakdown
        System.out.println(BOLD + "  TYPE BREAKDOWN:" + RESET);
        System.out.println("  " + "─".repeat(70));
        for (var entry : byType.entrySet()) {
            KnowledgeType type = entry.getKey();
            List<KnowledgeUnit> units = entry.getValue();
            String typeColor = switch (type) {
                case RISK_OBSERVATION -> RED;
                case SECURITY_INCIDENT -> RED;
                case REMEDIATION_ACTION -> GREEN;
                case LOCAL_PATTERN -> YELLOW;
                case SECURITY_KNOWLEDGE -> PURPLE;
                case PROMOTION_CANDIDATE -> BLUE;
                default -> WHITE;
            };

            System.out.println();
            System.out.println("  " + typeColor + "▸ " + type + RESET + " (" + units.size() + " units)");
            for (KnowledgeUnit u : units) {
                String stateIcon = u.getState() == null ? "?" : switch (u.getState()) {
                    case OBSERVATION -> CYAN + "○";
                    case PATTERN_CANDIDATE -> YELLOW + "◑";
                    case VALIDATED_PATTERN -> PURPLE + "◕";
                    case KNOWLEDGE -> GREEN + "●";
                    case DEPRECATED -> RED + "✗";
                    default -> WHITE + "◇";
                };
                String reviewed = u.getLastReviewed() != null
                        ? u.getLastReviewed().toLocalDate().toString()
                        : "—";
                System.out.printf("    %s" + RESET + " %-14s  Conf: %s  Reviewed: %s  %s%n",
                        stateIcon, u.id(), f(u.confidence()), reviewed,
                        truncate(u.statement(), 45));
            }
        }

        System.out.println();
        System.out.println("  " + "─".repeat(70));
        System.out.printf("  " + BOLD + "Total: %d units | %d types | Knowledge maturity: %.0f%%%n" + RESET,
                all.size(), byType.size(),
                all.isEmpty() ? 0 : (double) (validated + knowledge) / all.size() * 100);
    }

    private String renderBar(int count, int total, String color) {
        int width = 25;
        int filled = total > 0 ? (int) ((double) count / total * width) : 0;
        filled = Math.max(filled, count > 0 ? 1 : 0);
        return color + "█".repeat(filled) + RESET + "░".repeat(width - filled) + " " + count;
    }

    // ── Compliance Dashboard ─────────────────────────────────────────────────

    private void complianceDashboard() {
        List<KnowledgeUnit> all = storage.listKnowledgeUnits();
        List<KnowledgeUnit> risks = all.stream().filter(u -> u.type() == KnowledgeType.RISK_OBSERVATION).toList();
        List<KnowledgeUnit> incidents = all.stream().filter(u -> u.type() == KnowledgeType.SECURITY_INCIDENT).toList();
        List<KnowledgeUnit> remediations = all.stream().filter(u -> u.type() == KnowledgeType.REMEDIATION_ACTION)
                .toList();
        List<KnowledgeUnit> patterns = all.stream().filter(u -> u.type() == KnowledgeType.LOCAL_PATTERN).toList();
        List<KnowledgeUnit> knowledge = all.stream().filter(
                u -> u.type() == KnowledgeType.SECURITY_KNOWLEDGE || u.type() == KnowledgeType.ENGINEERING_KNOWLEDGE)
                .toList();

        System.out.println();
        System.out
                .println(BOLD + RED + "  ╔══════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + RED + "  ║" + RESET + BOLD
                + "           IKOS COMPLIANCE & RISK DASHBOARD                      " + BOLD + RED + "║" + RESET);
        System.out
                .println(BOLD + RED + "  ╚══════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();

        // Knowledge Store Summary
        System.out.println(BOLD + "  ┌─ KNOWLEDGE STORE SUMMARY ─────────────────────────────────────┐" + RESET);
        System.out.printf("  │  Total Units: %-6d  Risks: %-4d  Incidents: %-4d  Patterns: %-3d │%n", all.size(),
                risks.size(), incidents.size(), patterns.size());
        System.out.printf("  │  Remediations: %-4d   Promoted Knowledge: %-4d                   │%n",
                remediations.size(), knowledge.size());
        System.out.println("  └──────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Risk Severity Heatmap
        int critical = 0, high = 0, medium = 0, low = 0;
        for (KnowledgeUnit r : risks) {
            double c = r.confidence();
            if (c >= 0.9)
                critical++;
            else if (c >= 0.7)
                high++;
            else if (c >= 0.5)
                medium++;
            else
                low++;
        }
        System.out.println(BOLD + "  ┌─ RISK SEVERITY HEATMAP ───────────────────────────────────────┐" + RESET);
        System.out.println("  │  " + RED + "CRITICAL " + bar(critical, risks.size()) + " " + critical + RESET + " │");
        System.out.println(
                "  │  " + YELLOW + "HIGH     " + bar(high, risks.size()) + " " + high + RESET + "             │");
        System.out.println(
                "  │  " + CYAN + "MEDIUM   " + bar(medium, risks.size()) + " " + medium + RESET + "             │");
        System.out
                .println("  │  " + GREEN + "LOW      " + bar(low, risks.size()) + " " + low + RESET + "             │");
        System.out.println("  └──────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Confidence Distribution
        double avgConf = all.stream().mapToDouble(KnowledgeUnit::confidence).average().orElse(0);
        long validated = all.stream().filter(
                u -> u.getState() == KnowledgeState.VALIDATED_PATTERN || u.getState() == KnowledgeState.KNOWLEDGE)
                .count();
        long deprecated = all.stream().filter(u -> u.getState() == KnowledgeState.DEPRECATED).count();
        double remSuccess = remediations.stream().mapToDouble(KnowledgeUnit::confidence).average().orElse(0);

        System.out.println(BOLD + "  ┌─ COMPLIANCE SCORES ──────────────────────────────────────────┐" + RESET);
        System.out.printf("  │  Avg Confidence:       " + scoreColor(avgConf) + "%.1f%%" + RESET
                + "                                    │%n", avgConf * 100);
        System.out.printf("  │  Remediation Rate:     " + scoreColor(remSuccess) + "%.1f%%" + RESET
                + "                                    │%n", remSuccess * 100);
        System.out.printf(
                "  │  Validated Knowledge:  " + GREEN + "%d" + RESET + " unit(s)                                  │%n",
                validated);
        System.out.printf("  │  Deprecated:           " + (deprecated > 0 ? YELLOW : GREEN) + "%d" + RESET
                + " unit(s)                                  │%n", deprecated);
        int compScore = computeComplianceScore(risks.size(), remediations.size(), (int) validated, (int) deprecated);
        System.out.printf("  │  " + BOLD + "Overall Compliance:    " + scoreColor(compScore / 100.0) + "%d/100" + RESET
                + "                                   │%n", compScore);
        System.out.println("  └──────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Top risks
        if (!risks.isEmpty()) {
            System.out.println(BOLD + "  ┌─ TOP ACTIVE RISKS ───────────────────────────────────────────┐" + RESET);
            risks.stream().sorted((a, b) -> Double.compare(b.confidence(), a.confidence())).limit(5).forEach(r -> {
                String sev = r.confidence() >= 0.9 ? RED + "CRIT"
                        : r.confidence() >= 0.7 ? YELLOW + "HIGH" : CYAN + "MED ";
                System.out.printf("  │  [%s" + RESET + "] %-55s │%n", sev, truncate(r.statement(), 55));
            });
            System.out.println("  └──────────────────────────────────────────────────────────────────┘");
        }
    }

    private String bar(int count, int total) {
        int maxWidth = 30;
        int filled = total > 0 ? (int) ((double) count / Math.max(total, 1) * maxWidth) : 0;
        return "█".repeat(Math.max(filled, count > 0 ? 1 : 0))
                + "░".repeat(maxWidth - Math.max(filled, count > 0 ? 1 : 0));
    }

    private String scoreColor(double v) {
        if (v >= 0.8)
            return GREEN;
        if (v >= 0.5)
            return YELLOW;
        return RED;
    }

    private int computeComplianceScore(int risks, int remediations, int validated, int deprecated) {
        int score = 50;
        score -= risks * 5;
        score += remediations * 8;
        score += validated * 10;
        score -= deprecated * 3;
        return Math.max(0, Math.min(100, score));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void printRisk(KnowledgeUnit u) {
        System.out.println("  " + RED + "⚠ " + RESET + BOLD + u.statement() + RESET);
        System.out.println("    " + YELLOW + "Confidence: " + f(u.confidence()) + RESET
                + " | Evidence: " + (u.evidence() != null ? u.evidence().size() : 0) + " item(s)");
    }

    private void printUnit(KnowledgeUnit u) {
        System.out.println("  " + YELLOW + "┌─ " + u.id() + RESET);
        System.out.println("  " + YELLOW + "│  " + RESET + BOLD + u.statement() + RESET);
        System.out.println("  " + YELLOW + "│  " + RESET + "Type:       " + CYAN + u.type() + RESET);
        String state = u.getState() != null ? u.getState().name() : "—";
        System.out.println("  " + YELLOW + "│  " + RESET + "State:      " + PURPLE + state + RESET);
        System.out.println("  " + YELLOW + "│  " + RESET + "Confidence: " + GREEN + f(u.confidence()) + RESET);
        System.out.println("  " + YELLOW + "└  " + RESET + "Evidence:   "
                + (u.evidence() == null ? 0 : u.evidence().size()) + " item(s)");
    }

    private void printRecommendation(KnowledgeUnit risk) {
        System.out.println();
        System.out.println("  " + RED + "═══ RECOMMENDATION ═══" + RESET);
        System.out.println("  " + BOLD + "Risk: " + RESET + risk.statement());
        System.out.println("  " + BOLD + "Confidence: " + RESET + GREEN + f(risk.confidence()) + RESET);
        String stmt = risk.statement() != null ? risk.statement().toUpperCase() : "";
        if (stmt.contains("OFFBOARDING")) {
            System.out.println("  " + CYAN + "Action:" + RESET + " Disable active account, revoke tokens, review SSO");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-001, NIST AC-2");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~85%");
        } else if (stmt.contains("DORMANT")) {
            System.out.println("  " + CYAN + "Action:" + RESET + " Disable dormant admin, rotate credentials");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-003, NIST AC-2(3)");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~72%");
        } else if (stmt.contains("CROSS-PLATFORM")) {
            System.out.println("  " + CYAN + "Action:" + RESET + " Apply least privilege, implement JIT access");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-004, NIST AC-6");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~78%");
        } else if (stmt.contains("ORPHAN") || stmt.contains("SERVICE")) {
            System.out.println(
                    "  " + CYAN + "Action:" + RESET + " Assign owner, rotate credentials, enforce lifecycle policy");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-005, NIST IA-4");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~80%");
        } else if (stmt.contains("PRIVILEGE") || stmt.contains("CREEP")) {
            System.out.println(
                    "  " + CYAN + "Action:" + RESET + " Conduct entitlement review, remove excessive permissions");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-006, NIST AC-6(1)");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~70%");
        } else if (stmt.contains("CONTRACTOR")) {
            System.out.println("  " + CYAN + "Action:" + RESET
                    + " Enforce least privilege, recertify quarterly, set expiration date");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-007, NIST AC-6(5)");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~82%");
        } else if (stmt.contains("SOD") || stmt.contains("SEPARATION")) {
            System.out.println(
                    "  " + CYAN + "Action:" + RESET + " Split admin roles, implement break-glass for emergency access");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-009, NIST AC-5");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~90%");
        } else if (stmt.contains("STALE") && stmt.contains("SERVICE")) {
            System.out.println("  " + CYAN + "Action:" + RESET
                    + " Rotate credentials immediately, assign owner, set 90-day rotation");
            System.out.println("  " + CYAN + "Policy:" + RESET + " PAM-008, NIST IA-5(1)");
            System.out.println("  " + CYAN + "Risk Reduction:" + RESET + " ~88%");
        } else {
            System.out.println("  " + CYAN + "Action:" + RESET + " Review and remediate the detected risk");
        }
        System.out.println("  " + RED + "═══════════════════════" + RESET);
    }

    private void printContext(ContextPackage pkg) {
        if (pkg.isEmpty()) {
            warn("No relevant units for: " + pkg.query());
            return;
        }
        System.out.println("  " + CYAN + "Context for: \"" + pkg.query() + "\"" + RESET);
        System.out.println("  Overall relevance: " + GREEN + f(pkg.overallRelevance()) + RESET);
        System.out.println("  Retrieved " + pkg.rankedUnits().size() + " unit(s):");
        int rank = 1;
        for (var u : pkg.rankedUnits()) {
            System.out.printf("  %2d. " + BOLD + "[%s]" + RESET + " %s%n", rank++, u.id(), u.statement());
            System.out.printf("      Type: %-22s Confidence: %s%n", u.type(), f(u.confidence()));
        }
    }

    private static void banner() {
        System.out.println(BOLD + RED);
        System.out.println("  ██╗██╗  ██╗ ██████╗ ███████╗");
        System.out.println("  ██║██║ ██╔╝██╔═══██╗██╔════╝");
        System.out.println("  ██║█████╔╝ ██║   ██║███████╗");
        System.out.println("  ██║██╔═██╗ ██║   ██║╚════██║");
        System.out.println("  ██║██║  ██╗╚██████╔╝███████║");
        System.out.println("  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝");
        System.out.println(RESET + BOLD);
        System.out.println("  Identity Knowledge Operating System");
        System.out.println("  Continuously learning security intelligence" + RESET);
        System.out.println();
    }

    private void header(String title) {
        System.out.println(BOLD + BLUE + "── " + title + " ──" + RESET);
        System.out.println();
    }

    private void step(String num, String desc) {
        System.out.println();
        System.out.println("  " + BOLD + RED + "Step " + num + ": " + desc + RESET);
    }

    private void ok(String msg) {
        System.out.println("  " + GREEN + "✓ " + msg + RESET);
    }

    private void warn(String msg) {
        System.out.println("  " + YELLOW + "⚠ " + msg + RESET);
    }

    private void pause() {
        System.out.println();
        System.out.print("  " + BLUE + "[ press Enter to continue ]" + RESET);
        scanner.nextLine();
    }

    private String prompt(String label) {
        System.out.print("  " + BOLD + label + ": " + RESET);
        return scanner.nextLine();
    }

    private static String f(double v) {
        return String.format("%.3f", v);
    }

    private static String truncate(String s, int max) {
        if (s == null)
            return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static void bye() {
        System.out.println();
        System.out.println(BOLD + GREEN + "  Goodbye! IKOS data persisted in ~/.ikos-demo" + RESET);
        System.out.println();
    }
}
