# 🛡️ IKOS — Identity Knowledge Operating System

> **Continuously learning security intelligence built on Spring AI Agent Utils**

[![Java 25](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org)
[![Spring AI 2.0](https://img.shields.io/badge/Spring%20AI-2.0.0-green)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

---

## 🎯 The Problem

Modern enterprises operate across **5+ identity platforms** — Active Directory, AWS IAM, Okta, Salesforce, ServiceNow — each with different user identifiers, role models, audit formats, and review processes.

The result:

| Problem | Impact |
|---------|--------|
| **Privilege creep** grows unnoticed | Contractors accumulate 30+ permissions across platforms |
| **Offboarding gaps** persist for weeks | Disabled in AD, still admin on AWS with active API keys |
| **Dormant admin accounts** stay active | 120+ days inactive with Schema Admin privileges |
| **No organizational memory** | The same risk is investigated repeatedly, no learning |
| **Separation of duties violations** | Same person is admin on both identity provider AND cloud |

**Traditional IAM systems manage access. Traditional AI assistants retrieve documents.**

**IKOS creates organizational security memory.**

---

## 💡 The Solution

IKOS is a **cybersecurity knowledge operating system** that:

1. **Ingests** identity events from multiple platforms
2. **Correlates** cross-platform identities into unified profiles
3. **Detects** security risks using 8 rule-based engines
4. **Evolves knowledge** through a 5-stage confidence pipeline
5. **Recommends** explainable remediation with policy references
6. **Learns** from outcomes to improve future decisions

### The Moat: Knowledge Evolution

Unlike static rule engines, IKOS **learns from every interaction**:

```
Observation → Pattern Discovery → Governance Review → Security Knowledge → Outcome Learning
     ↑                                                                           ↓
     └──────────────────── Feedback Loop ──────────────────────────────────────────┘
```

Every risk detected, every remediation outcome, every audit finding feeds back into the system, increasing confidence and discovering patterns automatically.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        IKOS Architecture                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌──────────────────┐    ┌───────────────────┐  │
│  │  Identity    │    │  Risk Detection  │    │  Knowledge        │  │
│  │  Correlation │───▶│  Engine          │───▶│  Evolution        │  │
│  │  Engine      │    │  (8 rules)       │    │  Pipeline         │  │
│  └──────┬──────┘    └──────────────────┘    └────────┬──────────┘  │
│         │                                            │              │
│  ┌──────▼──────┐    ┌──────────────────┐    ┌────────▼──────────┐  │
│  │  Unified    │    │  Confidence      │    │  Pattern          │  │
│  │  Identity   │    │  Calculator      │    │  Discovery        │  │
│  │  Profile    │    │  E×C×V×O         │    │  Engine           │  │
│  └─────────────┘    └──────────────────┘    └───────────────────┘  │
│                                                                     │
│  ┌─────────────┐    ┌──────────────────┐    ┌───────────────────┐  │
│  │  Governance │    │  Context         │    │  Outcome          │  │
│  │  Memory     │◀──▶│  Assembler       │◀──▶│  Learning         │  │
│  │  (Persist)  │    │  (5-factor)      │    │  Engine           │  │
│  └─────────────┘    └──────────────────┘    └───────────────────┘  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              Spring AI Integration Layer                      │  │
│  │  ChatClient ◀──▶ KnowledgeEvolutionAdvisor ◀──▶ @Tool APIs   │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔬 Core Engines

### 1. Identity Correlation Engine
Resolves accounts across platforms into **Unified Identities** using email matching, employee ID, and display name fuzzy matching.

```
john.smith (AD) + jsmith (AWS) + john.smith@acme.com (Okta)
    → UnifiedIdentity: John Smith (UID-0001) — 3 platforms
```

### 2. Risk Detection Engine — 8 Rules

| # | Rule | Severity | Detection Logic |
|---|------|----------|-----------------|
| 1 | **Offboarding Gap** | 🔴 CRITICAL | Disabled on platform A, active on platform B |
| 2 | **Cross-Platform Admin** | 🟠 HIGH | Admin on 3+ platforms simultaneously |
| 3 | **Dormant Admin** | 🟠 HIGH | Admin account inactive >90 days |
| 4 | **Orphaned Account** | 🟡 MEDIUM | Active account with no identifiable owner |
| 5 | **Privilege Creep** | 🟡 MEDIUM | 30+ permissions including sensitive ones |
| 6 | **Contractor Excessive Access** | 🟠 HIGH | Non-employee with admin-level permissions |
| 7 | **Stale Service Account** | 🔴 CRITICAL | Service account with admin, unused >180 days |
| 8 | **SoD Violation** | 🔴 CRITICAL | Admin on both identity provider AND cloud infra |

### 3. Knowledge Evolution Pipeline

```
┌────────────┐   ┌──────────────────┐   ┌──────────────────┐   ┌──────────────┐
│ OBSERVATION│──▶│ PATTERN_CANDIDATE│──▶│ VALIDATED_PATTERN│──▶│  KNOWLEDGE   │
│  Conf: 0.3 │   │   Conf: 0.5      │   │   Conf: 0.7      │   │  Conf: 0.9   │
└────────────┘   └──────────────────┘   └──────────────────┘   └──────────────┘
                  Auto-Discovery         Governance Review      Human-Approved
```

### 4. Confidence Model

```
Confidence = Evidence × Consistency × Validation × Outcome
```

| Factor | Range | What it measures |
|--------|-------|-----------------|
| **Evidence** | 0.3–1.0 | Number of supporting evidence items (encodes **frequency**) |
| **Consistency** | 0.0–1.0 | Absence of contradictions with other knowledge |
| **Validation** | 0.5–1.0 | Review quality: human (1.0), auto (0.7), none (0.5) |
| **Outcome** | 0.0–1.0 | Success rate of applied remediations |

### 5. Context Scorer (5-Factor)

```
ContextScore = Relevance × Confidence × Similarity × EvidenceStrength × Recency
```

Uses exponential time decay (λ=0.01, ~70-day half-life) for recency scoring.

---

## 🔌 Spring AI Integration

IKOS integrates with Spring AI through three mechanisms:

### `@Tool` Annotated Methods — `IdentityGovernanceTools`

8 tools exposed to the LLM agent:

```java
@Tool("AnalyzeIdentityRisks")      // Analyze risks for a specific identity
@Tool("DetectOffboardingGap")       // Detect cross-platform offboarding gaps
@Tool("RecordSecurityIncident")     // Record security incidents
@Tool("RecordRemediationAction")    // Record remediation actions
@Tool("RecordAuditFinding")         // Record audit findings
@Tool("RecommendRemediation")       // Generate policy-mapped recommendations
@Tool("ListIdentityRisks")         // List all active risks
@Tool("ListAllIkosKnowledge")      // Query the full knowledge store
```

### `KnowledgeEvolutionAdvisor` — Spring AI ChatClient Advisor

Intercepts every LLM conversation to:
- **Before**: Inject relevant IKOS knowledge into the prompt context
- **After**: Extract observations from the response for learning

### `AutoMemoryToolsAdvisor` — Autonomous Memory Persistence

Manages persistent memory files (MEMORY.md pattern) automatically:
- Injects MemoryView, MemoryCreate, MemoryStrReplace, MemoryInsert, MemoryDelete tools
- Triggers memory consolidation when needed

### `AskUserQuestionTool` + `CommandLineQuestionHandler`

Structured agent-to-human clarification for governance decisions:
- Renders structured questions with 2-4 options
- Used in governance review (approve/reject/escalate)

### `TodoWriteTool` — Remediation Task Tracking

Structured task management for multi-step remediation:
- 3 states: `pending` → `in_progress` → `completed`
- Only 1 task can be `in_progress` at a time (enforced)

### `GovernanceMemory` + `FileMemoryStorage` — Persistent Knowledge

All knowledge units are persisted to disk with Jackson 3 serialization, organized by type:
- `project/` — Operational observations, risks, incidents
- `system/` — Promoted security knowledge (organizational memory)

---

## 🔄 Autonomous Agent Loop

IKOS demonstrates the full Spring AI agent loop pattern where the LLM autonomously chains tool calls:

```
User Goal: "Investigate and remediate the most critical identity risk"
                    │
    ┌───────────────▼───────────────┐
    │   Loop 1: ListIdentityRisks() │ ← Reconnaissance
    └───────────────┬───────────────┘
                    │ "Found 8 risks. Highest is RISK-0003."
    ┌───────────────▼───────────────┐
    │   Loop 2: AnalyzeIdentityRisks│ ← Deep Analysis
    └───────────────┬───────────────┘
                    │ "Sarah Jones has cross-platform admin."
    ┌───────────────▼───────────────┐
    │   Loop 3: RecommendRemediation│ ← Planning
    └───────────────┬───────────────┘
                    │ "Disable account, revoke tokens, MFA."
    ┌───────────────▼───────────────┐
    │   Loop 4: TodoWrite()         │ ← Task Tracking
    └───────────────┬───────────────┘
                    │ "3 remediation tasks created."
    ┌───────────────▼───────────────┐
    │   Loop 5: RecordRemediation() │ ← Execute & Learn
    └───────────────┘
    5 autonomous iterations, 6 tool calls, 0 human prompts
```

**Spring AI ChatClient wiring:**
```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        KnowledgeEvolutionAdvisor.builder()
            .contextBuilder(contextBuilder)
            .maxRetrievedUnits(10)
            .build(),
        AutoMemoryToolsAdvisor.builder()
            .memoriesRootDirectory(storageRoot + "/memories")
            .build())
    .defaultTools(governanceTools, engineeringTools)
    .build();

// Single call — agent loop handles everything
chatClient.prompt()
    .user("Investigate and fix the most critical identity risk")
    .call()
    .content();
```

---

## 🚀 Quick Start

### Prerequisites
- Java 25
- Maven 3.9+

### Run the Demo

```bash
# Clone and build
cd spring-ai-agent-utils

# Compile (first time)
./mvnw clean compile -pl spring-ai-agent-utils -am

# Run IKOS
./mvnw exec:java -Dexec.mainClass="org.springaicommunity.agent.memory.IkosDemo" -pl spring-ai-agent-utils
```

### Demo Menu

```
━━━ IKOS — Identity Knowledge Operating System ━━━━━━━
  1  Run full IKOS lifecycle demo (automated)        ← Start here!
  2  Detect identity risks (offboarding gap / dormant admin)
  3  Record a security incident
  4  Record a remediation action
  5  View auto-discovered pattern candidates
  6  Nominate + approve a promotion (governance review)
  7  Assemble context for identity governance query
  8  Record an outcome and learn
  9  List all IKOS knowledge units
  0  Compliance Dashboard (risk heatmap + scores)
  a  Audit Trail (knowledge evolution timeline)
  i  AI Agent Query (Spring AI tool-calling demo)    ← Agent loop!
  s  Spring AI Integration (framework showcase)      ← 15 components!
  q  Quit
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Recommended Demo Flow

1. **Press `1`** — Full 10-step automated lifecycle (ingestion → detection → remediation → dashboard)
2. **Press `i`** → type `demo` — Watch the 5-iteration autonomous agent loop
3. **Press `s`** — Spring AI framework showcase (15 components, 25+ tools)
4. **Press `0`** — View the compliance dashboard with risk heatmap
5. **Press `a`** — View the knowledge evolution audit trail

---

## 📁 Project Structure

```
spring-ai-agent-utils/src/main/java/org/springaicommunity/agent/
├── memory/
│   ├── IkosDemo.java                    # Interactive CLI demo (12 options)
│   ├── KnowledgeEvolutionPipeline.java  # Core evolution pipeline
│   ├── ApplicationMemory.java           # Project-level memory
│   ├── GovernanceMemory.java            # Organization-level memory
│   │
│   ├── identity/
│   │   ├── IdentityCorrelationEngine.java    # Interface
│   │   └── DefaultIdentityCorrelationEngine  # Cross-platform matching
│   │
│   ├── risk/
│   │   ├── RiskDetectionEngine.java          # Interface
│   │   └── DefaultRiskDetectionEngine.java   # 8 detection rules
│   │
│   ├── model/
│   │   ├── KnowledgeUnit.java           # Core knowledge entity
│   │   ├── IdentityAccount.java         # Platform account record
│   │   ├── UnifiedIdentity.java         # Cross-platform identity
│   │   ├── EffectivePrivilegeProfile.java # Privilege analysis
│   │   ├── RiskIndicator.java           # Risk with severity + score
│   │   ├── PolicyReference.java         # NIST/PAM policy mapping
│   │   ├── Confidence.java              # 4-factor confidence model
│   │   └── KnowledgeType.java           # 8 knowledge categories
│   │
│   ├── context/
│   │   ├── DefaultContextAssembler.java # 5-factor context scoring
│   │   └── DefaultContextScorer.java    # Relevance × recency × confidence
│   │
│   ├── validation/
│   │   ├── DefaultConfidenceCalculator.java  # E×C×V×O formula
│   │   └── KeywordContradictionDetector.java # Consistency checking
│   │
│   ├── synthesis/
│   │   └── KeywordPatternDiscoveryEngine.java # Auto-pattern discovery
│   │
│   ├── promotion/
│   │   └── DefaultPromotionEngine.java  # Governance review pipeline
│   │
│   ├── outcome/
│   │   └── OutcomeLearningEngine.java   # Feedback loop
│   │
│   ├── storage/
│   │   └── FileMemoryStorage.java       # Jackson 3 persistence
│   │
│   ├── tools/
│   │   └── IdentityGovernanceTools.java # 8 Spring AI @Tool methods
│   │
│   └── advisors/
│       └── KnowledgeEvolutionAdvisor.java # Spring AI ChatClient Advisor
```

---

## 🎯 Why IKOS Wins

| Traditional IAM | Traditional AI Assistant | IKOS |
|-----------------|------------------------|------|
| Manages access | Retrieves documents | **Creates organizational memory** |
| Point-in-time snapshots | Stateless Q&A | **Continuous knowledge evolution** |
| Platform-specific | Generic knowledge | **Cross-platform identity intelligence** |
| Manual review | No learning | **Autonomous pattern discovery + learning** |
| Compliance checkboxes | No governance | **Human-in-the-loop governance review** |

### The Moat

> "Every identity event makes IKOS smarter. Every remediation outcome improves future recommendations. Every audit finding strengthens the knowledge base. **Knowledge compounds. That is the moat.**"

---

## 🔧 Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `ikos.storage` | `~/.ikos-demo` | Persistence directory |
| Dormant threshold | 90 days | Days before admin is flagged dormant |
| Privilege creep threshold | 30 perms | Minimum permissions to trigger alert |
| Auto-promotion threshold | 0.5 confidence | Minimum for governance auto-review |

---

## 📊 Knowledge Types

| Type | Description | Lifecycle |
|------|-------------|-----------|
| `RISK_OBSERVATION` | Detected security risk | Observation → Pattern → Knowledge |
| `SECURITY_INCIDENT` | Recorded security event | Direct observation |
| `REMEDIATION_ACTION` | Action taken to fix risk | Feeds outcome learning |
| `AUDIT_FINDING` | Compliance check result | Observation → Knowledge |
| `LOCAL_PATTERN` | Auto-discovered pattern | Pattern candidate → Validated |
| `SECURITY_KNOWLEDGE` | Promoted organizational wisdom | Permanent knowledge base |
| `RECOMMENDATION` | AI-generated remediation plan | Generated from risk + context |
| `PROMOTION_CANDIDATE` | Pending governance review | Nominate → Approve/Reject |

---

## 📜 Policy Framework

All recommendations map to standard compliance controls:

| Policy | Control | Example |
|--------|---------|---------|
| PAM-001 | Offboarding Synchronization | All platforms disabled within 24h |
| PAM-003 | Inactive Privileged Account Review | 90-day inactivity triggers review |
| PAM-004 | Privileged Access Review | 3+ platform admin requires quarterly review |
| PAM-007 | Third-Party Access Control | Contractor least-privilege with recertification |
| PAM-008 | Service Account Lifecycle | 90-day credential rotation for service accounts |
| PAM-009 | Separation of Duties | No admin on both IdP and cloud |
| NIST AC-2 | Account Management | Disable when no longer needed |
| NIST AC-5 | Separation of Duties | Prevent malicious activity without collusion |
| NIST AC-6 | Least Privilege | Minimal access for job function |
| NIST IA-5 | Authenticator Management | Enforce credential rotation |

---

## 👥 Team

Built as an extension to [Spring AI Agent Utils](https://github.com/spring-ai-community/spring-ai-agent-utils)

---

## 📄 License

Apache License 2.0
