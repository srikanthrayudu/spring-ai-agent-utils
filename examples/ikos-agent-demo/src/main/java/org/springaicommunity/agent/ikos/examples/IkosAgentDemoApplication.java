package org.springaicommunity.agent.ikos.examples;

import java.util.List;
import java.util.Scanner;

import org.springaicommunity.agent.advisors.AutoMemoryToolsAdvisor;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;
import org.springaicommunity.agent.common.task.subagent.SubagentType;
import org.springaicommunity.agent.ikos.Ikos;
import org.springaicommunity.agent.ikos.autoconfigure.IkosAutoConfiguration;
import org.springaicommunity.agent.ikos.bridge.IkosEnvironmentProvider;
import org.springaicommunity.agent.ikos.bridge.IkosMemoryBridge;
import org.springaicommunity.agent.ikos.bridge.IkosSubagentDefinition;
import org.springaicommunity.agent.ikos.bridge.IkosSubagentExecutor;
import org.springaicommunity.agent.ikos.bridge.IkosSubagentResolver;
import org.springaicommunity.agent.ikos.connector.SimulatedDataSource;
import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.simulation.SimulatedDataGenerator;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.AutoMemoryTools;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.task.TaskOutputTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springaicommunity.agent.tools.task.repository.TaskRepository;
import org.springaicommunity.agent.utils.AgentEnvironment;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

/**
 * IKOS Agent Demo — Full-featured AI identity governance analyst.
 *
 * <p>Demonstrates deep integration with spring-ai-agent-utils:
 * <ul>
 *   <li>{@link AutoMemoryTools} — persistent agent memory bridged to IKOS knowledge store</li>
 *   <li>{@link AutoMemoryToolsAdvisor} — auto-injects memory context into every prompt</li>
 *   <li>{@link SkillsTool} — reusable governance workflows (risk-scan, investigate, compliance)</li>
 *   <li>{@link IkosEnvironmentProvider} — enriches system prompt with IKOS platform context</li>
 *   <li>Core tools: TodoWrite, Shell, Grep, FileSystem, AskUser</li>
 *   <li>Dual advisor composition: AutoMemoryToolsAdvisor + KnowledgeEvolutionAdvisor</li>
 * </ul>
 *
 * <pre>
 *   mvn spring-boot:run
 * </pre>
 */
