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
import org.springaicommunity.agent.ikos.storage.MemoryStorage;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

/**
 * Handles project-specific (application) memories, including observations, local patterns, local opinions,
 * decisions, incidents, and artifacts.
 * 
 * @author Antigravity
 */
public class ApplicationMemory {

	private final MemoryStorage storage;

	public ApplicationMemory(MemoryStorage storage) {
		Assert.notNull(storage, "Memory storage must not be null");
		this.storage = storage;
	}

	public void setProjectContext(ProjectContext context) {
		this.storage.saveProjectContext(context);
	}

	public Optional<ProjectContext> getProjectContext() {
		return this.storage.getProjectContext();
	}

	public void addObservation(KnowledgeUnit observation) {
		Assert.isTrue(observation.type() == KnowledgeType.OBSERVATION, "Must be of type OBSERVATION");
		this.storage.saveKnowledgeUnit(observation);
	}

	public void addLocalPattern(KnowledgeUnit pattern) {
		Assert.isTrue(pattern.type() == KnowledgeType.LOCAL_PATTERN, "Must be of type LOCAL_PATTERN");
		this.storage.saveKnowledgeUnit(pattern);
	}

	public void addLocalOpinion(KnowledgeUnit opinion) {
		Assert.isTrue(opinion.type() == KnowledgeType.LOCAL_OPINION, "Must be of type LOCAL_OPINION");
		this.storage.saveKnowledgeUnit(opinion);
	}

	public void addDecision(KnowledgeUnit decision) {
		Assert.isTrue(decision.type() == KnowledgeType.DECISION, "Must be of type DECISION");
		this.storage.saveKnowledgeUnit(decision);
	}

	public void addIncident(KnowledgeUnit incident) {
		Assert.isTrue(incident.type() == KnowledgeType.INCIDENT, "Must be of type INCIDENT");
		this.storage.saveKnowledgeUnit(incident);
	}

	public void addArtifact(KnowledgeUnit artifact) {
		Assert.isTrue(artifact.type() == KnowledgeType.ARTIFACT, "Must be of type ARTIFACT");
		this.storage.saveKnowledgeUnit(artifact);
	}

	public List<KnowledgeUnit> getObservations() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.OBSERVATION);
	}

	public List<KnowledgeUnit> getLocalPatterns() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.LOCAL_PATTERN);
	}

	public List<KnowledgeUnit> getLocalOpinions() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.LOCAL_OPINION);
	}

	public List<KnowledgeUnit> getDecisions() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.DECISION);
	}

	public List<KnowledgeUnit> getIncidents() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.INCIDENT);
	}

	public List<KnowledgeUnit> getArtifacts() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.ARTIFACT);
	}
}
