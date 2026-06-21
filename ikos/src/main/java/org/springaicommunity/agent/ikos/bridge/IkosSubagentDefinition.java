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

import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;

import java.util.Map;

/**
 * IKOS-specific subagent definition parsed from markdown with YAML frontmatter.
 *
 * <p>Each IKOS subagent represents a specialized security role:
 * <ul>
 *   <li>{@code risk-analyst} — scans identities, detects risks</li>
 *   <li>{@code compliance-reviewer} — maps risks to regulatory controls</li>
 *   <li>{@code remediation-planner} — generates remediation plans</li>
 * </ul>
 *
 * @author Antigravity
 */
public class IkosSubagentDefinition implements SubagentDefinition {

    public static final String KIND = "IKOS";

    private final Map<String, Object> frontMatter;
    private final String content;
    private final SubagentReference reference;

    public IkosSubagentDefinition(SubagentReference reference,
                                   Map<String, Object> frontMatter,
                                   String content) {
        this.reference = reference;
        this.frontMatter = frontMatter;
        this.content = content;
    }

    @Override
    public String getName() {
        return frontMatter.get("name").toString();
    }

    @Override
    public String getDescription() {
        return frontMatter.get("description").toString();
    }

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public SubagentReference getReference() {
        return reference;
    }

    /**
     * Returns the IKOS tools this subagent is allowed to use.
     */
    public String getAllowedTools() {
        Object tools = frontMatter.get("tools");
        return tools != null ? tools.toString() : "";
    }

    /**
     * Returns the full system prompt content parsed from the markdown body.
     */
    public String getContent() {
        return content;
    }

    public Map<String, Object> getFrontMatter() {
        return frontMatter;
    }

    @Override
    public String toSubagentRegistrations() {
        return "- %s: %s (IKOS Security Agent — tools: %s)".formatted(
                getName(), getDescription(), getAllowedTools());
    }

}
