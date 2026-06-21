# ⛨ IKOS — Identity Knowledge Operating System

> **Hackathon Track:** Identity & Access Risk Governance  
> **Problem Statement:** Identity Sprawl & Privileged Access Abuse Detection in Hybrid Enterprises

[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring AI 2.0](https://img.shields.io/badge/Spring_AI-2.0.0-green.svg)](https://docs.spring.io/spring-ai/reference/)
[![Spring Boot 4.0](https://img.shields.io/badge/Spring_Boot-4.0.0-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange.svg)](../LICENSE.txt)

---

## 🎯 What is IKOS?

IKOS is an **autonomous identity security intelligence platform** that detects cross-platform identity sprawl, privilege abuse, and offboarding gaps across 5 enterprise platforms — then **learns from every detection** to get smarter over time.

Unlike static rule engines that fire-and-forget alerts, IKOS builds **organizational security memory** through a 4-stage Knowledge Evolution Pipeline. Every risk observation, remediation outcome, and governance decision compounds into a growing knowledge base that reduces false positives and improves detection confidence autonomously.

### The Problem We Solve

A global enterprise manages 5,000+ identities across Active Directory, Azure AD, AWS IAM, Okta, Salesforce, and ServiceNow. Each platform has its own privilege model, audit log format, and review cadence. **No single team owns the full picture.**

When a service account is granted Domain Admin in AD and simultaneously given S3 full-access in AWS, the risk is **invisible to both teams**.

IKOS makes these invisible risks visible — and actionable.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    IKOS SYSTEM ARCHITECTURE                        │
│                                                                     │
│  DATA INGESTION ──→ IDENTITY CORRELATION ──→ RISK DETECTION        │
│  (5 platforms)      (405→200 profiles)      (8 rules + behavioral) │
│                                                                     │
│  ALERT CONSOLIDATION ──→ KNOWLEDGE EVOLUTION ──→ SPRING AI LAYER   │
│  (148→26 = 82% reduction) (4-stage pipeline)   (8 @Tool methods)  │
│                                                                     │
│  OUTPUT: Dashboard (10 tabs) + Risk Report + CSV Export            │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
Identity Event (AD/AWS/Okta/SF/SN)
       ↓
  SimulatedDataGenerator (seed=42, deterministic)
       ↓
  IdentityCorrelationEngine → 200 Unified Identities
       ↓
  RiskDetectionEngine (8 risk rules)
       ↓
  BehavioralAnalyzer (6 anomaly detectors)
       ↓
  AlertConsolidationEngine (82% noise reduction)
       ↓
  KnowledgeEvolutionPipeline (4-stage promotion)
       ↓
  InteractiveDashboardGenerator → HTML Dashboard
```

---

## ✨ Key Features

### 1. Cross-Platform Identity Correlation
- Ingests identity data from **5 platforms**: Active Directory, AWS IAM, Okta, Salesforce, ServiceNow
- Correlates 405 accounts into **200 unified identity profiles** using name-matching heuristics
- Resolves **effective permissions through nested group hierarchy** (BFS traversal of 25 groups)

### 2. 14-Rule Risk Detection Engine
| Rule | Risk Type | MITRE ATT&CK |
|------|-----------|---------------|
| Offboarding Gap | AD disabled, AWS/Okta still active | T1078 (Valid Accounts) |
| Cross-Platform Admin | Admin on 2+ platforms simultaneously | T1078.004 |
| Dormant Admin | Admin inactive >90 days | T1078 |
| Privilege Creep | Accumulated permissions across platforms | T1098 (Account Manipulation) |
| Orphaned Service Account | No identifiable owner, admin privileges | T1136 (Create Account) |
| SoD Violation | Conflicting admin roles (AD + AWS + Okta) | T1098 |
| Token Misuse | External IP access pattern anomaly | T1550 (Use Alternate Auth Material) |
| Stale Exception | Temporary admin never revoked | T1531 (Account Access Removal) |
| + 6 Behavioral Rules | Privilege spikes, off-hours access, cross-platform cascades, etc. | — |

### 3. Knowledge Evolution Pipeline (Unique Differentiator)
No competitor has autonomous learning. IKOS builds organizational security memory:

```
OBSERVATION (30%) → PATTERN_CANDIDATE (50%) → VALIDATED_PATTERN (70%) → SECURITY_KNOWLEDGE (90%)
     ↑                                                                          ↓
     └──────────── Outcome Learning Feedback Loop ←────────────────────────────┘
```

**4-factor confidence model:** `Confidence = Evidence × Consistency × Validation × Outcomes`

### 4. Spring AI Agent Loop (Autonomous)
The LLM autonomously chains 7+ tool calls without human intervention:

```
Loop 1: ListIdentityRisks()       → Discovered risk landscape
Loop 2: AnalyzeIdentityRisks()    → Deep-dived highest risk
Loop 3: RecommendRemediation()    → Generated policy-mapped plan
Loop 4: TodoWrite()               → Structured task tracking
Loop 5: RecordRemediationAction() → Outcome feeds learning loop
Loop 6: TaskRepository (parallel) → 2 background investigations
Loop 7: ContextBuilder.build()    → Knowledge-augmented summary
```

### 5. Nested Group Hierarchy (Effective Privilege Resolution)
Resolves hidden admin through group inheritance chains:
```
User → Contractors → IT-Team → AD-Admins → Domain Admins (HIDDEN ADMIN!)
```
25 groups across 5 platforms with BFS cycle detection.

### 6. 82% Alert Noise Reduction
148 raw risk indicators consolidated into 26 actionable incidents — exceeding the ≥40% target.

---

## 📊 Interactive Dashboard

The dashboard has **10 tabs** covering every aspect of identity risk:

| Tab | Content |
|-----|---------|
| **Overview** | 8 KPI cards, risk distribution chart, platform breakdown |
| **Identity Graph** | Interactive vis.js network — 43 nodes, risk-colored, filterable |
| **Risks** | Full risk table with severity badges, MITRE tags, drill-down details |
| **Identities** | 200 unified profiles with platform count, admin status, hidden admin chains |
| **Offboarding** | 18 offboarding gaps with days-since-termination and active platforms |
| **Behavioral** | Anomaly analysis: privilege spikes, off-hours, cross-platform cascades |
| **Privilege Heatmap** | 5-platform admin access grid color-coded by risk level |
| **Alert Consolidation** | Before/after comparison, grouped incident view |
| **Compliance** | NIST SP 800-53 + MITRE ATT&CK + GDPR + CIS mapping, case study detection |
| **Risk Report** | Top 10 risky identities with platform-specific CLI remediation commands |

---

## 🔬 Problem Statement Case Study Coverage

All 4 mandatory incident cases from the problem statement are detected:

| Case | Scenario | IKOS Rule | Result |
|------|----------|-----------|--------|
| **Case 1** | Contractor offboarding gap | `OFFBOARDING_GAP` | ✅ 18 gaps found |
| **Case 2** | Hidden admin via nested groups | `GROUP_HIERARCHY + BFS` | ✅ 25 groups modeled |
| **Case 3** | Unrotated API token → SaaS breach | `TOKEN_MISUSE + CREDENTIAL_ROTATION` | ✅ 7 stale accounts |
| **Case 4** | Temporary admin never revoked | `STALE_EXCEPTION` | ✅ 10 exceptions found |

---

## 🚀 Quick Start

### Prerequisites
- **Java 25** (JDK for building, JRE for running)
- **Maven** (included via `mvnw` wrapper)
- **Docker** (optional, for containerized deployment)

### Option 1: Run Locally

```bash
# Clone and build
git clone https://github.com/srikanthrayudu/spring-ai-agent-utils.git
cd spring-ai-agent-utils

# Build the IKOS module
./mvnw clean compile -pl ikos -am -q

# Generate the dashboard (headless — no API key needed)
./mvnw exec:java -pl ikos \
  -Dexec.mainClass=org.springaicommunity.agent.ikos.QuickDashboard

# Open the dashboard
open ~/.ikos-demo/ikos-dashboard.html
```

### Option 2: Interactive CLI Demo

```bash
# Run the full 15-step interactive demo
./mvnw exec:java -pl ikos \
  -Dexec.mainClass=org.springaicommunity.agent.ikos.IkosDemo
```

The CLI demo walks through:
1. Simulated data generation (200 identities, 5 platforms)
2. Identity correlation
3. Risk detection (8 rules)
4. Nested group privilege analysis
5. Auto-pattern discovery
6. Explainable recommendations
7. Context assembly
8. Remediation & outcome learning
9. Compliance dashboard
10. Governance review & auto-promotion
11. Behavioral analysis (1000 audit events)
12. Offboarding gap detection
13. Stale exception detection
14. Alert consolidation (82% reduction)
15. Interactive HTML dashboard generation

### Option 3: Docker

```bash
# Build and run
docker-compose up --build

# Dashboard: http://localhost:8080
# Interactive CLI:
docker exec -it ikos-dashboard ./mvnw exec:java -pl ikos \
  -Dexec.mainClass=org.springaicommunity.agent.ikos.IkosDemo
```

---

## 🛠️ Technology Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java 25 |
| **Framework** | Spring AI 2.0.0 + Spring Boot 4.0.0 |
| **AI Integration** | Spring AI `@Tool` annotations, `ChatClient`, `BaseChatMemoryAdvisor` |
| **Agent Tools** | `spring-ai-agent-utils` — 15 tools (FileSystem, Shell, Grep, Todo, etc.) |
| **Visualization** | vis.js (network graph), Chart.js (charts), vanilla CSS (dark theme) |
| **Build** | Maven with multi-stage Docker (JDK build → JRE runtime) |
| **Data** | Deterministic simulated dataset (seed=42) matching problem statement ratios |

---

## 📁 Project Structure

```
ikos/
├── src/main/java/org/springaicommunity/agent/ikos/
│   ├── IkosDemo.java                    # Interactive CLI demo (15 steps)
│   ├── QuickDashboard.java              # Headless dashboard generator
│   ├── KnowledgeEvolutionPipeline.java  # 4-stage knowledge lifecycle
│   ├── ContextBuilder.java              # Relevance-scored context assembly
│   │
│   ├── model/                           # 17 domain records
│   │   ├── UnifiedIdentity.java         # Cross-platform identity profile
│   │   ├── KnowledgeUnit.java           # Core knowledge abstraction
│   │   ├── GroupHierarchy.java          # Nested group BFS traversal
│   │   ├── EffectivePrivilegeProfile.java # Resolved permissions
│   │   └── ...
│   │
│   ├── identity/                        # Correlation engine
│   ├── risk/                            # Detection, behavioral, consolidation
│   ├── tools/                           # Spring AI @Tool methods (8 tools)
│   ├── advisors/                        # KnowledgeEvolutionAdvisor
│   ├── simulation/                      # SimulatedDataGenerator (seed=42)
│   ├── report/                          # Dashboard HTML generator
│   ├── storage/                         # File-based persistent memory
│   ├── promotion/                       # Governance review engine
│   ├── outcome/                         # Outcome learning feedback
│   ├── validation/                      # Confidence calculator
│   └── synthesis/                       # Auto-pattern discovery
│
├── src/test/java/                       # 65 unit tests (all passing)
│
../docs/
├── ARCHITECTURE.md                      # System architecture & design
├── DATA_DICTIONARY.md                   # Full schema reference (9 models)
├── ENTERPRISE_EXPECTATIONS.md           # Enterprise buyer expectations
├── SPRING_AI_AGENT_UTILS_INTEGRATION.md # Focused agent integration notes
└── RISK_REPORT.md                       # CISO-grade executive risk report

../ikos-dashboard.html                   # Pre-built interactive dashboard
../DEMO_SCRIPT.md                        # 5-minute hackathon demo script
../Dockerfile                            # Multi-stage JDK->JRE build
```

**Codebase:** 76 source files, 10 test files, ~11,700 lines of Java

---

## 🔌 Spring AI Integration

IKOS is built **natively on Spring AI 2.0** — not a wrapper around external APIs:

### @Tool Methods (LLM-Callable)
```java
@Tool(name = "AnalyzeIdentityRisks",
      description = "Analyze a unified identity across all platforms...")
public String analyzeIdentityRisks(@ToolParam("Display name") String displayName) { ... }

@Tool(name = "DetectOffboardingGap", ...)
@Tool(name = "RecordSecurityIncident", ...)
@Tool(name = "RecordRemediationAction", ...)
@Tool(name = "RecordAuditFinding", ...)
@Tool(name = "RecommendRemediation", ...)
@Tool(name = "ListIdentityRisks", ...)
@Tool(name = "ListAllIkosKnowledge", ...)
```

### KnowledgeEvolutionAdvisor (ChatClient Advisor)
```java
public class KnowledgeEvolutionAdvisor implements BaseChatMemoryAdvisor {
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        // Retrieves relevant knowledge units and injects into system prompt
        String context = contextBuilder.buildContext(userQuery, maxRetrievedUnits);
        // Augments prompt with <engineering-memory> block
    }
}
```

### Agent Loop (Autonomous Multi-Step Reasoning)
```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(knowledgeEvolutionAdvisor)
    .defaultTools(identityGovernanceTools)
    .build();

// LLM autonomously chains: ListRisks → Analyze → Recommend → Execute
chatClient.prompt().user("Investigate the most critical identity risk").call();
```

---

## 📋 Compliance Mapping

| Framework | Controls Covered |
|-----------|-----------------|
| **NIST SP 800-53** | AC-2, AC-5, AC-6, AC-6(5), IA-4, IA-5(1) |
| **MITRE ATT&CK** | T1078, T1098, T1136, T1550, T1531 |
| **GDPR** | Article 5 (Data Minimisation), Article 32 (Security of Processing) |
| **CIS Controls** | Control 5 (Account Mgmt), Control 6 (Access Control Mgmt) |

---

## 🧪 Testing

```bash
# Run all IKOS tests
./mvnw test -pl ikos

# Result: 65 tests, 0 failures, 0 errors
```

Tests cover:
- Knowledge evolution lifecycle (observation → pattern → validated → knowledge)
- Identity correlation engine
- Risk detection engine (offboarding, admin, dormant, orphaned)
- Alert consolidation
- Risk deduplication
- File-based memory storage
- Audit logging
- Confidence calculation
- Context assembly

---

## 📄 Documentation

| Document | Purpose |
|----------|---------|
| [ARCHITECTURE.md](../docs/ARCHITECTURE.md) | System design, data flow, MITRE mapping |
| [DATA_DICTIONARY.md](../docs/DATA_DICTIONARY.md) | Schema reference for all 9 data models |
| [ENTERPRISE_EXPECTATIONS.md](../docs/ENTERPRISE_EXPECTATIONS.md) | Enterprise buyer expectations and prototype coverage |
| [SPRING_AI_AGENT_UTILS_INTEGRATION.md](../docs/SPRING_AI_AGENT_UTILS_INTEGRATION.md) | Focused Spring AI Agent Utils integration strategy |
| [RISK_REPORT.md](../docs/RISK_REPORT.md) | Executive risk report with top 10 identities |
| [DEMO_SCRIPT.md](../DEMO_SCRIPT.md) | 5-minute hackathon presentation guide |

---

## 🏆 Why IKOS Wins

| Differentiator | What It Means | Competitors |
|---------------|---------------|-------------|
| **Knowledge Evolution** | System learns from every detection & remediation | Static rules, no learning |
| **Spring AI Agent Loop** | LLM chains 7+ tool calls autonomously | Manual API calls |
| **5-Platform Simulation** | AD, AWS, Okta, Salesforce, ServiceNow | Problem asks for 3-4 |
| **Nested Group Hierarchy** | BFS-based effective privilege resolution | Flat permission checks |
| **82% Noise Reduction** | 148 alerts → 26 incidents | Problem asks for ≥40% |
| **10-Tab Dashboard** | Interactive graph, heatmap, drill-down | Basic tables |

---

## 📜 License

Apache License 2.0 — see [LICENSE.txt](../LICENSE.txt)

## 👨‍💻 Author

Built by **Srikanth Rayudu** for the Identity & Access Risk Governance Hackathon, powered by [spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils).
