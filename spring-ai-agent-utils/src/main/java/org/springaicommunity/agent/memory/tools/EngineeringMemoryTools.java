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
package org.springaicommunity.agent.memory.tools;

import org.springaicommunity.agent.memory.ApplicationMemory;
import org.springaicommunity.agent.memory.KnowledgeEvolutionPipeline;
import org.springaicommunity.agent.memory.GovernanceMemory;
import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.KnowledgeType;
import org.springaicommunity.agent.memory.model.ProjectContext;
import org.springaicommunity.agent.memory.storage.MemoryStorage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring AI tools for agent interaction with the Engineering Knowledge Operating System (EKOS).
 * Exposes methods to record observations, decisions, incidents, and manage the promotion lifecycle.
 * 
 * @author Antigravity
 */
public class EngineeringMemoryTools {

	private final MemoryStorage storage;
	private final KnowledgeEvolutionPipeline learningPipeline;
	private final ApplicationMemory applicationMemory;
	private final GovernanceMemory systemMemory;

	public EngineeringMemoryTools(MemoryStorage storage, KnowledgeEvolutionPipeline learningPipeline,
			ApplicationMemory applicationMemory, GovernanceMemory systemMemory) {
		Assert.notNull(storage, "Storage must not be null");
		Assert.notNull(learningPipeline, "KnowledgeEvolutionPipeline must not be null");
		Assert.notNull(applicationMemory, "ApplicationMemory must not be null");
		Assert.notNull(systemMemory, "GovernanceMemory must not be null");
		this.storage = storage;
		this.learningPipeline = learningPipeline;
		this.applicationMemory = applicationMemory;
		this.systemMemory = systemMemory;
	}

	@Tool(name = "GetProjectContext", description = "Retrieve current project details (tech stack, team size, constraints).")
	public String getProjectContext() {
		Optional<ProjectContext> ctx = this.applicationMemory.getProjectContext();
		if (ctx.isEmpty()) {
			return "No project context set. Use UpdateProjectContext first.";
		}
		ProjectContext pc = ctx.get();
		return String.format("Project: %s\nStack: %s\nTeam Size: %d\nConstraints: %s",
				pc.projectName(), String.join(", ", pc.stack()), pc.teamSize(), String.join(", ", pc.constraints()));
	}

	@Tool(name = "UpdateProjectContext", description = "Set or update the metadata context of the active software project.")
	public String updateProjectContext(
			@ToolParam(description = "Name of the software project") String projectName,
			@ToolParam(description = "Comma-separated list of tech stack components") String stackText,
			@ToolParam(description = "Number of active engineers on the team") int teamSize,
			@ToolParam(description = "Comma-separated list of architectural constraints") String constraintsText) {

		List<String> stack = Arrays.stream(stackText.split(",")).map(String::trim).collect(Collectors.toList());
		List<String> constraints = Arrays.stream(constraintsText.split(",")).map(String::trim).collect(Collectors.toList());

		ProjectContext ctx = new ProjectContext(projectName, stack, teamSize, constraints, Map.of());
		this.applicationMemory.setProjectContext(ctx);

		return "Project context updated successfully for " + projectName;
	}

	@Tool(name = "RecordObservation", description = "Save a new immutable observation / raw fact about the project code or behavior.")
	public String recordObservation(
			@ToolParam(description = "Unique ID for the observation (e.g. OBS-001)") String id,
			@ToolParam(description = "Concise description of the observation") String statement,
			@ToolParam(description = "Context details or environment info") String context,
			@ToolParam(description = "Source file, line number, or trace ID") String eventSource) {

		KnowledgeUnit obs = this.learningPipeline.createObservation(id, statement, context, eventSource);
		return "Observation recorded successfully: " + obs.id() + " (Confidence: " + obs.confidence() + ")";
	}

	@Tool(name = "AddEvidence", description = "Add a new backing reference to an existing memory, growing its confidence score.")
	public String addEvidence(
			@ToolParam(description = "Target memory unit ID") String id,
			@ToolParam(description = "New evidence reference or log details") String newEvidence) {

		try {
			KnowledgeUnit unit = this.learningPipeline.addEvidence(id, newEvidence);
			return String.format("Evidence added to %s. Confidence updated to %.2f", unit.id(), unit.confidence());
		}
		catch (Exception e) {
			return "Error adding evidence: " + e.getMessage();
		}
	}

