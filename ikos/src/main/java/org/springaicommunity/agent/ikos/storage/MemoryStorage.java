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
package org.springaicommunity.agent.ikos.storage;

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.model.ProjectContext;

import java.util.List;
import java.util.Optional;

/**
 * Interface defining persistence operations for the memory architecture.
 * Supports storing both local project-specific data (Application Memory) and global shared data (System Memory).
 * 
 * @author Antigravity
 */
public interface MemoryStorage {

	/**
	 * Persists or updates the current project context metadata.
	 */
	void saveProjectContext(ProjectContext context);

	/**
	 * Retrieves the current project context.
	 */
	Optional<ProjectContext> getProjectContext();

	/**
	 * Persists a new or updated knowledge unit.
	 */
	void saveKnowledgeUnit(KnowledgeUnit unit);

	/**
	 * Retrieves a knowledge unit by its unique identifier.
	 */
	Optional<KnowledgeUnit> getKnowledgeUnit(String id);

	/**
	 * Lists all stored knowledge units across both System and Application memories.
	 */
	List<KnowledgeUnit> listKnowledgeUnits();

	/**
	 * Lists all stored knowledge units of a specific type.
	 */
	List<KnowledgeUnit> listKnowledgeUnitsByType(KnowledgeType type);

	/**
	 * Deletes a knowledge unit by its unique identifier.
	 */
	void deleteKnowledgeUnit(String id);
}
