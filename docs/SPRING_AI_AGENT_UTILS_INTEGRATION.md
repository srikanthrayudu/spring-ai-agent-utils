# Spring AI Agent Utils Integration — SOC-Grade

IKOS uses `spring-ai-agent-utils` as an agent execution layer. The project should not be presented as a Spring AI Agent Utils documentation site. Present it as an identity-risk product that uses agent utilities to make investigation, remediation planning, and governance memory interactive.

## Integration Goals

| Goal | IKOS implementation |
| --- | --- |
| Let an LLM investigate identity risk | `IdentityGovernanceTools` exposes 13 identity-risk functions as Spring AI `@Tool` methods |
| Execute SOC containment & escalation | `ContainIdentity` + `EscalateToSOC` tools enable real-time incident response |
| Inject learned risk context | `KnowledgeEvolutionAdvisor` (bidirectional) retrieves knowledge + tracks response patterns |
| Turn recommendations into work | `TodoWriteTool` structures multi-step remediation tasks |
| Use specialist agent roles | `TaskTool` supports risk analyst, compliance reviewer, and remediation planner subagents |
| Reuse domain workflows | `SkillsTool` loads 5 SOC skills (risk-scan, investigate, compliance, threat-hunt, incident-response) |
| Support hands-on investigation | Shell, grep, glob, and filesystem tools let the agent inspect project output during demos |

## Core IKOS Tool Layer — 13 Governance Tools

Main class:

```text
ikos/src/main/java/org/springaicommunity/agent/ikos/tools/IdentityGovernanceTools.java
```

### Identity Investigation

| Tool | Purpose |
| --- | --- |
| `AnalyzeIdentityRisks` | Finds stored risk observations for a named identity |
| `DetectOffboardingGap` | Records a disabled-on-one-platform, active-on-another risk |
| `ListIdentityRisks` | Returns active risk observations and incidents |
| `ListAllIkosKnowledge` | Returns the knowledge base for audit/review |

### Cross-Platform Analysis

| Tool | Purpose |
| --- | --- |
| `ComplianceCheck` | Maps detected risks to NIST SP 800-53, GDPR, CIS Controls, MITRE ATT&CK |
| `ComputeBlastRadius` | Computes lateral movement blast radius for a compromised identity |
| `QueryIdentityGraph` | Explores cross-platform identity-to-knowledge-unit graph relationships |

### SOC Operations (Incident Response)

| Tool | Purpose |
| --- | --- |
| `ContainIdentity` | Execute containment actions: DISABLE, REVOKE_SESSIONS, ISOLATE, MONITOR. Records all actions for audit trail. |
| `EscalateToSOC` | Packages risk profile, blast radius, compliance impact, and recommended actions into a SOC Tier-3 escalation report. |

### Knowledge Management

| Tool | Purpose |
| --- | --- |
| `RecordSecurityIncident` | Saves an incident into the IKOS knowledge store |
| `RecordRemediationAction` | Stores remediation outcome so future confidence can improve |
| `RecordAuditFinding` | Records audit findings with control references |
| `RecommendRemediation` | Generates evidence-backed remediation guidance |

## Advisor Layer

Main class:

```text
ikos/src/main/java/org/springaicommunity/agent/ikos/advisors/KnowledgeEvolutionAdvisor.java
```

The advisor makes the agent context-aware with a **bidirectional** design:

**`before()` — Context Injection:**
1. Reads the current user question.
2. Retrieves relevant knowledge units from IKOS memory.
3. Scores them by relevance, confidence, evidence strength, and recency.
4. Injects the selected context into the prompt as `<ikos-security-memory>` block.

**`after()` — Observability & Feedback:**
1. Tracks response patterns (risk analysis, compliance references, remediation guidance).
2. Increments atomic counters for observability.
3. Exposes `utilizationSummary()` API for dashboard metrics.

This is the main uniqueness story: the agent does not start from an empty prompt every time. It uses accumulated identity-governance memory and tracks how that memory influences responses.

## SOC Agent System Prompt

```text
examples/ikos-agent-demo/src/main/resources/prompt/MAIN_AGENT_SYSTEM_PROMPT_V2.md
```

The system prompt defines the agent's SOC Analyst persona with:
- Investigation protocol (10-step SOC playbook: Triage → Investigate → Correlate → Assess → Contain → Remediate → Document → Verify → Report → Learn)
- All 13 governance tools documented inline
- Subagent orchestration instructions
- Response formatting with severity badges and compliance references

## Recommended Agent Composition

For the hackathon demo, keep the agent lean:

```java
ChatClient chatClient = chatClientBuilder
    .defaultAdvisors(ikos.knowledgeEvolutionAdvisor())
    .defaultTools(
        ikos.governanceTools(),
        TodoWriteTool.builder().build(),
        SkillsTool.builder().addSkillsResources(skillPaths).build()
    )
    .build();
```

For the full SOC agent, add investigation and orchestration utilities:

