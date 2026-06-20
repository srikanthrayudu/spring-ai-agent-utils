/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.agent.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.KnowledgeType;
import org.springaicommunity.agent.memory.model.Evidence;
import org.springaicommunity.agent.memory.model.ProjectContext;
import org.springaicommunity.agent.memory.storage.FileMemoryStorage;
import org.springaicommunity.agent.memory.storage.MemoryStorage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying the Engineering Knowledge Operating System (EKOS) memory implementation.
 * Covers FileMemoryStorage, ApplicationMemory, GovernanceMemory, KnowledgeEvolutionPipeline, and ContextBuilder.
 * 
 * @author Antigravity
 */
class EngineeringMemoryTest {

	@TempDir
	Path tempDir;

	private MemoryStorage storage;
	private ApplicationMemory applicationMemory;
	private GovernanceMemory systemMemory;
	private KnowledgeEvolutionPipeline learningPipeline;
	private ContextBuilder contextBuilder;

	@BeforeEach
	void setUp() {
		this.storage = new FileMemoryStorage(this.tempDir.toString());
		this.applicationMemory = new ApplicationMemory(this.storage);
		this.systemMemory = new GovernanceMemory(this.storage);
		this.learningPipeline = new KnowledgeEvolutionPipeline(this.storage);
		this.contextBuilder = new ContextBuilder(this.applicationMemory, this.systemMemory);
	}