@SpringBootApplication(exclude = IkosAutoConfiguration.class)
public class IkosAgentDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(IkosAgentDemoApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompt/MAIN_AGENT_SYSTEM_PROMPT_V2.md") Resource systemPrompt) {

        return args -> {
            String storagePath = System.getProperty("user.home") + "/.ikos-agent-demo";
            String memoriesPath = storagePath + "/memories";

            // ── Wire all IKOS components ─────────────────────────────────
            Ikos ikos = Ikos.builder()
                    .storagePath(storagePath)
                    .auditEnabled(true)
                    .dataSources(List.of(new SimulatedDataSource(100)))
                    .advisorMaxUnits(10)
                    .build();

            // ── Seed with simulated data ─────────────────────────────────
            System.out.println("\n  ⛨  Seeding IKOS knowledge store...");
            var data = new SimulatedDataGenerator().generate(100);
            List<UnifiedIdentity> identities = ikos.correlationEngine().correlate(data.accounts());
            List<KnowledgeUnit> risks = ikos.riskEngine().detectRisks(identities);

            // Deduplicate before storing
            var dedupResult = ikos.deduplicationEngine().deduplicate(risks);
            for (KnowledgeUnit risk : dedupResult.uniqueRisks()) {
                ikos.storage().saveKnowledgeUnit(risk);
                ikos.pipeline().createObservation(risk.id() + "-OBS", risk.statement(),
                        risk.context() != null ? risk.context().toString() : "", "RiskDetectionEngine");
            }
            System.out.println("  ✓ Seeded " + dedupResult.deduplicatedCount() + " unique risks from "
                    + identities.size() + " identities (" + dedupResult.duplicatesRemoved() + " duplicates removed)\n");

            // ══════════════════════════════════════════════════════════════
            // GAP 1: AutoMemoryTools — Bridge IKOS → Agent Memory
            // ══════════════════════════════════════════════════════════════
            System.out.println("  🧠 Bridging IKOS knowledge → Agent Memory...");
            var autoMemoryTools = AutoMemoryTools.builder()
                    .memoriesDir(memoriesPath)
                    .build();
            var memoryBridge = new IkosMemoryBridge(autoMemoryTools, ikos.storage());
            int synced = memoryBridge.syncAllRisksToMemory();
            System.out.println("  ✓ Synced " + synced + " risks to agent persistent memory\n");

            // ══════════════════════════════════════════════════════════════
            // GAP 6: Enriched Environment — IKOS context in system prompt
            // ══════════════════════════════════════════════════════════════
            var envProvider = new IkosEnvironmentProvider(ikos);

            // ══════════════════════════════════════════════════════════════
            // GAP 5: Skills — Reusable governance workflows
            // ══════════════════════════════════════════════════════════════
            System.out.println("  🎯 Loading IKOS governance skills...");
            ToolCallback skillsTool = SkillsTool.builder()
                    .addSkillsResource(new org.springframework.core.io.ClassPathResource("skills/"))
                    .build();
            System.out.println("  ✓ Skills loaded: identity-risk-scan, investigate-identity, compliance-report, threat-hunt, incident-response\n");

            // ── Core Agent Utils Tools ───────────────────────────────────
            var todoTool = TodoWriteTool.builder()
                    .todoEventHandler(todos -> {
                        System.out.println("\n📋 Tasks:");
                        for (var item : todos.todos()) {
                            String icon = switch (item.status()) {
                                case pending -> "○";
                                case in_progress -> "◑";
                                case completed -> "●";
                            };
                            System.out.println("  " + icon + " " + item.content());
                        }
                    })
                    .build();

            var shellTools = ShellTools.builder().build();
            var grepTool = GrepTool.builder().workingDirectory(storagePath).build();
            var fileSystemTools = FileSystemTools.builder().allowedDirectory(storagePath).build();
            var askUserTool = AskUserQuestionTool.builder()
                    .questionHandler(new CommandLineQuestionHandler())
                    .answersValidation(false)
                    .build();

            // ══════════════════════════════════════════════════════════════
            // GAP 3+4: TaskTool + SubagentExecutor — multi-agent security SOC
            //   3 specialized IKOS subagents with background task support
            // ══════════════════════════════════════════════════════════════
            System.out.println("  🤖 Registering IKOS security subagents...");

            // Create IKOS subagent infrastructure
            // Clone builder BEFORE main tools are added — avoids duplicate tool registration
            var subagentBuilder = chatClientBuilder.clone();
            var ikosResolver = new IkosSubagentResolver();
            var ikosExecutor = new IkosSubagentExecutor(subagentBuilder,
                    ikos.governanceTools(), ikos.engineeringTools());
            var ikosSubagentType = new SubagentType(ikosResolver, ikosExecutor);

            // TaskRepository for background task tracking
            TaskRepository taskRepository = new DefaultTaskRepository();

            // Build TaskTool with 3 IKOS subagent definitions
            ToolCallback taskTool = TaskTool.builder()
                    .subagentTypes(ikosSubagentType)
                    .subagentReferences(
                            new SubagentReference("classpath:/agents/RISK_ANALYST_SUBAGENT.md",
                                    IkosSubagentDefinition.KIND),
                            new SubagentReference("classpath:/agents/COMPLIANCE_REVIEWER_SUBAGENT.md",
                                    IkosSubagentDefinition.KIND),
                            new SubagentReference("classpath:/agents/REMEDIATION_PLANNER_SUBAGENT.md",
                                    IkosSubagentDefinition.KIND))
                    .taskRepository(taskRepository)
                    .build();

            // TaskOutput tool for retrieving background task results
            ToolCallback taskOutputTool = TaskOutputTool.builder()
                    .taskRepository(taskRepository)
                    .build();

            System.out.println("  ✓ Subagents: risk-analyst, compliance-reviewer, remediation-planner");
            System.out.println("  ✓ TaskRepository: background task tracking enabled\n");

            // ── Build ChatClient with FULL integration ──────────────────
            var autoMemoryAdvisor = AutoMemoryToolsAdvisor.builder()
                    .memoriesRootDirectory(memoriesPath)
                    .order(50)
                    .build();

            ChatClient chatClient = chatClientBuilder
                    .defaultSystem(p -> p.text(systemPrompt)
                            .param(AgentEnvironment.ENVIRONMENT_INFO_KEY, envProvider.enrichedInfo())
                            .param(AgentEnvironment.GIT_STATUS_KEY, AgentEnvironment.gitStatus())
                            .param(AgentEnvironment.AGENT_MODEL_KEY, "gemini")
                            .param(AgentEnvironment.AGENT_MODEL_KNOWLEDGE_CUTOFF_KEY, "2025-01-01"))
                    .defaultTools(
                            // IKOS domain tools
                            ikos.governanceTools(), ikos.engineeringTools(),
                            // Core agent tools
                            todoTool, shellTools, grepTool, fileSystemTools, askUserTool,
                            // Memory tools (Gap 1)
                            autoMemoryTools,
                            // Skills (Gap 5)
                            skillsTool,
                            // Subagents + Tasks (Gap 3+4)
                            taskTool, taskOutputTool)
                    .defaultAdvisors(
                            autoMemoryAdvisor,
                            ikos.advisor())
                    .build();

            // ── Interactive agent loop ───────────────────────────────────
            System.out.println("""
                ⛨  IKOS SOC Analyst — AI Identity Governance Agent
                ═══════════════════════════════════════════════════
                
                 Integration with spring-ai-agent-utils (13/13 capabilities):
                   ✅ AutoMemoryTools — persistent agent memory bridged to IKOS
                   ✅ AutoMemoryToolsAdvisor — auto-injects memory into prompts
                   ✅ KnowledgeEvolutionAdvisor — bidirectional IKOS context + observability
                   ✅ SkillsTool — 5 SOC skills loaded
                   ✅ TaskTool — 3 IKOS subagents (risk/compliance/remediation)
                   ✅ TaskRepository — background task tracking
                   ✅ TodoWriteTool — remediation task tracking
                   ✅ ShellTools + GrepTool + FileSystemTools — investigation
                   ✅ AskUserQuestionTool — governance decision escalation
                   ✅ AgentEnvironment — enriched with IKOS platform context
                   ✅ IkosMemoryBridge — IKOS↔agent memory synchronization
                   ✅ IkosSubagentExecutor — SOC-grade multi-agent orchestration
                   ⊘ BraveWebSearch — N/A (internal security operations)
                
                 SOC Tools (13 governance tools):
                   🔍 AnalyzeRisks, DetectOffboarding, ComplianceCheck, BlastRadius
                   🔗 QueryIdentityGraph, RecommendRemediation, ListRisks, ListKnowledge
                   🛡️ ContainIdentity, EscalateToSOC — SOC incident response
                   📝 RecordIncident, RecordRemediation, RecordAuditFinding
                
                 Subagents ("launch <name>"):
                   🔍 risk-analyst — Risk scanning + blast radius analysis
                   📋 compliance-reviewer — NIST/SOX/HIPAA/ISO/GDPR/CIS/MITRE mapping
                   🔧 remediation-planner — Containment + remediation roadmaps
                
                 Skills ("use skill <name>"):
                   • identity-risk-scan — Full scan pipeline
                   • investigate-identity — Deep-dive into a specific person
                   • compliance-report — Regulatory compliance mapping
                   • threat-hunt — Proactive threat hunting across identity infra
                   • incident-response — NIST SP 800-61 incident handling lifecycle
                
                 Try:
                   • "Analyze all critical risks and compute blast radius"
                   • "Use the threat-hunt skill to find hidden privilege abuse"
                   • "Contain svc-pipeline-8 with DISABLE action"
                   • "Escalate Sarah Jones — offboarding gap with admin access"
                   • "Launch compliance-reviewer to map all risks to NIST"
                   • "Use incident-response skill for David Lee"
                
                Type 'quit' to exit.
                """);

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("> ");
                    String input = scanner.nextLine().trim();
                    if ("quit".equalsIgnoreCase(input)) break;
                    if (input.isBlank()) continue;

                    // Log to audit trail
                    if (ikos.auditLogger() != null) {
                        ikos.auditLogger().log("agent-user",
                                org.springaicommunity.agent.ikos.audit.AuditLogger.AuditAction.TOOL_CALL,
                                "agent-query", java.util.Map.of("query", input));
                    }

                    try {
                        String response = chatClient.prompt(input)
                                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "ikos-agent"))
                                .call()
                                .content();

                        System.out.println("\n" + response + "\n");
                    } catch (Exception e) {
                        // Extract root cause for clean error display
                        Throwable cause = e;
                        while (cause.getCause() != null) cause = cause.getCause();
                        System.err.println("\n⚠ Agent error: " + cause.getMessage());
                        System.err.println("  (This is often a transient API issue — try again or rephrase)\n");
                    }
                }
            }
        };
    }

}
