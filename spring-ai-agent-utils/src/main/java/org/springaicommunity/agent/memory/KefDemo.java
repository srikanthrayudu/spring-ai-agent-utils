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
import org.springaicommunity.agent.memory.model.*;
import org.springaicommunity.agent.memory.outcome.InMemoryOutcomeTracker;
import org.springaicommunity.agent.memory.outcome.OutcomeLearningEngine;
import org.springaicommunity.agent.memory.promotion.DefaultPromotionEngine;
import org.springaicommunity.agent.memory.storage.FileMemoryStorage;
import org.springaicommunity.agent.memory.validation.DefaultConfidenceCalculator;
import org.springaicommunity.agent.memory.validation.DefaultKnowledgeValidator;
import org.springaicommunity.agent.memory.validation.KeywordContradictionDetector;
import org.springaicommunity.agent.memory.model.ValidationResult;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interactive CLI demo for the Knowledge Evolution Framework (KEF).
 *
 * <p>Run with:
 * <pre>
 *   mvn exec:java -Dexec.mainClass="org.springaicommunity.agent.memory.KefDemo"
 * </pre>
 *
 * @author Antigravity
 */
public class KefDemo {

    // ANSI colour codes
    static final String RESET  = "\033[0m";
    static final String BOLD   = "\033[1m";
    static final String CYAN   = "\033[0;36m";
    static final String GREEN  = "\033[0;32m";
    static final String YELLOW = "\033[0;33m";
    static final String RED    = "\033[0;31m";
    static final String BLUE   = "\033[0;34m";
    static final String PURPLE = "\033[0;35m";

    private final FileMemoryStorage storage;
    private final ApplicationMemory appMemory;
    private final GovernanceMemory  govMemory;
    private final KnowledgeEvolutionPipeline pipeline;
    private final DefaultPromotionEngine promotionEngine;
    private final DefaultContextAssembler assembler;
    private final OutcomeLearningEngine learningEngine;
    private final Scanner scanner;

    public KefDemo(String storageRoot) {
        this.storage         = new FileMemoryStorage(storageRoot);
        this.appMemory       = new ApplicationMemory(storage);
        this.govMemory       = new GovernanceMemory(storage);
        this.pipeline        = new KnowledgeEvolutionPipeline(storage);
        this.promotionEngine = new DefaultPromotionEngine(storage);
        this.assembler       = new DefaultContextAssembler(appMemory, govMemory);
        this.learningEngine  = new OutcomeLearningEngine(storage, new InMemoryOutcomeTracker());
        this.scanner         = new Scanner(System.in);
    }

    public static void main(String[] args) {
        String storageRoot = System.getProperty("kef.storage", System.getProperty("user.home") + "/.kef-demo");
        new File(storageRoot).mkdirs();

        banner();
        System.out.println(CYAN + "  Storage: " + storageRoot + RESET);
        System.out.println();

        KefDemo demo = new KefDemo(storageRoot);
        demo.run();
    }

    // ── Main loop ─────────────────────────────────────────────────────────

    private void run() {
        while (true) {
            printMenu();
            String choice = prompt("Choose").trim();
            System.out.println();
            switch (choice) {
                case "1" -> demoFullLifecycle();
                case "2" -> interactiveObservation();
                case "3" -> {
                    header("Auto-Discovered Pattern Candidates");
                    var candidates = pipeline.getPatternCandidates();
                    if (candidates.isEmpty()) { warn("No pattern candidates yet."); }
                    else { for (var c : candidates) { printUnit(c); } }
                }
                case "4" -> interactivePromotion();
                case "5" -> interactiveContextAssembly();
                case "6" -> interactiveOutcome();
                case "7" -> listAllMemory();
                case "8" -> showConfidenceCalculator();
                case "q", "Q" -> { bye(); return; }
                default -> warn("Unknown option — try again.");
            }
            System.out.println();
        }
    }

    // ── Menu ──────────────────────────────────────────────────────────────