```java
.defaultTools(
    ikos.governanceTools(),          // 13 identity governance + SOC IR tools
    ikos.engineeringMemoryTools(),   // knowledge management
    autoMemoryTools,                 // persistent agent memory
    skillsTool,                      // 5 SOC skills
    taskTool, taskOutputTool,        // subagent orchestration
    todoTool,                        // remediation task tracking
    shellTools, grepTool,            // investigation
    fileSystemTools,                 // file access
    askUserTool                      // human-in-the-loop escalation
)
.defaultAdvisors(
    autoMemoryAdvisor,               // auto-inject memory context
    ikos.advisor()                   // bidirectional knowledge evolution
)
```

Lead with the IKOS domain tools (especially `ContainIdentity`, `EscalateToSOC`, `ComputeBlastRadius`), then mention the utility tools as accelerators.

## Subagent Roles

IKOS includes resources for specialist roles:

```text
examples/ikos-agent-demo/src/main/resources/agents/
  RISK_ANALYST_SUBAGENT.md
  COMPLIANCE_REVIEWER_SUBAGENT.md
  REMEDIATION_PLANNER_SUBAGENT.md
```

Recommended responsibilities:

| Subagent | Responsibility | Key Tools |
| --- | --- | --- |
| Risk analyst | Scan identities, detect risks, compute blast radius, generate MITRE ATT&CK mappings | AnalyzeRisks, BlastRadius, IdentityGraph, ComplianceCheck |
| Compliance reviewer | Map findings to NIST, GDPR, CIS, SOX, HIPAA, ISO 27001, and MITRE ATT&CK | ComplianceCheck, QueryIdentityGraph, RecordAuditFinding |
| Remediation planner | Execute containment, generate remediation roadmaps, track outcomes, escalate critical risks | ContainIdentity, EscalateToSOC, RecommendRemediation, BlastRadius |

## Skills — SOC Playbooks

IKOS packages 5 reusable SOC procedures:

```text
examples/ikos-agent-demo/src/main/resources/skills/
  identity-risk-scan/     — Full identity scan pipeline
  investigate-identity/   — Deep-dive into a specific person
  compliance-report/      — Multi-framework compliance mapping
  threat-hunt/            — Proactive threat hunting for hidden privilege abuse
  incident-response/      — NIST SP 800-61 incident handling lifecycle
```

| Skill | SOC Use Case |
| --- | --- |
| `identity-risk-scan` | Daily automated risk assessment across all platforms |
| `investigate-identity` | Tier-2 deep-dive investigation of a suspicious identity |
| `compliance-report` | Audit preparation and regulatory compliance gap analysis |
| `threat-hunt` | Proactive hunting for hidden lateral movement paths and IoCs |
| `incident-response` | Structured IR workflow: triage → contain → investigate → remediate → verify |

## Bridge Layer

| Component | Purpose |
| --- | --- |
| `IkosMemoryBridge` | Syncs IKOS knowledge store ↔ AutoMemoryTools persistent memory |
| `IkosEnvironmentProvider` | Enriches system prompt with IKOS platform context (13 tools, live stats) |
| `IkosSubagentDefinition` | Defines IKOS-specific subagent metadata (kind, tools, role) |
| `IkosSubagentResolver` | Resolves markdown+YAML subagent definitions from classpath |
| `IkosSubagentExecutor` | Executes subagent tasks using cloned ChatClient with IKOS tools |

## REST And Spring Boot Integration

Main classes:

```text
ikos/src/main/java/org/springaicommunity/agent/ikos/autoconfigure/IkosAutoConfiguration.java
ikos/src/main/java/org/springaicommunity/agent/ikos/web/IkosRestController.java
ikos/src/main/java/org/springaicommunity/agent/ikos/config/IkosProperties.java
```

The module can be added to a Spring Boot app and auto-configured with:

```yaml
ikos:
  storage-path: /var/ikos
  policies:
    dormant-admin-threshold-days: 90
    cross-platform-admin-min-platforms: 3
    stale-service-account-days: 180
  api:
    enabled: true
    base-path: /api/ikos
```

Useful REST endpoints:

| Endpoint | Purpose |
| --- | --- |
| `GET /api/ikos/health` | Component and storage health |
| `GET /api/ikos/risks` | Risk observations and incidents |
| `GET /api/ikos/knowledge` | Knowledge base review |
| `POST /api/ikos/incidents` | Record a security incident |
| `POST /api/ikos/scan` | Trigger a simulated scan |
| `GET /api/ikos/audit` | View API and tool audit trail |

## What To Avoid In The Submission

Avoid putting long Spring AI Agent Utils reference material in the main README or homepage. It makes the project look like a library demo instead of a cybersecurity product.

Use this structure instead:

1. Main README: IKOS problem, solution, uniqueness, run commands.
2. Architecture: data flow, engines, AI/ML approach.
3. Integration doc: only the Spring AI Agent Utils pieces IKOS actually uses.
4. Original library docs: keep them under `spring-ai-agent-utils/` as implementation reference only.
