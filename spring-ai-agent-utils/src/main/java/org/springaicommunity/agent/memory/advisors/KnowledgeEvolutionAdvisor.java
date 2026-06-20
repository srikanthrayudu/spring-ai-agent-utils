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
package org.springaicommunity.agent.memory.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.Assert;
import org.springaicommunity.agent.memory.ContextBuilder;

/**
 * Spring AI ChatClient Advisor that intercepts conversation cycles, retrieves relevant memories from the
 * Engineering Knowledge Operating System (EKOS), and augments the chat request with structured engineering context.
 * 
 * @author Antigravity
 */
public class KnowledgeEvolutionAdvisor implements BaseChatMemoryAdvisor {

	private final ContextBuilder contextBuilder;
	private final int order;
	private final int maxRetrievedUnits;

	public KnowledgeEvolutionAdvisor(ContextBuilder contextBuilder, int order, int maxRetrievedUnits) {
		Assert.notNull(contextBuilder, "Context builder must not be null");
		this.contextBuilder = contextBuilder;
		this.order = order;
		this.maxRetrievedUnits = maxRetrievedUnits;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		String userQuery = "";

		// Extract user query to retrieve relevant memory entries
		Message lastMessage = chatClientRequest.prompt().getLastUserOrToolResponseMessage();
		if (lastMessage != null && lastMessage.getMessageType() == MessageType.USER) {
			userQuery = lastMessage.getText();
		}

		if (userQuery == null || userQuery.isBlank()) {
			return chatClientRequest;
		}

		// Query context builder
		String memoryContext = this.contextBuilder.buildContext(userQuery, this.maxRetrievedUnits);

		if (memoryContext.isBlank()) {
			return chatClientRequest;
		}

		// Augment system prompt
		String originalSystemText = chatClientRequest.prompt().getSystemMessage() != null
				? chatClientRequest.prompt().getSystemMessage().getText()
				: "";

		String augmentedSystemMessage = originalSystemText + System.lineSeparator() + System.lineSeparator()
				+ "You are operating inside an Engineering Knowledge Operating System. Use the following project experiences and engineering knowledge to guide your response:\n"
				+ "<engineering-memory>\n"
				+ memoryContext
				+ "\n</engineering-memory>";

		Prompt augmentedPrompt = chatClientRequest.prompt().augmentSystemMessage(augmentedSystemMessage);

		return chatClientRequest.mutate().prompt(augmentedPrompt).build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		return chatClientResponse;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private ContextBuilder contextBuilder;
		private int order = 100;
		private int maxRetrievedUnits = 10;

		private Builder() {}

		public Builder contextBuilder(ContextBuilder contextBuilder) {
			this.contextBuilder = contextBuilder;
			return this;
		}

		public Builder order(int order) {
			this.order = order;
			return this;
		}

		public Builder maxRetrievedUnits(int maxRetrievedUnits) {
			this.maxRetrievedUnits = maxRetrievedUnits;
			return this;
		}

		public KnowledgeEvolutionAdvisor build() {
			Assert.notNull(this.contextBuilder, "ContextBuilder must be set");
			return new KnowledgeEvolutionAdvisor(this.contextBuilder, this.order, this.maxRetrievedUnits);
		}
	}
}
