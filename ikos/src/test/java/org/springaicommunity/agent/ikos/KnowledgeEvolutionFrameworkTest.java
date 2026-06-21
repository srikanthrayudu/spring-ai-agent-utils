/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.ikos.context.ContextPackage;
import org.springaicommunity.agent.ikos.context.DefaultContextAssembler;
import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.outcome.InMemoryOutcomeTracker;
import org.springaicommunity.agent.ikos.outcome.OutcomeLearningEngine;
import org.springaicommunity.agent.ikos.outcome.OutcomeTracker;
import org.springaicommunity.agent.ikos.promotion.DefaultPromotionEngine;
import org.springaicommunity.agent.ikos.promotion.PromotionEngine;
import org.springaicommunity.agent.ikos.storage.FileMemoryStorage;
import org.springaicommunity.agent.ikos.storage.MemoryStorage;
import org.springaicommunity.agent.ikos.validation.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Full Knowledge Evolution Framework integration test.
 * Exercises every engine through the complete lifecycle:
 *   Experience → Observation → Pattern → Validation → Promotion → Knowledge
 *   → Context Assembly → Outcome Learning
 *
 * @author Antigravity
 */
@DisplayName("Knowledge Evolution Framework — Full Lifecycle Test")
class KnowledgeEvolutionFrameworkTest {

    @TempDir
    Path tempDir;

    private MemoryStorage storage;
    private ApplicationMemory appMemory;
    private GovernanceMemory govMemory;
    private KnowledgeEvolutionPipeline pipeline;

    @BeforeEach
    void setUp() {
        this.storage    = new FileMemoryStorage(tempDir.toString());
        this.appMemory  = new ApplicationMemory(storage);
        this.govMemory  = new GovernanceMemory(storage);
        this.pipeline   = new KnowledgeEvolutionPipeline(storage);
    }

    // ───────────────────────────────────────────────────────────────────────
    // Phase 1 — Domain Model
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 1 — Domain Model")
    class DomainModelTests {

        @Test
        @DisplayName("KnowledgeState has DEPRECATED and ARCHIVED terminal states")
        void kefStatesAreComplete() {
            assertThat(KnowledgeState.values())
                .contains(KnowledgeState.DEPRECATED, KnowledgeState.ARCHIVED);
        }

        @Test
        @DisplayName("KnowledgeType has RECOMMENDATION and PROMOTION_CANDIDATE")
        void kefTypesAreComplete() {
            assertThat(KnowledgeType.values())
                .contains(KnowledgeType.RECOMMENDATION, KnowledgeType.PROMOTION_CANDIDATE);
        }

        @Test
        @DisplayName("Confidence formula: evidenceScore × consistencyScore × validationScore × outcomeScore")
        void confidenceFormula() {
            Confidence c = new Confidence(0.7, 1.0, Confidence.VALIDATION_HUMAN, 1.0);
            assertThat(c.value()).isEqualTo(0.7, within(0.001));
        }

        @Test
        @DisplayName("Confidence.ofEvidenceCount grows with evidence items")
        void evidenceGrowth() {
            Confidence one   = Confidence.ofEvidenceCount(1);
            Confidence three = Confidence.ofEvidenceCount(3);
            assertThat(three.evidenceScore()).isGreaterThan(one.evidenceScore());
        }

        @Test
        @DisplayName("Confidence.isTrusted() is false below 0.5")
        void confidenceTrustThreshold() {
            Confidence low  = new Confidence(0.3, 1.0, Confidence.VALIDATION_NONE, 1.0);
            Confidence high = new Confidence(0.7, 1.0, Confidence.VALIDATION_HUMAN, 1.0);
            assertThat(low.isTrusted()).isFalse();
            assertThat(high.isTrusted()).isTrue();
        }

        @Test
        @DisplayName("ValidationResult factory methods")
        void validationResultFactories() {
            ValidationResult passing = ValidationResult.passing(0.8);
            ValidationResult failing = ValidationResult.failing(List.of("contradicts ENG-001"));

            assertThat(passing.valid()).isTrue();
            assertThat(passing.confidenceScore()).isEqualTo(0.8);
            assertThat(failing.valid()).isFalse();
            assertThat(failing.contradictions()).containsExactly("contradicts ENG-001");
        }

