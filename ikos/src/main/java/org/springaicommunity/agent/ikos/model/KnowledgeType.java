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
package org.springaicommunity.agent.ikos.model;

/**
 * Defines the types of knowledge and memory objects within the Engineering Knowledge Operating System.
 * 
 * @author Antigravity
 */
public enum KnowledgeType {
	// --- Application Memory (Project-specific) ---
	/** Raw facts, immutable, requires only one piece of evidence. */
	OBSERVATION,
	
	/** Derived relationships or generalizations from observations. */
	LOCAL_PATTERN,
	
	/** Actionable recommendations based on local patterns. */
	LOCAL_OPINION,
	
	/** Design or architecture decisions made during development. */
	DECISION,
	
	/** Outages, bugs, failures, and connection pool saturations. */
	INCIDENT,
	
	/** Output files, templates, or schemas created by the project. */
	ARTIFACT,

	// --- System Memory (Global across projects) ---
	/** General imported/learned knowledge (e.g. CAP, concurrency rules). */
	ENGINEERING_KNOWLEDGE,
	
	/** Near-universal, fundamental rules (e.g. Least Privilege). */
	PRINCIPLE,
	
	/** Patterns validated across multiple projects. */
	GLOBAL_PATTERN,
	
	/** Recommended global actions (e.g. Always deploy to staging). */
	GLOBAL_OPINION,
	
	/** Specific rules and techniques regarding agent tools. */
	TOOL_KNOWLEDGE,

	// --- KEF Lifecycle types ---
	/** Actionable recommendation generated from a validated pattern. */
	RECOMMENDATION,

	/** A unit nominated for promotion to GovernanceMemory; pending human review. */
	PROMOTION_CANDIDATE,

	// --- IKOS (Identity Knowledge Operating System) types ---
	/** Security risk detected from identity analysis (offboarding gap, dormant admin, etc.). */
	RISK_OBSERVATION,

	/** Security incident: SoD violation, unauthorized access, token abuse, etc. */
	SECURITY_INCIDENT,

	/** Reference to a governance policy (PAM-004, NIST SP 800-53, internal control). */
	POLICY_REFERENCE,

	/** Actionable remediation step: disable account, revoke tokens, etc. */
	REMEDIATION_ACTION,

	/** Compliance or audit finding from an identity review. */
	AUDIT_FINDING,

	/** Cross-platform identity correlation link. */
	IDENTITY_CORRELATION,

	/** Effective privilege snapshot after group traversal and permission expansion. */
	PRIVILEGE_PROFILE,

	/** Validated, human-approved security knowledge (IKOS equivalent of ENGINEERING_KNOWLEDGE). */
	SECURITY_KNOWLEDGE
}