	@Tool(name = "RecordDecision", description = "Save a design, product, or technical decision made during development.")
	public String recordDecision(
			@ToolParam(description = "Unique ID for the decision (e.g. DEC-001)") String id,
			@ToolParam(description = "Decisive statement") String statement,
			@ToolParam(description = "Rationale, trade-offs, and details") String context,
			@ToolParam(description = "Source or team member proposing it") String source) {

		KnowledgeUnit dec = KnowledgeUnit.builder()
				.id(id)
				.statement(statement)
				.type(KnowledgeType.DECISION)
				.context(context)
				.source(source)
				.confidence(1.0) // Decisions are absolute facts, high confidence
				.build();
		this.applicationMemory.addDecision(dec);
		return "Decision " + id + " recorded successfully.";
	}

	@Tool(name = "RecordIncident", description = "Save a failure incident, outage details, or major bug description.")
	public String recordIncident(
			@ToolParam(description = "Unique ID (e.g. INC-001)") String id,
			@ToolParam(description = "What failed / went down") String statement,
			@ToolParam(description = "Downtime, resolution details, or post-mortem context") String context,
			@ToolParam(description = "Triggering source or logs") String source) {

		KnowledgeUnit inc = KnowledgeUnit.builder()
				.id(id)
				.statement(statement)
				.type(KnowledgeType.INCIDENT)
				.context(context)
				.source(source)
				.confidence(1.0)
				.build();
		this.applicationMemory.addIncident(inc);
		return "Incident " + id + " recorded successfully.";
	}

	@Tool(name = "ProposeLocalPattern", description = "Propose a localized pattern candidate derived from multiple observations.")
	public String proposeLocalPattern(
			@ToolParam(description = "Unique ID (e.g. PAT-001)") String id,
			@ToolParam(description = "Statement summarizing the pattern") String statement,
			@ToolParam(description = "Reasoning and pattern details") String context,
			@ToolParam(description = "Comma-separated observation IDs backing this pattern") String observationIdsText) {

		List<String> obsIds = Arrays.stream(observationIdsText.split(","))
				.map(String::trim)
				.collect(Collectors.toList());

		KnowledgeUnit pat = this.learningPipeline.proposeLocalPattern(id, statement, context, obsIds);
		return String.format("Local Pattern %s proposed with initial confidence %.2f", pat.id(), pat.confidence());
	}

	@Tool(name = "ProposeLocalOpinion", description = "Propose a local action recommendation based on a pattern.")
	public String proposeLocalOpinion(
			@ToolParam(description = "Unique ID (e.g. OPN-001)") String id,
			@ToolParam(description = "Action recommendation statement") String statement,
			@ToolParam(description = "Instructions and operational guidelines") String context,
			@ToolParam(description = "Backing pattern ID") String patternId) {

		KnowledgeUnit opn = this.learningPipeline.proposeLocalOpinion(id, statement, context, patternId);
		return String.format("Local Opinion %s proposed with initial confidence %.2f", opn.id(), opn.confidence());
	}

	@Tool(name = "PromoteToGlobalPattern", description = "Promote a local pattern to global system memory after human approval.")
	public String promoteToGlobalPattern(
			@ToolParam(description = "Local pattern ID to promote") String localId,
			@ToolParam(description = "Target global pattern ID (e.g. GPAT-001)") String globalId,
			@ToolParam(description = "Approving engineer name") String humanApprover) {

		try {
			KnowledgeUnit global = this.learningPipeline.promoteToGlobalPattern(localId, globalId, humanApprover);
			return String.format("Successfully promoted local pattern %s to global system memory: %s", localId, global.id());
		}
		catch (Exception e) {
			return "Promotion failed: " + e.getMessage();
		}
	}

	@Tool(name = "PromoteToGlobalOpinion", description = "Promote a local opinion to global system memory after human approval.")
	public String promoteToGlobalOpinion(
			@ToolParam(description = "Local opinion ID to promote") String localId,
			@ToolParam(description = "Target global opinion ID (e.g. GOPN-001)") String globalId,
			@ToolParam(description = "Approving engineer name") String humanApprover) {

		try {
			KnowledgeUnit global = this.learningPipeline.promoteToGlobalOpinion(localId, globalId, humanApprover);
			return String.format("Successfully promoted local opinion %s to global system memory: %s", localId, global.id());
		}
		catch (Exception e) {
			return "Promotion failed: " + e.getMessage();
		}
	}

	@Tool(name = "ListMemories", description = "List all registered memory units in the active session.")
	public String listMemories() {
		List<KnowledgeUnit> units = this.storage.listKnowledgeUnits();
		if (units.isEmpty()) {
			return "No memory units found.";
		}
		return units.stream()
				.map(u -> String.format("[%s] (%s) %s (Conf: %.2f)", u.id(), u.type(), u.statement(), u.confidence()))
				.collect(Collectors.joining("\n"));
	}
}
