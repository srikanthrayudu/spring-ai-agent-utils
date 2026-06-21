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
package org.springaicommunity.agent.ikos.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.Assert;
import org.springaicommunity.agent.ikos.ContextBuilder;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring AI ChatClient Advisor that intercepts conversation cycles, retrieves relevant memories from the
 * Identity Knowledge Operating System (IKOS), and augments the chat request with structured security context.
 *
 * <p>Bidirectional advisor:
 * <ul>
 *   <li>{@code before()} — injects relevant knowledge into the prompt</li>
 *   <li>{@code after()} — tracks tool invocations, response patterns, and knowledge utilization</li>
 * </ul>
 *
 * @author Antigravity
 */
public class KnowledgeEvolutionAdvisor implements BaseChatMemoryAdvisor {

	private final ContextBuilder contextBuilder;
	private final int order;
	private final int maxRetrievedUnits;

	// ── Observability counters ────────────────────────────────────────────
	private final AtomicInteger queriesAugmented = new AtomicInteger(0);
	private final AtomicInteger totalInteractions = new AtomicInteger(0);
	private final AtomicLong totalContextChars = new AtomicLong(0);
	private final AtomicInteger riskAnalysisResponses = new AtomicInteger(0);
	private final AtomicInteger complianceResponses = new AtomicInteger(0);
	private final AtomicInteger remediationResponses = new AtomicInteger(0);

	public KnowledgeEvolutionAdvisor(ContextBuilder contextBuilder, int order, int maxRetrievedUnits) {
		Assert.notNull(contextBuilder, "Context builder must not be null");
		this.contextBuilder = contextBuilder;
		this.order = order;
		this.maxRetrievedUnits = maxRetrievedUnits;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		totalInteractions.incrementAndGet();
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

		queriesAugmented.incrementAndGet();
		totalContextChars.addAndGet(memoryContext.length());

		// Augment system prompt
		String originalSystemText = chatClientRequest.prompt().getSystemMessage() != null
				? chatClientRequest.prompt().getSystemMessage().getText()
				: "";

		String augmentedSystemMessage = originalSystemText + System.lineSeparator() + System.lineSeparator()
				+ "You are operating inside the Identity Knowledge Operating System (IKOS). "
				+ "Use the following organizational security memory to guide your response. "
				+ "Reference specific knowledge IDs when making recommendations.\n"
				+ "<ikos-security-memory>\n"
				+ memoryContext
				+ "\n</ikos-security-memory>";

		Prompt augmentedPrompt = chatClientRequest.prompt().augmentSystemMessage(augmentedSystemMessage);

		return chatClientRequest.mutate().prompt(augmentedPrompt).build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		// Track response patterns for observability
		if (chatClientResponse != null && chatClientResponse.chatResponse() != null) {
			try {
				String content = chatClientResponse.chatResponse().getResult() != null
						? chatClientResponse.chatResponse().getResult().getOutput().getText()
						: "";
				if (content != null) {
					String upper = content.toUpperCase();
					if (upper.contains("RISK") || upper.contains("THREAT") || upper.contains("VULNERABILITY")) {
						riskAnalysisResponses.incrementAndGet();
					}
					if (upper.contains("NIST") || upper.contains("COMPLIANCE") || upper.contains("GDPR") || upper.contains("MITRE")) {
						complianceResponses.incrementAndGet();
					}
					if (upper.contains("REMEDIAT") || upper.contains("REVOKE") || upper.contains("DISABLE")) {
						remediationResponses.incrementAndGet();
					}
				}
			} catch (Exception e) {
				// Non-critical — don't break the advisor chain for observability
			}
		}
		return chatClientResponse;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	// ── Observability API ─────────────────────────────────────────────────

	/** Total queries processed by this advisor. */
	public int totalInteractions() { return totalInteractions.get(); }

	/** Queries where IKOS memory was injected into the prompt. */
	public int queriesAugmented() { return queriesAugmented.get(); }

	/** Total characters of security context injected. */
	public long totalContextChars() { return totalContextChars.get(); }

	/** Responses that contained risk analysis content. */
	public int riskAnalysisResponses() { return riskAnalysisResponses.get(); }

	/** Responses that referenced compliance frameworks. */
	public int complianceResponses() { return complianceResponses.get(); }

	/** Responses that contained remediation guidance. */
	public int remediationResponses() { return remediationResponses.get(); }

	/** Summary of advisor utilization metrics. */
	public String utilizationSummary() {
		return String.format(
				"KnowledgeEvolutionAdvisor: %d interactions, %d augmented (%.0f%%), " +
				"%d risk / %d compliance / %d remediation responses",
				totalInteractions.get(), queriesAugmented.get(),
				totalInteractions.get() > 0 ? queriesAugmented.get() * 100.0 / totalInteractions.get() : 0,
				riskAnalysisResponses.get(), complianceResponses.get(), remediationResponses.get());
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