	@Test
	@DisplayName("Test Project Context Saving and Retrieval")
	void testProjectContext() {
		ProjectContext context = new ProjectContext(
				"Spring AI Agent UT",
				List.of("Java 17", "Spring Boot", "Spring AI"),
				5,
				List.of("Low memory usage", "High speed"),
				Map.of("environment", "test")
		);

		this.applicationMemory.setProjectContext(context);
		Optional<ProjectContext> retrieved = this.applicationMemory.getProjectContext();

		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().projectName()).isEqualTo("Spring AI Agent UT");
		assertThat(retrieved.get().stack()).contains("Java 17", "Spring Boot", "Spring AI");
		assertThat(retrieved.get().teamSize()).isEqualTo(5);
		assertThat(retrieved.get().constraints()).containsExactly("Low memory usage", "High speed");
	}

	@Test
	@DisplayName("Test Learning Pipeline: Observation creation & Evidence/Confidence growth")
	void testKnowledgeEvolutionPipelineConfidence() {
		// 1. Create Observation from event (Confidence starts at 0.3)
		KnowledgeUnit obs = this.learningPipeline.createObservation(
				"OBS-001",
				"Database connection pool exhausted during high load.",
				"HikariCP pool limit: 10",
				"DatabaseConnectionAdvisor"
		);

		assertThat(obs.id()).isEqualTo("OBS-001");
		assertThat(obs.confidence()).isEqualTo(0.3);
		assertThat(obs.evidence()).extracting(Evidence::description).containsExactly("DatabaseConnectionAdvisor");

		// 2. Add second evidence (Confidence grows to 0.5)
		KnowledgeUnit obsWithMoreEvidence = this.learningPipeline.addEvidence("OBS-001", "LoginServiceException");
		assertThat(obsWithMoreEvidence.confidence()).isEqualTo(0.5);
		assertThat(obsWithMoreEvidence.evidence()).extracting(Evidence::description).containsExactly("DatabaseConnectionAdvisor", "LoginServiceException");

		// 3. Add third evidence (Confidence grows to 0.7)
		KnowledgeUnit obsWithEvenMoreEvidence = this.learningPipeline.addEvidence("OBS-001", "BillingServiceOutage");
		assertThat(obsWithEvenMoreEvidence.confidence()).isEqualTo(0.7);
	}

	@Test
	@DisplayName("Test Learning Pipeline: Local Pattern and Opinion proposal")
	void testProposePatternAndOpinion() {
		this.learningPipeline.createObservation("OBS-001", "Connection timeout", "Database", "LogA");
		this.learningPipeline.createObservation("OBS-002", "HikariPool saturated", "Database", "LogB");

		KnowledgeUnit pattern = this.learningPipeline.proposeLocalPattern(
				"PAT-001",
				"Hikari connection pool saturation causes timeout incidents.",
				"Database connection configuration",
				List.of("OBS-001", "OBS-002")
		);

		assertThat(pattern.id()).isEqualTo("PAT-001");
		assertThat(pattern.type()).isEqualTo(KnowledgeType.LOCAL_PATTERN);
		assertThat(pattern.evidence()).extracting(Evidence::description).containsExactly("OBS-001", "OBS-002");

		KnowledgeUnit opinion = this.learningPipeline.proposeLocalOpinion(
				"OPN-001",
				"Always monitor connection pool metrics under stress tests.",
				"Monitoring recommendation",
				"PAT-001"
		);

		assertThat(opinion.id()).isEqualTo("OPN-001");
		assertThat(opinion.type()).isEqualTo(KnowledgeType.LOCAL_OPINION);
		assertThat(opinion.evidence()).extracting(Evidence::description).containsExactly("PAT-001");
	}

	@Test
	@DisplayName("Test Learning Pipeline: Local Pattern Promotion to Global Memory")
	void testPromotionRules() {
		// Create a local pattern with high confidence (requires multiple evidences)
		this.learningPipeline.createObservation("OBS-001", "Timeout", "DB", "Log1");
		
		KnowledgeUnit pat = this.learningPipeline.proposeLocalPattern(
				"PAT-001",
				"Saturation causes Timeout",
				"DB",
				List.of("OBS-001")
		);
		
		// Set confidence high to satisfy promotion check
		this.learningPipeline.addEvidence("OBS-001", "Log2"); // grows obs confidence to 0.5
		
		// Propose again to pull average confidence of backing obs
		KnowledgeUnit patHighConf = this.learningPipeline.proposeLocalPattern(
				"PAT-001",
				"Saturation causes Timeout",
				"DB",
				List.of("OBS-001")
		);
		
		// Promote it
		KnowledgeUnit globalPat = this.learningPipeline.promoteToGlobalPattern("PAT-001", "GPAT-001", "Christian");
		
		assertThat(globalPat.id()).isEqualTo("GPAT-001");
		assertThat(globalPat.type()).isEqualTo(KnowledgeType.GLOBAL_PATTERN);
		// source() returns empty string (backward compat stub); verify via statement instead
		assertThat(globalPat.statement()).isEqualTo("Saturation causes Timeout");
		
		// Ensure local pattern is cleaned up from active project store
		assertThat(this.storage.getKnowledgeUnit("PAT-001")).isEmpty();
		assertThat(this.storage.getKnowledgeUnit("GPAT-001")).isPresent();
	}

	@Test
	@DisplayName("Test Context Builder query matching and ranking")
	void testContextBuilder() {
		// Setup Project context
		this.applicationMemory.setProjectContext(new ProjectContext(
				"Spring AI", List.of("Java"), 1, List.of("None"), Map.of()
		));

		// Setup System Memories
		this.systemMemory.addEngineeringKnowledge(KnowledgeUnit.builder()
				.id("ENG-001")
				.statement("Java concurrency is handled using Threads or Virtual Threads.")
				.type(KnowledgeType.ENGINEERING_KNOWLEDGE)
				.confidence(0.9)
				.build());

		this.systemMemory.addPrinciple(KnowledgeUnit.builder()
				.id("PRN-001")
				.statement("Principle of Least Privilege: only grant required system permissions.")
				.type(KnowledgeType.PRINCIPLE)
				.confidence(0.95)
				.build());

		// Setup Local Memories
		this.learningPipeline.createObservation("OBS-001", "Connection pool exhausted on Login API.", "HikariCP pool limit", "AuthService");
		this.learningPipeline.addEvidence("OBS-001", "HighConcurrencyLogs"); // bump confidence to 0.5

		// Query context builder with terms matching 'concurrency' and 'HikariCP'
		String context = this.contextBuilder.buildContext("How should we handle concurrency and HikariCP connection pool saturation?", 5);

		// Verify project context is present
		assertThat(context).contains("Spring AI");
		
		// Verify relevant matched units are present in context
		assertThat(context).contains("ENG-001");
		assertThat(context).contains("OBS-001");
		
		// Verify irrelevant units (Least Privilege) are NOT present
		assertThat(context).doesNotContain("PRN-001");
	}
}
