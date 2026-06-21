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
package org.springaicommunity.agent.ikos;

import org.springaicommunity.agent.ikos.model.KnowledgeState;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.storage.MemoryStorage;
import org.springaicommunity.agent.ikos.synthesis.KeywordPatternDiscoveryEngine;
import org.springaicommunity.agent.ikos.synthesis.PatternDiscoveryEngine;
import org.springaicommunity.agent.ikos.validation.DefaultConfidenceCalculator;
import org.springaicommunity.agent.ikos.validation.DefaultKnowledgeValidator;
import org.springaicommunity.agent.ikos.validation.KeywordContradictionDetector;
import org.springaicommunity.agent.ikos.validation.KnowledgeValidator;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages the transition of experiences: Project Event -> Observation -> Local Pattern -> Local Opinion -> Global Memory.
 * Implements the growth of knowledge confidence based on the abundance of evidence, and the promotion rules
 * for moving localized project context memories to system memory.
 * 
 * @author Antigravity
 */
public class KnowledgeEvolutionPipeline {

	private final MemoryStorage storage;
	private final PatternDiscoveryEngine discoveryEngine;
	private final KnowledgeValidator validator;
	private final double baseConfidence = 0.3;
	private final double confidenceMultiplier = 0.2;

	/** Creates a pipeline with the default keyword-based discovery and validation engines. */
	public KnowledgeEvolutionPipeline(MemoryStorage storage) {
		this(storage,
				new KeywordPatternDiscoveryEngine(),
				new DefaultKnowledgeValidator(
						new KeywordContradictionDetector(),
						new DefaultConfidenceCalculator()));
	}

	/** Full constructor — inject custom discovery and validation engines. */
	public KnowledgeEvolutionPipeline(MemoryStorage storage,
									  PatternDiscoveryEngine discoveryEngine,
									  KnowledgeValidator validator) {
		Assert.notNull(storage, "Memory storage must not be null");
		Assert.notNull(discoveryEngine, "PatternDiscoveryEngine must not be null");
		Assert.notNull(validator, "KnowledgeValidator must not be null");
		this.storage = storage;
		this.discoveryEngine = discoveryEngine;
		this.validator = validator;
	}

	/**
	 * Creates an immutable raw observation from a project event.
	 * One evidence is sufficient for storage (confidence starts low).
	 */
	public KnowledgeUnit createObservation(String id, String statement, String context, String eventSource) {
		KnowledgeUnit observation = KnowledgeUnit.builder()
				.id(id)
				.statement(statement)
				.type(KnowledgeType.OBSERVATION)
				.context(context)
				.evidenceStrings(List.of(eventSource))
				.confidence(this.baseConfidence)
				.source(eventSource)
				.lastReviewed(LocalDateTime.now())
				.build();
		this.storage.saveKnowledgeUnit(observation);

		// ── Auto-discover patterns from the updated observation corpus ────────
		autoDiscoverPatterns();

		return observation;
	}

