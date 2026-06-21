package org.springaicommunity.agent.ikos.examples;

import java.util.Scanner;

import org.springaicommunity.agent.ikos.Ikos;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.utils.AgentEnvironment;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

/**
 * IKOS Lifecycle Demo — Spring Boot application demonstrating the full
 * identity governance intelligence lifecycle with an LLM agent.
 *
 * <p>Uses {@link Ikos#builder()} to wire all components in a single call,
 * then attaches governance tools and the knowledge advisor to a ChatClient.
 *
 * <pre>
 *   mvn spring-boot:run
 * </pre>
 */
@SpringBootApplication
public class IkosLifecycleDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(IkosLifecycleDemoApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompt/MAIN_AGENT_SYSTEM_PROMPT_V2.md") Resource systemPrompt,
            @Value("${ikos.storage.path:#{systemProperties['user.home'] + '/.ikos-demo'}}") String storagePath) {

        return args -> {
            // ── Wire all IKOS components in one call ──────────────────────
            Ikos ikos = Ikos.builder()
                    .storagePath(storagePath)
                    .advisorMaxUnits(10)
                    .build();

            // ── TodoWriteTool for remediation tracking ────────────────────
            var todoTool = TodoWriteTool.builder()
                    .todoEventHandler(todos -> {
                        System.out.println("\n📋 Remediation Tasks:");
                        for (var item : todos.todos()) {
                            String icon = switch (item.status()) {
                                case pending -> "○";
                                case in_progress -> "◑";
                                case completed -> "●";
                            };
                            System.out.println("  " + icon + " [" + item.status() + "] " + item.content());
                        }
                    })
                    .build();

            // ── Build ChatClient with IKOS integration ───────────────────
            ChatClient chatClient = chatClientBuilder
                    .defaultSystem(p -> p.text(systemPrompt)
                            .param(AgentEnvironment.ENVIRONMENT_INFO_KEY, AgentEnvironment.info())
                            .param(AgentEnvironment.GIT_STATUS_KEY, AgentEnvironment.gitStatus())
                            .param(AgentEnvironment.AGENT_MODEL_KEY, "gemini")
                            .param(AgentEnvironment.AGENT_MODEL_KNOWLEDGE_CUTOFF_KEY, "2025-01-01"))
                    .defaultTools(ikos.governanceTools(), todoTool)
                    .defaultAdvisors(
                            ikos.advisor(),
                            MessageChatMemoryAdvisor.builder(
                                    MessageWindowChatMemory.builder().maxMessages(100).build()
                            ).build())
                    .build();

            // ── Interactive chat loop ────────────────────────────────────
            System.out.println("""
                
                ⛨  IKOS — Identity Knowledge Operating System
                ─────────────────────────────────────────────
                Your AI identity governance analyst is ready.
                
                Try:
                  • "Detect offboarding gaps for John Smith disabled on AD but active on AWS"
                  • "List all identity risks"
                  • "Recommend remediation for the top risk"
                  • "Record a security incident: unauthorized API key usage on AWS"
                
                Type 'quit' to exit.
                """);

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\n> ");
                    String input = scanner.nextLine().trim();
                    if ("quit".equalsIgnoreCase(input)) break;
                    if (input.isBlank()) continue;

                    String response = chatClient.prompt(input)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "ikos-session"))
                            .call()
                            .content();

                    System.out.println("\n" + response);
                }
            }
        };
    }

}
