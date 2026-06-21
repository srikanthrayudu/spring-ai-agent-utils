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
import org.springaicommunity.agent.ikos.storage.MemoryStorage;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Handles global engineering memory shared across multiple projects.
 * Includes principles, general engineering knowledge, global patterns, global opinions, and tool knowledge.
 * 
 * @author Antigravity
 */
public class GovernanceMemory {

	private final MemoryStorage storage;

	public GovernanceMemory(MemoryStorage storage) {
		Assert.notNull(storage, "Memory storage must not be null");
		this.storage = storage;
	}

	public void addEngineeringKnowledge(KnowledgeUnit knowledge) {
		Assert.isTrue(knowledge.type() == KnowledgeType.ENGINEERING_KNOWLEDGE, "Must be of type ENGINEERING_KNOWLEDGE");
		this.storage.saveKnowledgeUnit(knowledge);
	}

	public void addPrinciple(KnowledgeUnit principle) {
		Assert.isTrue(principle.type() == KnowledgeType.PRINCIPLE, "Must be of type PRINCIPLE");
		this.storage.saveKnowledgeUnit(principle);
	}

	public void addGlobalPattern(KnowledgeUnit pattern) {
		Assert.isTrue(pattern.type() == KnowledgeType.GLOBAL_PATTERN, "Must be of type GLOBAL_PATTERN");
		this.storage.saveKnowledgeUnit(pattern);
	}

	public void addGlobalOpinion(KnowledgeUnit opinion) {
		Assert.isTrue(opinion.type() == KnowledgeType.GLOBAL_OPINION, "Must be of type GLOBAL_OPINION");
		this.storage.saveKnowledgeUnit(opinion);
	}

	public void addToolKnowledge(KnowledgeUnit toolKnowledge) {
		Assert.isTrue(toolKnowledge.type() == KnowledgeType.TOOL_KNOWLEDGE, "Must be of type TOOL_KNOWLEDGE");
		this.storage.saveKnowledgeUnit(toolKnowledge);
	}

	public List<KnowledgeUnit> getEngineeringKnowledge() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.ENGINEERING_KNOWLEDGE);
	}

	public List<KnowledgeUnit> getPrinciples() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.PRINCIPLE);
	}

	public List<KnowledgeUnit> getGlobalPatterns() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.GLOBAL_PATTERN);
	}

	public List<KnowledgeUnit> getGlobalOpinions() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.GLOBAL_OPINION);
	}

	public List<KnowledgeUnit> getToolKnowledge() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.TOOL_KNOWLEDGE);
	}

	// --- IKOS (Identity Knowledge Operating System) methods ---

	public void addSecurityKnowledge(KnowledgeUnit knowledge) {
		Assert.isTrue(knowledge.type() == KnowledgeType.SECURITY_KNOWLEDGE, "Must be of type SECURITY_KNOWLEDGE");
		this.storage.saveKnowledgeUnit(knowledge);
	}

	public List<KnowledgeUnit> getSecurityKnowledge() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.SECURITY_KNOWLEDGE);
	}

	public List<KnowledgeUnit> getRiskObservations() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION);
	}

	public List<KnowledgeUnit> getSecurityIncidents() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.SECURITY_INCIDENT);
	}

	public List<KnowledgeUnit> getRemediationActions() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.REMEDIATION_ACTION);
	}

	public List<KnowledgeUnit> getAuditFindings() {
		return this.storage.listKnowledgeUnitsByType(KnowledgeType.AUDIT_FINDING);
	}
}