    private static void printMenu() {
        System.out.println(BOLD + BLUE + "━━━ KEF Demo Menu ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
        System.out.println("  " + CYAN + "1" + RESET + "  Run full lifecycle demo (automated)");
        System.out.println("  " + CYAN + "2" + RESET + "  Record an observation manually");
        System.out.println("  " + CYAN + "3" + RESET + "  View auto-discovered pattern candidates");
        System.out.println("  " + CYAN + "4" + RESET + "  Nominate + approve a promotion candidate");
        System.out.println("  " + CYAN + "5" + RESET + "  Assemble context for a query");
        System.out.println("  " + CYAN + "6" + RESET + "  Record an outcome and learn");
        System.out.println("  " + CYAN + "7" + RESET + "  List all memory units");
        System.out.println("  " + CYAN + "8" + RESET + "  Show confidence calculation");
        System.out.println("  " + CYAN + "q" + RESET + "  Quit");
        System.out.println(BLUE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
    }

    // ── Option 1: Full automated lifecycle ────────────────────────────────

    private void demoFullLifecycle() {
        header("Full KEF Lifecycle: Observation → Knowledge → Context → Learning");

        // Step 1
        step("1", "Capture two observations from project events");
        KnowledgeUnit obs1 = pipeline.createObservation(
                "OBS-001", "HikariCP connection pool exhausted under load",
                "Payment service — prod", "payment-service.log:4521");
        pipeline.addEvidence("OBS-001", "NewRelic alert: pool utilisation 100%");
        printUnit(obs1);

        KnowledgeUnit obs2 = pipeline.createObservation(
                "OBS-002", "Login service timeouts correlate with pool exhaustion",
                "Auth service — prod", "auth-service.log:892");
        printUnit(obs2);
        pause();

        // Step 2
        step("2", "Propose a local pattern from the observations");
        KnowledgeUnit pattern = pipeline.proposeLocalPattern(
                "PAT-001", "HikariCP pool saturation causes cascading timeouts across services",
                "High-load production scenarios", List.of("OBS-001", "OBS-002"));
        printUnit(pattern);
        pause();

        // Step 3
        step("3", "Validate the pattern");
        var validator = new DefaultKnowledgeValidator(
                new KeywordContradictionDetector(), new DefaultConfidenceCalculator());
        ValidationResult vr = validator.validate(pattern, govMemory.getEngineeringKnowledge());
        printValidation(vr);
        pause();

        // Step 4
        step("4", "Nominate for promotion (human review queue)");
        PromotionCandidate candidate = promotionEngine.nominate(pattern, "alice");
        printPromotion(candidate);
        pause();

        // Step 5
        step("5", "Human approves — knowledge promoted to GovernanceMemory");
        KnowledgeUnit promoted = promotionEngine.approve(candidate, "bob");
        printUnit(promoted);
        ok("Pattern is now organizational KNOWLEDGE with confidence " + f(promoted.confidence()));
        pause();

        // Step 6
        step("6", "Assemble context for an agent query");
        govMemory.addGlobalPattern(KnowledgeUnit.builder()
                .id(promoted.id()).statement(promoted.statement())
                .type(KnowledgeType.GLOBAL_PATTERN).confidence(promoted.confidence())
                .state(KnowledgeState.KNOWLEDGE).lastReviewed(LocalDateTime.now()).build());

        ContextPackage pkg = assembler.assemble("HikariCP connection pool timeouts", 10);
        printContext(pkg);
        pause();

        // Step 7
        step("7", "Record a successful outcome — learning loop closes");
        Outcome outcome = new Outcome(true, "Increased pool size to 50 — timeouts eliminated.", LocalDateTime.now());
        Optional<KnowledgeUnit> updated = learningEngine.learn(promoted.id(), outcome);
        updated.ifPresent(u -> ok("Confidence after outcome learning: " + f(u.confidence())));

        ok("Full KEF lifecycle complete!");
    }

    // ── Option 2: Interactive observation ─────────────────────────────────

    private void interactiveObservation() {
        header("Record Observation");
        String id        = prompt("  Observation ID (e.g. OBS-010)");
        String statement = prompt("  Statement (what did you observe?)");
        String context   = prompt("  Context (service / env / component)");
        String source    = prompt("  Source (log file / trace ID / URL)");

        KnowledgeUnit obs = pipeline.createObservation(id, statement, context, source);
        ok("Observation saved!");
        printUnit(obs);
    }

    // ── Option 3: Interactive pattern ─────────────────────────────────────

    private void interactivePattern() {
        header("Propose Local Pattern");
        String id         = prompt("  Pattern ID (e.g. PAT-010)");
        String statement  = prompt("  Pattern statement");
        String context    = prompt("  Context");
        String obsIdsText = prompt("  Backing observation IDs (comma-separated, e.g. OBS-001,OBS-002)");

        List<String> obsIds = List.of(obsIdsText.split(",")).stream().map(String::trim).toList();
        KnowledgeUnit pattern = pipeline.proposeLocalPattern(id, statement, context, obsIds);
        ok("Local pattern proposed!");
        printUnit(pattern);
    }

    // ── Option 4: Interactive promotion ───────────────────────────────────

    private void interactivePromotion() {
        header("Promote Pattern to Organizational Knowledge");
        String sourceId  = prompt("  Local pattern ID to promote");
        String nominator = prompt("  Your name (nominator)");

        Optional<KnowledgeUnit> unitOpt = storage.getKnowledgeUnit(sourceId);
        if (unitOpt.isEmpty()) {
            warn("Pattern '" + sourceId + "' not found. Record it first (option 3).");
            return;
        }
        KnowledgeUnit unit = unitOpt.get();
        PromotionCandidate candidate = promotionEngine.nominate(unit, nominator);
        printPromotion(candidate);

        String answer = prompt("  Approve promotion? (y/n)").trim().toLowerCase();
        if (answer.equals("y")) {
            String approver = prompt("  Approver name");
            KnowledgeUnit promoted = promotionEngine.approve(candidate, approver);
            ok("PROMOTED! ID: " + promoted.id() + " | Confidence: " + f(promoted.confidence()));
        } else {
            String reason = prompt("  Rejection reason");
            String rejector = prompt("  Your name (rejector)");
            promotionEngine.reject(candidate, rejector, reason);
            warn("Candidate rejected. Source unit remains as VALIDATED_PATTERN.");
        }
    }

    // ── Option 5: Interactive context assembly ─────────────────────────────

    private void interactiveContextAssembly() {
        header("Assemble Context for Agent Query");
        String query = prompt("  Enter agent query / question");
        String limitStr = prompt("  Max units to retrieve (default 10)").trim();
        int limit = limitStr.isBlank() ? 10 : Integer.parseInt(limitStr);

        ContextPackage pkg = assembler.assemble(query, limit);
        printContext(pkg);
    }

    // ── Option 6: Interactive outcome ─────────────────────────────────────

    private void interactiveOutcome() {
        header("Record Outcome and Learn");
        String unitId   = prompt("  Knowledge unit ID that drove the decision");
        String result   = prompt("  Was the outcome successful? (y/n)").trim().toLowerCase();
        String feedback = prompt("  Feedback / notes");

        boolean successful = result.equals("y");
        Outcome outcome = new Outcome(successful, feedback, LocalDateTime.now());
        Optional<KnowledgeUnit> updated = learningEngine.learn(unitId, outcome);
        if (updated.isPresent()) {
            KnowledgeUnit u = updated.get();
            ok("Unit updated. New confidence: " + f(u.confidence()) + " | State: " + u.getState());
            if (u.getState() == KnowledgeState.DEPRECATED) {
                warn("Unit auto-deprecated due to low cumulative success rate.");
            }
        } else {
            warn("Unit '" + unitId + "' not found.");
        }
    }

    // ── Option 7: List all memory ─────────────────────────────────────────

    private void listAllMemory() {
        header("All Memory Units");
        List<org.springaicommunity.agent.memory.model.KnowledgeUnit> all = storage.listKnowledgeUnits();
        if (all.isEmpty()) {
            warn("No units stored yet. Start with option 1 or 2.");
            return;
        }
        System.out.printf("  %-12s %-20s %-22s %-6s %s%n", "ID", "TYPE", "STATE", "CONF", "STATEMENT");
        System.out.println("  " + "─".repeat(90));
        for (var u : all) {
            String state = u.getState() != null ? u.getState().name() : "—";
            System.out.printf("  %-12s %-20s %-22s %-6s %s%n",
                    u.id(), u.type(), state, f(u.confidence()),
                    truncate(u.statement(), 40));
        }
        System.out.println("  " + "─".repeat(90));
        System.out.println("  Total: " + all.size() + " units");
    }

    // ── Option 8: Confidence calculator demo ─────────────────────────────

    private void showConfidenceCalculator() {
        header("Confidence = Evidence × Consistency × Validation × Outcome");
        System.out.println();
        System.out.printf("  %-8s %-16s %-16s %-16s %-16s %-10s%n",
                "# Evid", "EvidScore", "ConsistScore", "ValidScore", "OutcomeScore", "TOTAL");
        System.out.println("  " + "─".repeat(82));

        for (int n = 1; n <= 8; n++) {
            Confidence c = new Confidence(
                    Math.min(1.0, 0.3 + 0.2 * (n - 1)),   // evidence
                    1.0,                                    // no contradictions
                    n >= 3 ? Confidence.VALIDATION_HUMAN : Confidence.VALIDATION_NONE,
                    1.0                                     // all outcomes positive
            );
            System.out.printf("  %-8d %-16s %-16s %-16s %-16s " + BOLD + GREEN + "%-10s" + RESET + "%n",
                    n, f(c.evidenceScore()), f(c.consistencyScore()),
                    f(c.validationScore()), f(c.outcomeScore()), f(c.value()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void printUnit(KnowledgeUnit u) {
        System.out.println("  " + YELLOW + "┌─ " + u.id() + RESET);
        System.out.println("  " + YELLOW + "│  " + RESET + BOLD + u.statement() + RESET);
        System.out.println("  " + YELLOW + "│  " + RESET + "Type:       " + CYAN + u.type() + RESET);
        String state = u.getState() != null ? u.getState().name() : "—";
        System.out.println("  " + YELLOW + "│  " + RESET + "State:      " + PURPLE + state + RESET);
        System.out.println("  " + YELLOW + "│  " + RESET + "Confidence: " + GREEN + f(u.confidence()) + RESET);
        int evCount = u.evidence() == null ? 0 : u.evidence().size();
        System.out.println("  " + YELLOW + "└  " + RESET + "Evidence:   " + evCount + " item(s)");
    }

    private void printValidation(ValidationResult vr) {
        String status = vr.valid() ? GREEN + "PASSED ✓" + RESET : RED + "FAILED ✗" + RESET;
        System.out.println("  Validation: " + status);
        System.out.println("  Confidence: " + GREEN + f(vr.confidenceScore()) + RESET);
        if (!vr.contradictions().isEmpty()) {
            System.out.println("  Contradictions: " + RED + vr.contradictions() + RESET);
        }
        System.out.println("  Reviewed by: " + vr.reviewedBy());
    }

    private void printPromotion(PromotionCandidate c) {
        System.out.println("  " + PURPLE + "Promotion Candidate: " + c.id() + RESET);
        System.out.println("  Source:   " + c.sourceId() + " → " + c.proposedGlobalId());
        System.out.println("  Status:   " + YELLOW + c.status() + RESET);
        System.out.println("  Proposed: " + c.proposedBy() + " at " + c.proposedAt());
    }

    private void printContext(ContextPackage pkg) {
        if (pkg.isEmpty()) {
            warn("No relevant units found for query: " + pkg.query());
            return;
        }
        System.out.println("  " + CYAN + "Context for: \"" + pkg.query() + "\"" + RESET);
        System.out.println("  Overall relevance: " + GREEN + f(pkg.overallRelevance()) + RESET);
        System.out.println("  Retrieved " + pkg.rankedUnits().size() + " unit(s):");
        System.out.println();
        int rank = 1;
        for (var u : pkg.rankedUnits()) {
            System.out.printf("  %2d. " + BOLD + "[%s]" + RESET + " %s%n", rank++, u.id(), u.statement());
            System.out.printf("      Type: %-22s Confidence: %s%n", u.type(), f(u.confidence()));
        }
    }

    private static void banner() {
        System.out.println(BOLD + CYAN);
        System.out.println("  ██╗  ██╗███████╗███████╗");
        System.out.println("  ██║ ██╔╝██╔════╝██╔════╝");
        System.out.println("  █████╔╝ █████╗  █████╗  ");
        System.out.println("  ██╔═██╗ ██╔══╝  ██╔══╝  ");
        System.out.println("  ██║  ██╗███████╗██║     ");
        System.out.println("  ╚═╝  ╚═╝╚══════╝╚═╝     ");
        System.out.println(RESET + BOLD);
        System.out.println("  Knowledge Evolution Framework — Interactive Demo");
        System.out.println("  spring-ai-agent-utils v0.11.0-SNAPSHOT" + RESET);
        System.out.println();
    }

    private void header(String title) {
        System.out.println(BOLD + BLUE + "── " + title + " ──────────────────────────────────────────" + RESET);
        System.out.println();
    }

    private void step(String num, String desc) {
        System.out.println();
        System.out.println("  " + BOLD + CYAN + "Step " + num + ": " + desc + RESET);
    }

    private void ok(String msg)   { System.out.println("  " + GREEN  + "✓ " + msg + RESET); }
    private void warn(String msg) { System.out.println("  " + YELLOW + "⚠ " + msg + RESET); }

    private void pause() {
        System.out.println();
        System.out.print("  " + BLUE + "[ press Enter to continue ]" + RESET);
        scanner.nextLine();
    }

    private String prompt(String label) {
        System.out.print("  " + BOLD + label + ": " + RESET);
        return scanner.nextLine();
    }

    private static String f(double v) { return String.format("%.3f", v); }
    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static void bye() {
        System.out.println();
        System.out.println(BOLD + GREEN + "  Goodbye! KEF data is persisted in ~/.kef-demo" + RESET);
        System.out.println();
    }
}
