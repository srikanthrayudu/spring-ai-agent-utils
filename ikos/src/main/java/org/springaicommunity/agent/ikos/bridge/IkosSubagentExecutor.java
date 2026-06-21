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
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;

/**
 * Executes IKOS subagent tasks using Spring AI ChatClient.
 *
 * <p>Each IKOS subagent gets:
 * <ul>
 *   <li>A specialized system prompt (from its markdown definition)</li>
 *   <li>Full IKOS tool access via the same tool objects as the parent agent</li>
 *   <li>Access to the knowledge store via tools</li>
 * </ul>
 *
 * @author Antigravity
 */
public class IkosSubagentExecutor implements SubagentExecutor {

    private final ChatClient.Builder chatClientBuilder;
    private final Object[] toolObjects;

    /**
     * @param chatClientBuilder the base ChatClient builder (with model configured)
     * @param toolObjects IKOS tool objects with @Tool annotated methods
     */
    public IkosSubagentExecutor(ChatClient.Builder chatClientBuilder, Object... toolObjects) {
        this.chatClientBuilder = chatClientBuilder;
        this.toolObjects = toolObjects;
    }

    @Override
    public String getKind() {
        return IkosSubagentDefinition.KIND;
    }

    @Override
    public String execute(TaskCall taskCall, SubagentDefinition subagent) {
        var ikosSubagent = (IkosSubagentDefinition) subagent;

        // Build a fresh ChatClient with the subagent's system prompt and all IKOS tools
        ChatClient subagentClient = chatClientBuilder.clone()
                .defaultTools(toolObjects)
                .defaultAdvisors(ToolCallAdvisor.builder().build())
                .build();

        return subagentClient.prompt()
                .system(ikosSubagent.getContent())
                .user(taskCall.prompt())
                .call()
                .content();
    }

}
