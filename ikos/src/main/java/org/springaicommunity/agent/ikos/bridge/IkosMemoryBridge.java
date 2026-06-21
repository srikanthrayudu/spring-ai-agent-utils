/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.bridge;

import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.storage.MemoryStorage;
import org.springaicommunity.agent.tools.AutoMemoryTools;

import java.util.List;

/**
 * Bridges the IKOS knowledge store into the agent-utils {@link AutoMemoryTools}
 * persistent memory system.
 *
 * <p>When IKOS detects a risk, creates a pattern, or records an incident,
 * this bridge syncs it into the agent's memory so the LLM remembers
 * findings across conversations.
 *
 * <p>Two-way sync:
 * <ul>
 *   <li>{@link #syncToMemory(KnowledgeUnit)} — IKOS → AutoMemoryTools</li>
 *   <li>{@link #syncAllRisksToMemory()} — Bulk export of all risks</li>
 *   <li>{@link #updateMemoryIndex()} — Rebuilds MEMORY.md index</li>
 * </ul>
 *
 * @author Antigravity
 */
public class IkosMemoryBridge {

    private final AutoMemoryTools memoryTools;
    private final MemoryStorage ikosStorage;

    public IkosMemoryBridge(AutoMemoryTools memoryTools, MemoryStorage ikosStorage) {
        this.memoryTools = memoryTools;
        this.ikosStorage = ikosStorage;
    }

    /**
     * Sync a single IKOS knowledge unit into the agent's persistent memory.
     *
     * @param unit the knowledge unit to persist as agent memory
     * @return result message from the memory tool
     */
    public String syncToMemory(KnowledgeUnit unit) {
        String subDir = switch (unit.type()) {
            case RISK_OBSERVATION -> "ikos/risks";
            case SECURITY_INCIDENT -> "ikos/incidents";
            case LOCAL_PATTERN, GLOBAL_PATTERN, PROMOTION_CANDIDATE -> "ikos/patterns";
            case REMEDIATION_ACTION -> "ikos/remediations";
            default -> "ikos/knowledge";
        };

        String memoryType = switch (unit.type()) {
            case RISK_OBSERVATION, SECURITY_INCIDENT -> "project";
            case GLOBAL_PATTERN -> "reference";
            default -> "project";
        };

        String severity = extractSeverity(unit);
        String yaml = """
                ---
                name: %s
                description: %s [%s] %s
                type: %s
                ---
                # %s

                **Type:** %s
                **Confidence:** %.0f%%
                **Source:** %s
                %s
                ## Context
                %s
                """.formatted(
                unit.id(),
                unit.type().name(), severity, truncate(unit.statement(), 120),
                memoryType,
                truncate(unit.statement(), 200),
                unit.type().name(),
                unit.confidence() * 100,
                "IKOS",
                severity.isEmpty() ? "" : "**Severity:** " + severity + "\n",
                unit.context() != null ? unit.context().toString() : "N/A");

        String path = subDir + "/" + sanitizeFilename(unit.id()) + ".md";
        return memoryTools.memoryCreate(path, yaml);
    }

    /**
     * Bulk-sync all risk observations from IKOS into agent memory.
     *
     * @return number of risks synced
     */
    public int syncAllRisksToMemory() {
        List<KnowledgeUnit> risks = ikosStorage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION);
        int synced = 0;
        for (KnowledgeUnit risk : risks) {
            String result = syncToMemory(risk);
            if (result.startsWith("Successfully")) {
                synced++;
            }
        }
        return synced;
    }

    /**
     * Rebuild the MEMORY.md index with all synced IKOS knowledge.
     *
     * @return result message
     */
    public String updateMemoryIndex() {
        List<KnowledgeUnit> all = ikosStorage.listKnowledgeUnits();

        StringBuilder index = new StringBuilder();
        index.append("# IKOS Knowledge Index\n\n");

        // Group by type
        for (KnowledgeType type : KnowledgeType.values()) {
            List<KnowledgeUnit> ofType = all.stream()
                    .filter(u -> u.type() == type)
                    .toList();
            if (!ofType.isEmpty()) {
                index.append("## ").append(type.name()).append(" (").append(ofType.size()).append(")\n\n");
                for (KnowledgeUnit unit : ofType) {
                    String subDir = switch (type) {
                        case RISK_OBSERVATION -> "ikos/risks";
                        case SECURITY_INCIDENT -> "ikos/incidents";
                        case LOCAL_PATTERN, GLOBAL_PATTERN, PROMOTION_CANDIDATE -> "ikos/patterns";
                        case REMEDIATION_ACTION -> "ikos/remediations";
                        default -> "ikos/knowledge";
                    };
                    String path = subDir + "/" + sanitizeFilename(unit.id()) + ".md";
                    index.append("- [").append(unit.id()).append("](").append(path)
                            .append(") — ").append(truncate(unit.statement(), 100)).append("\n");
                }
                index.append("\n");
            }
        }

        // Try to create or update MEMORY.md
        String existing = memoryTools.memoryView("MEMORY.md", null);
        if (existing.startsWith("Error")) {
            return memoryTools.memoryCreate("MEMORY.md", index.toString());
        } else {
            // Replace entire content
            return memoryTools.memoryStrReplace("MEMORY.md", existing.substring(existing.indexOf("# IKOS")),
                    index.toString());
        }
    }

    /**
     * Get the underlying AutoMemoryTools instance for direct access.
     */
    public AutoMemoryTools memoryTools() {
        return memoryTools;
    }

    private String extractSeverity(KnowledgeUnit unit) {
        String stmt = unit.statement() != null ? unit.statement().toUpperCase() : "";
        if (stmt.contains("CRITICAL") || stmt.contains("OFFBOARDING")) return "CRITICAL";
        if (stmt.contains("CROSS-PLATFORM ADMIN") || stmt.contains("SOD VIOLATION")) return "HIGH";
        if (stmt.contains("DORMANT") || stmt.contains("STALE")) return "MEDIUM";
        if (stmt.contains("PRIVILEGE CREEP")) return "MEDIUM";
        return "";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String sanitizeFilename(String id) {
        return id.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

}
