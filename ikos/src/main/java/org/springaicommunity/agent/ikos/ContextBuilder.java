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

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.model.ProjectContext;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds context for queries by retrieving relevant memories from both System and Application Memory,
 * ranking them by the product of keyword relevance and unit confidence, and building an augmented prompt segment.
 * 
 * @author Antigravity
 */
public class ContextBuilder {

	private final ApplicationMemory applicationMemory;
	private final GovernanceMemory systemMemory;

	public ContextBuilder(ApplicationMemory applicationMemory, GovernanceMemory systemMemory) {
		Assert.notNull(applicationMemory, "Application memory must not be null");
		Assert.notNull(systemMemory, "System memory must not be null");
		this.applicationMemory = applicationMemory;
		this.systemMemory = systemMemory;
	}

	/**
	 * Builds a formatted string containing project context and ranked, typed memories related to the query.
	 * 
	 * @param question the user request or question
	 * @param limit maximum number of memory units to include in the context
	 * @return formatted string ready to be injected into the system prompt
	 */
	public String buildContext(String question, int limit) {
		List<KnowledgeUnit> allMemories = new ArrayList<>();
		
		// Collect application memories
		allMemories.addAll(this.applicationMemory.getObservations());
		allMemories.addAll(this.applicationMemory.getLocalPatterns());
		allMemories.addAll(this.applicationMemory.getLocalOpinions());
		allMemories.addAll(this.applicationMemory.getDecisions());
		allMemories.addAll(this.applicationMemory.getIncidents());
		allMemories.addAll(this.applicationMemory.getArtifacts());

		// Collect system memories
		allMemories.addAll(this.systemMemory.getEngineeringKnowledge());
		allMemories.addAll(this.systemMemory.getPrinciples());
		allMemories.addAll(this.systemMemory.getGlobalPatterns());
		allMemories.addAll(this.systemMemory.getGlobalOpinions());
		allMemories.addAll(this.systemMemory.getToolKnowledge());

		// Tokenize and normalize keywords from the question
		Set<String> queryKeywords = extractKeywords(question);

		// Score all memories — using parallel lists to avoid inner-class generation
		// (exec-maven-plugin classloader cannot resolve synthetic $RankedMemory classes)
		List<KnowledgeUnit> scoredUnits = new ArrayList<>();
		List<Double> scoredValues = new ArrayList<>();
		for (KnowledgeUnit unit : allMemories) {
			double relevance = calculateRelevance(queryKeywords, unit);
			double score = relevance * unit.confidence();
			if (score > 0.05) {
				scoredUnits.add(unit);
				scoredValues.add(score);
			}
		}

		// Sort by score descending using index-based sorting
		Integer[] indices = new Integer[scoredUnits.size()];
		for (int i = 0; i < indices.length; i++) indices[i] = i;
		java.util.Arrays.sort(indices, (a, b) -> Double.compare(scoredValues.get(b), scoredValues.get(a)));

		List<KnowledgeUnit> rankedUnits = new ArrayList<>();
		for (int i = 0; i < Math.min(limit, indices.length); i++) {
			rankedUnits.add(scoredUnits.get(indices[i]));
		}

		StringBuilder sb = new StringBuilder();

		// Append Project Context first (if available)
		this.applicationMemory.getProjectContext().ifPresent(pc -> {
			sb.append("--- PROJECT CONTEXT ---\n");
			sb.append("Project Name: ").append(pc.projectName()).append("\n");
			sb.append("Tech Stack: ").append(String.join(", ", pc.stack())).append("\n");
			sb.append("Team Size: ").append(pc.teamSize()).append(" Engineers\n");
			if (pc.constraints() != null && !pc.constraints().isEmpty()) {
				sb.append("Constraints:\n");
				for (String constraint : pc.constraints()) {
					sb.append("  - ").append(constraint).append("\n");
				}
			}
			if (pc.additionalMetadata() != null && !pc.additionalMetadata().isEmpty()) {
				sb.append("Metadata:\n");
				pc.additionalMetadata().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
			}
			sb.append("\n");
		});

		// Group retrieved memories by memory type for structured presentation
		Map<KnowledgeType, List<KnowledgeUnit>> grouped = rankedUnits.stream()
				.collect(Collectors.groupingBy(KnowledgeUnit::type));

		// Build formatted sections
		appendSection(sb, "Engineering Knowledge", grouped.get(KnowledgeType.ENGINEERING_KNOWLEDGE));
		appendSection(sb, "Core Principles", grouped.get(KnowledgeType.PRINCIPLE));
		appendSection(sb, "Observations", grouped.get(KnowledgeType.OBSERVATION));
		appendSection(sb, "Local Patterns", grouped.get(KnowledgeType.LOCAL_PATTERN));
		appendSection(sb, "Local Opinions", grouped.get(KnowledgeType.LOCAL_OPINION));
		appendSection(sb, "Global Patterns", grouped.get(KnowledgeType.GLOBAL_PATTERN));
		appendSection(sb, "Global Opinions", grouped.get(KnowledgeType.GLOBAL_OPINION));
		appendSection(sb, "Decisions Made", grouped.get(KnowledgeType.DECISION));
		appendSection(sb, "Incidents", grouped.get(KnowledgeType.INCIDENT));
		appendSection(sb, "Artifacts", grouped.get(KnowledgeType.ARTIFACT));
		appendSection(sb, "Tool Knowledge", grouped.get(KnowledgeType.TOOL_KNOWLEDGE));

		return sb.toString();
	}

	private void appendSection(StringBuilder sb, String title, List<KnowledgeUnit> units) {
		if (units == null || units.isEmpty()) {
			return;
		}
		sb.append("--- ").append(title.toUpperCase()).append(" ---\n");
		for (KnowledgeUnit unit : units) {
			sb.append("- [").append(unit.id()).append("] ").append(unit.statement()).append("\n");
			if (unit.context() != null && !unit.context().toString().isBlank()) {
				sb.append("  Context: ").append(unit.context()).append("\n");
			}
			if (unit.exceptions() != null && !unit.exceptions().isEmpty()) {
				sb.append("  Exceptions: ").append(unit.exceptions().stream().map(Object::toString).collect(Collectors.joining(", "))).append("\n");
			}
			if (unit.evidence() != null && !unit.evidence().isEmpty()) {
				sb.append("  Evidence: ").append(unit.evidence().stream().map(Object::toString).collect(Collectors.joining(", "))).append("\n");
			}
			sb.append("  Confidence: ").append(String.format("%.2f", unit.confidence())).append("\n");
		}
		sb.append("\n");
	}

	private Set<String> extractKeywords(String text) {
		if (text == null || text.isBlank()) {
			return Collections.emptySet();
		}
		Set<String> keywords = new HashSet<>();
		String[] tokens = text.toLowerCase().split("\\W+");
		for (String token : tokens) {
			if (token.length() > 3) {
				keywords.add(token);
			}
		}
		return keywords;
	}

	private double calculateRelevance(Set<String> queryKeywords, KnowledgeUnit unit) {
		if (queryKeywords.isEmpty()) {
			return 0.0;
		}
		String searchable = (unit.statement() + " " +
				(unit.context() != null ? unit.context().toString() : "") + " " +
				(unit.exceptions() != null ? unit.exceptions().toString() : "") + " " +
				(unit.source() != null ? unit.source() : "")).toLowerCase();

		long matches = queryKeywords.stream()
				.filter(searchable::contains)
				.count();

		return (double) matches / queryKeywords.size();
	}
}