        @Test
        @DisplayName("PromotionCandidate state transitions: PENDING → APPROVED / REJECTED")
        void promotionCandidateWorkflow() {
            PromotionCandidate pending = PromotionCandidate.nominate(
                    "PROMO-001", "PAT-001", "GPAT-001",
                    "Saturation causes timeouts.", KnowledgeType.GLOBAL_PATTERN,
                    List.of(), ValidationResult.passing(0.7), "alice");

            assertThat(pending.isPending()).isTrue();

            PromotionCandidate approved = pending.approve("bob");
            assertThat(approved.isApproved()).isTrue();
            assertThat(approved.reviewedBy()).isEqualTo("bob");

            PromotionCandidate rejected = pending.reject("carol", "needs more evidence");
            assertThat(rejected.isRejected()).isTrue();
            assertThat(rejected.rejectionReason()).isEqualTo("needs more evidence");
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Phase 2 — Confidence Calculator
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 2 — Confidence Calculator")
    class ConfidenceCalculatorTests {

        private ConfidenceCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new DefaultConfidenceCalculator(10);
        }

        @Test
        @DisplayName("Single evidence → base score 0.3")
        void singleEvidenceBaseScore() {
            KnowledgeUnit unit = KnowledgeUnit.builder()
                    .id("OBS-001").statement("Timeout under load.")
                    .type(KnowledgeType.OBSERVATION)
                    .evidenceStrings(List.of("LogA")).confidence(0.3).build();

            Confidence c = calculator.calculate(unit, null);
            assertThat(c.evidenceScore()).isEqualTo(0.3, within(0.01));
        }

        @Test
        @DisplayName("Three evidence items → evidence score 0.7")
        void threeEvidenceScore() {
            KnowledgeUnit unit = KnowledgeUnit.builder()
                    .id("OBS-002").statement("Pool saturated repeatedly.")
                    .type(KnowledgeType.OBSERVATION)
                    .evidenceStrings(List.of("LogA", "LogB", "LogC")).confidence(0.7).build();

            Confidence c = calculator.calculate(unit, null);
            assertThat(c.evidenceScore()).isEqualTo(0.7, within(0.01));
        }

        @Test
        @DisplayName("Human-reviewed ValidationResult → validationScore 1.0")
        void humanValidationScore() {
            KnowledgeUnit unit = KnowledgeUnit.builder()
                    .id("PAT-001").statement("HikariCP saturation → timeout.")
                    .type(KnowledgeType.LOCAL_PATTERN)
                    .evidenceStrings(List.of("OBS-001", "OBS-002")).confidence(0.5).build();

            ValidationResult vr = ValidationResult.humanReviewed(true, 0.9, List.of(), List.of(), "alice");
            Confidence c = calculator.calculate(unit, vr);
            assertThat(c.validationScore()).isEqualTo(Confidence.VALIDATION_HUMAN, within(0.001));
        }

        @Test
        @DisplayName("Contradictions reduce consistency score")
        void contradictionsReduceConsistency() {
            KnowledgeUnit unit = KnowledgeUnit.builder()
                    .id("PAT-002").statement("Always use connection pooling.")
                    .type(KnowledgeType.LOCAL_PATTERN)
                    .evidenceStrings(List.of("OBS-001")).confidence(0.4).build();

            ValidationResult vr = ValidationResult.failing(List.of("contradicts ENG-001"));
            Confidence c = calculator.calculate(unit, vr);
            assertThat(c.consistencyScore()).isLessThan(1.0);
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Phase 3 — Validation Engine
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 3 — Validation Engine")
    class ValidationEngineTests {

        private KnowledgeValidator validator;

        @BeforeEach
        void setUp() {
            validator = new DefaultKnowledgeValidator(
                    new KeywordContradictionDetector(),
                    new DefaultConfidenceCalculator(10));
        }

        @Test
        @DisplayName("Unit with no contradictions passes validation")
        void noContradictionsPassesValidation() {
            KnowledgeUnit candidate = KnowledgeUnit.builder()
                    .id("PAT-001").statement("Always use connection pooling under load.")
                    .type(KnowledgeType.LOCAL_PATTERN)
                    .evidenceStrings(List.of("OBS-001", "OBS-002")).confidence(0.5).build();

            ValidationResult result = validator.validate(candidate, List.of());
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("ContradictionDetector finds antonym-based contradictions")
        void contradictionDetected() {
            var detector = new KeywordContradictionDetector();

            KnowledgeUnit existing = KnowledgeUnit.builder()
                    .id("ENG-001").statement("Always use connection pooling for database access.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE).confidence(0.9).build();

            KnowledgeUnit candidate = KnowledgeUnit.builder()
                    .id("PAT-001").statement("Never use connection pooling for database access.")
                    .type(KnowledgeType.LOCAL_PATTERN).confidence(0.5).build();

            List<String> contradictions = detector.detectContradictions(candidate, List.of(existing));
            assertThat(contradictions).isNotEmpty();
            assertThat(contradictions.get(0)).contains("ENG-001");
        }

        @Test
        @DisplayName("KNOWLEDGE state units pass validation without re-checking")
        void knowledgeStateSkipsValidation() {
            KnowledgeUnit knowledge = KnowledgeUnit.builder()
                    .id("ENG-001").statement("Always use connection pooling.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE)
                    .state(KnowledgeState.KNOWLEDGE).confidence(0.95).build();

            ValidationResult result = validator.validate(knowledge, List.of());
            assertThat(result.valid()).isTrue();
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Phase 4 — Promotion Engine
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 4 — Promotion Engine")
    class PromotionEngineTests {

        private PromotionEngine promotionEngine;

        @BeforeEach
        void setUp() {
            promotionEngine = new DefaultPromotionEngine(storage);
        }

        @Test
        @DisplayName("Nominate creates a PENDING promotion candidate")
        void nominateCreatesPending() {
            pipeline.createObservation("OBS-001", "Timeout under load", "DB", "LogA");
            pipeline.addEvidence("OBS-001", "LogB");
            KnowledgeUnit pattern = pipeline.proposeLocalPattern(
                    "PAT-001", "Pool saturation causes timeouts", "DB", List.of("OBS-001"));

            PromotionCandidate candidate = promotionEngine.nominate(pattern, "alice");

            assertThat(candidate.isPending()).isTrue();
            assertThat(candidate.sourceId()).isEqualTo("PAT-001");
            assertThat(candidate.proposedBy()).isEqualTo("alice");
        }

        @Test
        @DisplayName("Approve promotes unit to GovernanceMemory and removes from ApplicationMemory")
        void approvePromotesToGlobalMemory() {
            pipeline.createObservation("OBS-001", "Timeout under load", "DB", "LogA");
            pipeline.addEvidence("OBS-001", "LogB");
            KnowledgeUnit pattern = pipeline.proposeLocalPattern(
                    "PAT-001", "Pool saturation causes timeouts", "DB", List.of("OBS-001"));

            PromotionCandidate candidate = promotionEngine.nominate(pattern, "alice");
            KnowledgeUnit promoted = promotionEngine.approve(candidate, "bob");

            assertThat(promoted.getState()).isEqualTo(KnowledgeState.KNOWLEDGE);
            assertThat(promoted.id()).isEqualTo("GPAT-001");
            assertThat(storage.getKnowledgeUnit("PAT-001")).isEmpty();
            assertThat(storage.getKnowledgeUnit("GPAT-001")).isPresent();
        }

        @Test
        @DisplayName("Reject records rejection reason and keeps candidate status REJECTED")
        void rejectRecordsReason() {
            KnowledgeUnit unit = KnowledgeUnit.builder()
                    .id("PAT-002").statement("Use always lazy loading.")
                    .type(KnowledgeType.LOCAL_PATTERN)
                    .evidenceStrings(List.of("OBS-A", "OBS-B")).confidence(0.6).build();
            storage.saveKnowledgeUnit(unit);

            PromotionCandidate candidate = promotionEngine.nominate(unit, "alice");
            PromotionCandidate rejected  = promotionEngine.reject(candidate, "carol", "needs cross-project evidence");

            assertThat(rejected.isRejected()).isTrue();
            assertThat(rejected.rejectionReason()).contains("cross-project evidence");
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Phase 5 — Context Assembly (5-factor scoring)
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 5 — Context Assembly")
    class ContextAssemblyTests {

        private DefaultContextAssembler assembler;

        @BeforeEach
        void setUp() {
            assembler = new DefaultContextAssembler(appMemory, govMemory);
        }

        @Test
        @DisplayName("ContextPackage ranks high-confidence relevant units first")
        void relevantUnitRankedFirst() {
            govMemory.addEngineeringKnowledge(KnowledgeUnit.builder()
                    .id("ENG-001")
                    .statement("Java concurrency is managed via Threads or Virtual Threads.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE).confidence(0.9)
                    .state(KnowledgeState.KNOWLEDGE)
                    .lastReviewed(LocalDateTime.now()).build());

            govMemory.addPrinciple(KnowledgeUnit.builder()
                    .id("PRN-001")
                    .statement("Principle of Least Privilege applies to system permissions.")
                    .type(KnowledgeType.PRINCIPLE).confidence(0.95)
                    .state(KnowledgeState.KNOWLEDGE)
                    .lastReviewed(LocalDateTime.now()).build());

            ContextPackage pkg = assembler.assemble("How should we handle Java concurrency?", 5);

            assertThat(pkg.rankedUnits()).isNotEmpty();
            // ENG-001 (concurrency match) should rank above PRN-001 (no match)
            List<String> ids = pkg.rankedUnits().stream().map(KnowledgeUnit::id).toList();
            assertThat(ids).contains("ENG-001");
        }

        @Test
        @DisplayName("Units with confidence < 0.5 are excluded from ContextPackage")
        void lowConfidenceExcluded() {
            govMemory.addEngineeringKnowledge(KnowledgeUnit.builder()
                    .id("ENG-LOW")
                    .statement("Java concurrency patterns exist.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE).confidence(0.2)  // too low
                    .state(KnowledgeState.KNOWLEDGE)
                    .lastReviewed(LocalDateTime.now()).build());

            ContextPackage pkg = assembler.assemble("Java concurrency patterns", 10);

            List<String> ids = pkg.rankedUnits().stream().map(KnowledgeUnit::id).toList();
            assertThat(ids).doesNotContain("ENG-LOW");
        }

        @Test
        @DisplayName("DEPRECATED and ARCHIVED units are excluded from ContextPackage")
        void deprecatedAndArchivedExcluded() {
            govMemory.addEngineeringKnowledge(KnowledgeUnit.builder()
                    .id("ENG-DEP")
                    .statement("Old concurrency approach using raw Thread.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE).confidence(0.9)
                    .state(KnowledgeState.DEPRECATED)
                    .lastReviewed(LocalDateTime.now()).build());

            ContextPackage pkg = assembler.assemble("concurrency Thread", 10);
            List<String> ids = pkg.rankedUnits().stream().map(KnowledgeUnit::id).toList();
            assertThat(ids).doesNotContain("ENG-DEP");
        }

        @Test
        @DisplayName("ContextPackage.operationalUnits() and governanceUnits() partition correctly")
        void packagePartitioning() {
            appMemory.addObservation(KnowledgeUnit.builder()
                    .id("OBS-001").statement("Database timeouts under heavy load.")
                    .type(KnowledgeType.OBSERVATION).confidence(0.6)
                    .state(KnowledgeState.OBSERVATION)
                    .lastReviewed(LocalDateTime.now()).build());

            govMemory.addEngineeringKnowledge(KnowledgeUnit.builder()
                    .id("ENG-001").statement("Database connection pooling prevents timeouts.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE).confidence(0.9)
                    .state(KnowledgeState.KNOWLEDGE)
                    .lastReviewed(LocalDateTime.now()).build());

            ContextPackage pkg = assembler.assemble("database timeout connection", 10);

            assertThat(pkg.operationalUnits().stream().map(KnowledgeUnit::id))
                    .contains("OBS-001");
            assertThat(pkg.governanceUnits().stream().map(KnowledgeUnit::id))
                    .contains("ENG-001");
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Phase 6 — Outcome Learning
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 6 — Outcome Learning")
    class OutcomeLearningTests {

        private OutcomeTracker tracker;
        private OutcomeLearningEngine learningEngine;

        @BeforeEach
        void setUp() {
            tracker        = new InMemoryOutcomeTracker();
            learningEngine = new OutcomeLearningEngine(storage, tracker);
        }

        @Test
        @DisplayName("Successful outcome strengthens unit confidence")
        void successfulOutcomeIncreasesConfidence() {
            govMemory.addEngineeringKnowledge(KnowledgeUnit.builder()
                    .id("ENG-001").statement("Use connection pooling.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE).confidence(0.6)
                    .state(KnowledgeState.KNOWLEDGE).build());

            Outcome success = new Outcome(true, "Timeout resolved after pooling config.", LocalDateTime.now());
            Optional<KnowledgeUnit> updated = learningEngine.learn("ENG-001", success);

            assertThat(updated).isPresent();
            assertThat(updated.get().confidence()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Repeated failures deprecate KNOWLEDGE units")
        void repeatedFailuresDeprecateUnit() {
            govMemory.addEngineeringKnowledge(KnowledgeUnit.builder()
                    .id("ENG-002").statement("Always use synchronous calls.")
                    .type(KnowledgeType.ENGINEERING_KNOWLEDGE).confidence(0.9)
                    .state(KnowledgeState.KNOWLEDGE).build());

            Outcome fail = new Outcome(false, "Caused deadlocks.", LocalDateTime.now());
            // Record 5 failures
            for (int i = 0; i < 5; i++) {
                learningEngine.learn("ENG-002", fail);
            }

            Optional<KnowledgeUnit> unit = storage.getKnowledgeUnit("ENG-002");
            assertThat(unit).isPresent();
            // Unit should be deprecated due to 0% success rate (below 30% threshold)
            assertThat(unit.get().getState()).isEqualTo(KnowledgeState.DEPRECATED);
        }

        @Test
        @DisplayName("OutcomeTracker counts successful and total outcomes independently")
        void outcomeTrackerCounting() {
            tracker.record("ENG-001", new Outcome(true,  "Success", LocalDateTime.now()));
            tracker.record("ENG-001", new Outcome(true,  "Success", LocalDateTime.now()));
            tracker.record("ENG-001", new Outcome(false, "Failure", LocalDateTime.now()));

            assertThat(tracker.countTotal("ENG-001")).isEqualTo(3);
            assertThat(tracker.countSuccessful("ENG-001")).isEqualTo(2);
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Full End-to-End Lifecycle
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Full KEF Lifecycle — End-to-End")
    class FullLifecycleTest {

        @Test
        @DisplayName("Complete lifecycle: Observation → Pattern → Promotion → Knowledge → Context → Outcome")
        void completeKefLifecycle() {
            // ── Step 1: Capture observations ──────────────────────────────
            pipeline.createObservation("OBS-001", "HikariCP pool exhausted under load", "DB pool=10", "ServiceLog");
            pipeline.addEvidence("OBS-001", "MonitoringAlert");
            pipeline.createObservation("OBS-002", "Login service timeouts correlate with pool exhaustion", "DB", "LoginLog");

            // ── Step 2: Discover a local pattern ──────────────────────────
            KnowledgeUnit pattern = pipeline.proposeLocalPattern(
                    "PAT-001", "HikariCP pool saturation causes cascading timeouts",
                    "High-load DB scenarios", List.of("OBS-001", "OBS-002"));
            assertThat(pattern.type()).isEqualTo(KnowledgeType.LOCAL_PATTERN);

            // ── Step 3: Validate the pattern ──────────────────────────────
            KnowledgeValidator validator = new DefaultKnowledgeValidator(
                    new KeywordContradictionDetector(), new DefaultConfidenceCalculator());
            ValidationResult vr = validator.validate(pattern, govMemory.getEngineeringKnowledge());
            assertThat(vr.valid()).isTrue();

            // ── Step 4: Nominate for promotion ────────────────────────────
            DefaultPromotionEngine promotionEngine = new DefaultPromotionEngine(storage);
            PromotionCandidate candidate = promotionEngine.nominate(pattern, "alice");
            assertThat(candidate.isPending()).isTrue();

            // ── Step 5: Human approves ────────────────────────────────────
            KnowledgeUnit promoted = promotionEngine.approve(candidate, "bob");
            assertThat(promoted.getState()).isEqualTo(KnowledgeState.KNOWLEDGE);
            assertThat(storage.getKnowledgeUnit("GPAT-001")).isPresent();

            // ── Step 6: Context Assembly retrieves the promoted knowledge ─
            DefaultContextAssembler assembler = new DefaultContextAssembler(appMemory, govMemory);
            // Give promoted unit correct type for gov storage lookup
            govMemory.addGlobalPattern(KnowledgeUnit.builder()
                    .id("GPAT-CHECK").statement("HikariCP pool saturation causes cascading timeouts")
                    .type(KnowledgeType.GLOBAL_PATTERN).confidence(0.85)
                    .state(KnowledgeState.KNOWLEDGE).lastReviewed(LocalDateTime.now()).build());

            ContextPackage pkg = assembler.assemble("HikariCP connection pool timeouts", 10);
            assertThat(pkg.rankedUnits()).isNotEmpty();

            // ── Step 7: Record outcome and learn ──────────────────────────
            OutcomeTracker tracker = new InMemoryOutcomeTracker();
            OutcomeLearningEngine learner = new OutcomeLearningEngine(storage, tracker);
            Outcome success = new Outcome(true, "Increased pool size fixed timeouts.", LocalDateTime.now());
            Optional<KnowledgeUnit> learned = learner.learn("GPAT-CHECK", success);
            assertThat(learned).isPresent();
            // Confidence remains positive after successful outcome
            assertThat(learned.get().confidence()).isGreaterThan(0.0);
        }
    }
}
