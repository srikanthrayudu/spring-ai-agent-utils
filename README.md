# IKOS - Identity Knowledge Operating System

Hackathon track: Identity & Access Risk Governance  
Problem statement: Identity Sprawl & Privileged Access Abuse Detection in Hybrid Enterprises

IKOS is a working Java/Spring AI prototype that correlates identities across Active Directory, AWS IAM, Okta, Salesforce, and ServiceNow, computes effective privilege including inherited access, detects cross-platform risk, consolidates noisy findings into incidents, and generates explainable remediation guidance.

The project uses `spring-ai-agent-utils` as the agent foundation, but the deliverable is not Spring documentation. The deliverable is an enterprise identity-risk product prototype.

## What Enterprise Users Expect

An enterprise buyer would not only expect an alert list. They would expect:

| Expectation | IKOS answer |
| --- | --- |
| Unified identity view | Correlates 405 platform accounts into 200 unified identities |
| Effective privilege visibility | Resolves nested group and role inheritance before scoring risk |
| Cross-platform abuse detection | Finds risks that AD, AWS, Okta, and SaaS teams miss in isolation |
| Explainable decisions | Every finding includes evidence, severity, policy mapping, and remediation |
| Audit-ready output | Generates data dictionary, architecture docs, and a sample risk report |
| Noise reduction | Consolidates raw risk indicators into actionable incidents |
| Integration path | Exposes Spring AI tools, REST endpoints, data-source interfaces, and dashboard output |

## Why This Is Unique

Most hackathon submissions for this problem stop at deterministic rules and a dashboard. IKOS adds an identity knowledge layer:

- Cross-platform identity resolver for inconsistent usernames, emails, employee IDs, and service accounts.
- Effective privilege calculator that models hidden admin access from nested groups.
- Risk detection for offboarding gaps, cross-platform admin, dormant admin, service accounts, stale exceptions, token misuse, and separation-of-duty violations.
- Alert consolidation that reduces standalone alerts into identity-centered incidents.
- Knowledge Evolution Pipeline that promotes repeated observations into validated security knowledge using evidence, review, and outcome feedback.
- Spring AI agent tools that let an LLM investigate identities, recommend remediation, record incidents, and learn from remediation outcomes.

## Current Prototype Coverage

| Requirement from problem statement | Status |
| --- | --- |
| Simulated multi-platform identity data | Implemented |
| Cross-platform identity resolver | Implemented |
| Effective privilege calculator with nested groups | Implemented |
| Risk scoring engine with evidence | Implemented |
| Dashboard with risk list, graph, heatmap, gaps, incidents | Implemented |
| Architecture and AI/ML approach docs | Implemented |
| Sample risk report with remediation steps | Implemented |
| Spring AI Agent Utils integration | Implemented via tools, advisor, examples |

## Quick Start

Generate the static dashboard without any LLM API key:

```bash
./mvnw exec:java -pl ikos \
  -Dexec.mainClass=org.springaicommunity.agent.ikos.QuickDashboard
```

Run the IKOS tests:

```bash
./mvnw test -pl ikos
```

Run the dashboard container:

```bash
docker-compose up --build
```

Then open `http://localhost:8080`.

## Documentation Map

Start here for judging and project review:

| Document | Purpose |
| --- | --- |
| [Enterprise Expectations](docs/ENTERPRISE_EXPECTATIONS.md) | What an enterprise user would expect and how IKOS answers it |
| [Architecture](docs/ARCHITECTURE.md) | System design, data flow, AI/ML approach, and detection matrix |
| [Data Dictionary](docs/DATA_DICTIONARY.md) | Simulated telemetry schema and field definitions |
| [Spring AI Agent Utils Integration](docs/SPRING_AI_AGENT_UTILS_INTEGRATION.md) | How IKOS uses tools, skills, advisors, and subagents without becoming a Spring docs project |
| [Sample Risk Report](docs/RISK_REPORT.md) | CISO-style output with top risky identities and remediation steps |
| [Demo Script](DEMO_SCRIPT.md) | Five-minute hackathon presentation flow |
| [IKOS Module README](ikos/README.md) | Module-level implementation details |

The original `spring-ai-agent-utils` tool documentation is still present under `spring-ai-agent-utils/` because the code depends on it, but it is not the main project documentation.

## Architecture Snapshot

```text
Simulated identity snapshots + audit events
        |
        v
IdentityDataAggregator
        |
        v
DefaultIdentityCorrelationEngine
        |
        v
DefaultRiskDetectionEngine + BehavioralAnalyzer
        |
        v
AlertConsolidationEngine + RiskDeduplicationEngine
        |
        v
KnowledgeEvolutionPipeline
        |
        v
Spring AI tools + REST API + dashboard/report output
```

## Spring AI Agent Utils Leverage

IKOS uses the agent utilities where they add hackathon value:

- `@Tool` methods expose identity investigation and remediation actions to an AI agent.
- `KnowledgeEvolutionAdvisor` injects relevant learned identity-risk context into agent prompts.
- `TodoWriteTool` can turn remediation recommendations into tracked tasks.
- `TaskTool` and subagents support separate risk analyst, compliance reviewer, and remediation planner roles.
- `SkillsTool` packages reusable identity-investigation and compliance workflows.
- File, shell, grep, and glob tools support agent-driven investigation during demos.

This gives the project a credible agentic layer without drowning the submission in framework reference docs.

## Verified Locally

The IKOS module test suite passes:

```text
./mvnw test -pl ikos
Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
```

The full reactor compiles with:

```bash
./mvnw compile -pl ikos -am
```

Note: full reactor tests currently hit Mockito inline/Byte Buddy self-attach behavior in the upstream `spring-ai-agent-utils` tests on Java 25. IKOS tests are isolated and passing.

## Project Structure

```text
ikos/
  src/main/java/org/springaicommunity/agent/ikos/
    connector/       identity source abstraction and simulated source
    identity/        cross-platform correlation
    risk/            rules, behavior analysis, deduplication, consolidation
    model/           identities, events, evidence, policies, risks
    tools/           Spring AI @Tool integration
    advisors/        knowledge context injection
    report/          HTML dashboard and report generation
    web/             REST API
docs/
  ARCHITECTURE.md
  DATA_DICTIONARY.md
  ENTERPRISE_EXPECTATIONS.md
  SPRING_AI_AGENT_UTILS_INTEGRATION.md
  RISK_REPORT.md
DEMO_SCRIPT.md
ikos-dashboard.html
Dockerfile
docker-compose.yml
```

## License

Apache License 2.0. See [LICENSE.txt](LICENSE.txt).
