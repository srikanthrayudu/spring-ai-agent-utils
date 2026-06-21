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
import org.springaicommunity.agent.common.task.subagent.SubagentResolver;
import org.springaicommunity.agent.utils.MarkdownParser;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Resolves {@link SubagentReference}s with kind "IKOS" into
 * {@link IkosSubagentDefinition}s by loading and parsing markdown
 * files with YAML frontmatter.
 *
 * @author Antigravity
 */
public class IkosSubagentResolver implements SubagentResolver {

    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

    @Override
    public boolean canResolve(SubagentReference subagentRef) {
        return IkosSubagentDefinition.KIND.equals(subagentRef.kind());
    }

    @Override
    public SubagentDefinition resolve(SubagentReference subagentRef) {
        try {
            Resource resource = resourceLoader.getResource(subagentRef.uri());
            String rawContent = resource.getContentAsString(StandardCharsets.UTF_8);

            MarkdownParser parser = new MarkdownParser(rawContent);
            Map<String, Object> frontMatter = parser.getFrontMatter();

            return new IkosSubagentDefinition(subagentRef, frontMatter, parser.getContent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve IKOS subagent from: " + subagentRef.uri(), e);
        }
    }

}