	/**
	 * Retrieves all auto-discovered pattern candidates (state = PATTERN_CANDIDATE).
	 * These are ready for human review and nomination for promotion.
	 */
	public List<KnowledgeUnit> getPatternCandidates() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.LOCAL_PATTERN)
				.stream()
				.filter(u -> u.getState() == KnowledgeState.PATTERN_CANDIDATE
						|| u.getState() == KnowledgeState.VALIDATED_PATTERN)
				.toList();
	}

	/**
	 * Triggers the full auto-discovery + auto-validation cycle.
	 * Called internally after each observation but also callable externally.
	 */
	public List<KnowledgeUnit> autoDiscoverPatterns() {
		List<KnowledgeUnit> observations = new ArrayList<>(this.storage.listKnowledgeUnitsByType(KnowledgeType.OBSERVATION));
		// Also include RISK_OBSERVATION units — they are the primary output of risk detection
		observations.addAll(this.storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION));
		List<KnowledgeUnit> discovered = this.discoveryEngine.discover(observations);

		List<KnowledgeUnit> result = new ArrayList<>();
		List<KnowledgeUnit> existingKnowledge = this.storage.listKnowledgeUnits();

		for (KnowledgeUnit candidate : discovered) {
			// Auto-validate against existing knowledge
			var vr = this.validator.validate(candidate, existingKnowledge);
			KnowledgeState newState = vr.valid()
					? KnowledgeState.VALIDATED_PATTERN
					: KnowledgeState.PATTERN_CANDIDATE;

			KnowledgeUnit toSave = candidate.toBuilder()
					.state(newState)
					.confidence(vr.confidenceScore() > 0 ? vr.confidenceScore() : candidate.confidence())
					.lastReviewed(LocalDateTime.now())
					.build();

			this.storage.saveKnowledgeUnit(toSave);
			result.add(toSave);
		}
		return result;
	}

	/**
	 * Adds a piece of evidence to an existing knowledge unit, automatically increasing its confidence.
	 */
	public KnowledgeUnit addEvidence(String id, String newEvidence) {
		Optional<KnowledgeUnit> optionalUnit = this.storage.getKnowledgeUnit(id);
		if (optionalUnit.isEmpty()) {
			throw new IllegalArgumentException("Knowledge unit not found: " + id);
		}
		KnowledgeUnit existing = optionalUnit.get();
		List<org.springaicommunity.agent.ikos.model.Evidence> updatedEvidence = existing.evidence() != null ? new ArrayList<>(existing.evidence()) : new ArrayList<>();
		boolean found = updatedEvidence.stream().anyMatch(e -> e.description().equals(newEvidence));
		if (!found) {
			updatedEvidence.add(new org.springaicommunity.agent.ikos.model.Evidence("legacy", newEvidence, 0.5));
		}

		// Calculate updated confidence: base + multiplier * additional evidence, capped at 1.0
		double updatedConfidence = Math.min(1.0, this.baseConfidence + (this.confidenceMultiplier * (updatedEvidence.size() - 1)));

		KnowledgeUnit updated = existing.toBuilder()
				.evidence(updatedEvidence)
				.confidence(updatedConfidence)
				.lastReviewed(LocalDateTime.now())
				.build();

		this.storage.saveKnowledgeUnit(updated);
		return updated;
	}

	/**
	 * Manually propose a local pattern from specific observations.
	 *
	 * @deprecated Pattern discovery is now automatic — use {@link #autoDiscoverPatterns()}
	 *             or simply record observations and check {@link #getPatternCandidates()}.
	 *             This method is retained for backward compatibility and controlled demos.
	 */
	@Deprecated(since = "0.11.0", forRemoval = false)
	public KnowledgeUnit proposeLocalPattern(String id, String statement, String context, List<String> observationIds) {
		// Aggregate confidence of backing observations
		double avgConfidence = observationIds.stream()
				.map(this.storage::getKnowledgeUnit)
				.flatMap(Optional::stream)
				.mapToDouble(KnowledgeUnit::confidence)
				.average()
				.orElse(this.baseConfidence);

		KnowledgeUnit localPattern = KnowledgeUnit.builder()
				.id(id)
				.statement(statement)
				.type(KnowledgeType.LOCAL_PATTERN)
				.context(context)
				.evidenceStrings(observationIds)
				.confidence(avgConfidence)
				.lastReviewed(LocalDateTime.now())
				.build();

		this.storage.saveKnowledgeUnit(localPattern);
		return localPattern;
	}

	/**
	 * Creates a candidate Local Opinion derived from a local pattern.
	 */
	public KnowledgeUnit proposeLocalOpinion(String id, String statement, String context, String patternId) {
		Optional<KnowledgeUnit> pattern = this.storage.getKnowledgeUnit(patternId);
		double confidence = pattern.map(KnowledgeUnit::confidence).orElse(this.baseConfidence);

		KnowledgeUnit localOpinion = KnowledgeUnit.builder()
				.id(id)
				.statement(statement)
				.type(KnowledgeType.LOCAL_OPINION)
				.context(context)
				.evidenceStrings(List.of(patternId))
				.confidence(confidence)
				.lastReviewed(LocalDateTime.now())
				.build();

		this.storage.saveKnowledgeUnit(localOpinion);
		return localOpinion;
	}

	/**
	 * Promotes a Local Pattern to a Global Pattern in System Memory.
	 * Rule: Multiple projects/evidences + High Confidence + Human Approval -> Promote.
	 */
	public KnowledgeUnit promoteToGlobalPattern(String localPatternId, String globalPatternId, String humanApprover) {
		Optional<KnowledgeUnit> optionalLocal = this.storage.getKnowledgeUnit(localPatternId);
		if (optionalLocal.isEmpty()) {
			throw new IllegalArgumentException("Local pattern not found: " + localPatternId);
		}
		KnowledgeUnit local = optionalLocal.get();
		if (local.type() != KnowledgeType.LOCAL_PATTERN) {
			throw new IllegalArgumentException("Unit " + localPatternId + " is not a local pattern");
		}

		// Ensure criteria: High Confidence (e.g. >= 0.5) and multiple evidences (or verified inputs)
		if (local.confidence() < 0.5) {
			throw new IllegalStateException("Confidence too low to promote: " + local.confidence() + ". Needs more evidence.");
		}

		KnowledgeUnit globalPattern = KnowledgeUnit.builder()
				.id(globalPatternId)
				.statement(local.statement())
				.type(KnowledgeType.GLOBAL_PATTERN)
				.context(local.context())
				.evidence(local.evidence())
				.confidence(Math.min(1.0, local.confidence() + 0.1)) // Boost confidence on promotion
				.exceptions(local.exceptions())
				.source(localPatternId + " (Approved by " + humanApprover + ")")
				.lastReviewed(LocalDateTime.now())
				.build();

		this.storage.saveKnowledgeUnit(globalPattern);
		
		// Clean up the local pattern after promotion
		this.storage.deleteKnowledgeUnit(localPatternId);

		return globalPattern;
	}

	/**
	 * Promotes a Local Opinion to a Global Opinion in System Memory.
	 */
	public KnowledgeUnit promoteToGlobalOpinion(String localOpinionId, String globalOpinionId, String humanApprover) {
		Optional<KnowledgeUnit> optionalLocal = this.storage.getKnowledgeUnit(localOpinionId);
		if (optionalLocal.isEmpty()) {
			throw new IllegalArgumentException("Local opinion not found: " + localOpinionId);
		}
		KnowledgeUnit local = optionalLocal.get();
		if (local.type() != KnowledgeType.LOCAL_OPINION) {
			throw new IllegalArgumentException("Unit " + localOpinionId + " is not a local opinion");
		}

		KnowledgeUnit globalOpinion = KnowledgeUnit.builder()
				.id(globalOpinionId)
				.statement(local.statement())
				.type(KnowledgeType.GLOBAL_OPINION)
				.context(local.context())
				.evidence(local.evidence())
				.confidence(local.confidence())
				.exceptions(local.exceptions())
				.source(localOpinionId + " (Approved by " + humanApprover + ")")
				.lastReviewed(LocalDateTime.now())
				.build();

		this.storage.saveKnowledgeUnit(globalOpinion);
		this.storage.deleteKnowledgeUnit(localOpinionId);

		return globalOpinion;
	}
}
