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

import tools.jackson.databind.ObjectMapper;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.model.ProjectContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * File-based JSON implementation of {@link MemoryStorage}.
 * Scopes files to separate folders under a designated root directory:
 * - {@code project/} for Application Memory (observations, local patterns, local opinions, decisions, incidents, artifacts).
 * - {@code system/} for System Memory (engineering knowledge, principles, global patterns, global opinions, tool knowledge).
 * 
 * @author Antigravity
 */
public class FileMemoryStorage implements MemoryStorage {

	private final Path projectDir;
	private final Path systemDir;
	private final ObjectMapper objectMapper;

	public FileMemoryStorage(String rootPath) {
		Path rootDir = Paths.get(rootPath).toAbsolutePath().normalize();
		this.projectDir = rootDir.resolve("project");
		this.systemDir = rootDir.resolve("system");
		this.objectMapper = new ObjectMapper();

		try {
			Files.createDirectories(this.projectDir);
			Files.createDirectories(this.systemDir);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to create memory persistence directories at: " + rootDir, e);
		}
	}

	@Override
	public void saveProjectContext(ProjectContext context) {
		Path contextFile = this.projectDir.resolve("project-context.json");
		try {
			this.objectMapper.writeValue(contextFile.toFile(), context);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to save project context", e);
		}
	}

	@Override
	public Optional<ProjectContext> getProjectContext() {
		Path contextFile = this.projectDir.resolve("project-context.json");
		if (!Files.exists(contextFile)) {
			return Optional.empty();
		}
		try {
			return Optional.of(this.objectMapper.readValue(contextFile.toFile(), ProjectContext.class));
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to read project context", e);
		}
	}

	@Override
	public void saveKnowledgeUnit(KnowledgeUnit unit) {
		Path targetDir = getDirectoryForType(unit.type());
		Path file = targetDir.resolve(unit.id() + ".json");
		try {
			this.objectMapper.writeValue(file.toFile(), unit);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to save knowledge unit: " + unit.id(), e);
		}
	}

	@Override
	public Optional<KnowledgeUnit> getKnowledgeUnit(String id) {
		Path file = this.projectDir.resolve(id + ".json");
		if (!Files.exists(file)) {
			file = this.systemDir.resolve(id + ".json");
		}
		if (!Files.exists(file)) {
			return Optional.empty();
		}
		try {
			return Optional.of(this.objectMapper.readValue(file.toFile(), KnowledgeUnit.class));
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to read knowledge unit: " + id, e);
		}
	}

	@Override
	public List<KnowledgeUnit> listKnowledgeUnits() {
		List<KnowledgeUnit> units = new ArrayList<>();
		readUnitsFromDir(this.projectDir, units);
		readUnitsFromDir(this.systemDir, units);
		return units;
	}

	@Override
	public List<KnowledgeUnit> listKnowledgeUnitsByType(KnowledgeType type) {
		Path targetDir = getDirectoryForType(type);
		List<KnowledgeUnit> units = new ArrayList<>();
		if (!Files.exists(targetDir)) {
			return units;
		}
		try (Stream<Path> stream = Files.list(targetDir)) {
			stream.filter(path -> path.toString().endsWith(".json"))
				.forEach(path -> {
					try {
						KnowledgeUnit unit = this.objectMapper.readValue(path.toFile(), KnowledgeUnit.class);
						if (unit.type() == type) {
							units.add(unit);
						}
					}
					catch (Exception ignored) {
					}
				});
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to list knowledge units for type: " + type, e);
		}
		return units;
	}

	@Override
	public void deleteKnowledgeUnit(String id) {
		Path file = this.projectDir.resolve(id + ".json");
		if (!Files.exists(file)) {
			file = this.systemDir.resolve(id + ".json");
		}
		if (Files.exists(file)) {
			try {
				Files.delete(file);
			}
			catch (IOException e) {
				throw new RuntimeException("Failed to delete knowledge unit: " + id, e);
			}
		}
	}

	private Path getDirectoryForType(KnowledgeType type) {
		if (type == KnowledgeType.ENGINEERING_KNOWLEDGE || type == KnowledgeType.PRINCIPLE
				|| type == KnowledgeType.GLOBAL_PATTERN || type == KnowledgeType.GLOBAL_OPINION
				|| type == KnowledgeType.TOOL_KNOWLEDGE || type == KnowledgeType.SECURITY_KNOWLEDGE) {
			return this.systemDir;
		}
		return this.projectDir;
	}

	private void readUnitsFromDir(Path dir, List<KnowledgeUnit> list) {
		if (!Files.exists(dir)) {
			return;
		}
		try (Stream<Path> stream = Files.list(dir)) {
			stream.filter(path -> path.toString().endsWith(".json") && !path.getFileName().toString().equals("project-context.json"))
				.forEach(path -> {
					try {
						list.add(this.objectMapper.readValue(path.toFile(), KnowledgeUnit.class));
					}
					catch (Exception ignored) {
					}
				});
		}
		catch (IOException ignored) {
		}
	}
}
