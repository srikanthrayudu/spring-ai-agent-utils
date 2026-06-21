# 🏗️ IKOS Architecture Documentation

> **Identity Knowledge Operating System — System Architecture & AI/ML Approach**

## 1. System Overview

IKOS is a cybersecurity knowledge operating system that correlates identities from 5+ platforms into unified profiles, detects 8 categories of identity risk, and continuously evolves knowledge through a confidence-weighted learning pipeline.

### Design Principles
1. **Cross-Platform First** — Every component reasons across platform boundaries
2. **Knowledge Over Alerts** — Convert raw signals into organizational memory
3. **Explainable Decisions** — Every finding traceable to evidence and policy
4. **Continuous Learning** — Every interaction improves future detection
5. **Enterprise Integration** — Spring Boot/AI native for Java ecosystem adoption

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        IKOS Architecture                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  DATA INGESTION ──→ IDENTITY CORRELATION ──→ RISK DETECTION         │
│  (5 platforms)      (405→200 profiles)      (8 rules + behavioral)  │
│                                                                     │
│  ALERT CONSOLIDATION ──→ KNOWLEDGE EVOLUTION ──→ SPRING AI LAYER    │
│  (148→26 = 82% reduction) (4-stage pipeline)   (8 @Tool methods)   │
│                                                                     │
│  OUTPUT: Dashboard (10 tabs) + Risk Report + CSV Export             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Component Details

### 3.1 Data Ingestion Layer
- `SimulatedDataGenerator` — Deterministic seed (42) for reproducible demo data
- Generates 300+ accounts across AD, AWS IAM, Okta, Salesforce, ServiceNow
- Anomaly distribution: ~12% offboarding gaps, ~10% over-privileged, ~7% dormant admin, ~4% token abuse, ~15% false positive traps

### 3.2 Identity Correlation Engine
- **Algorithm:** Employee ID match (1.0 confidence) → Email match (0.95) → Display name fuzzy match (0.8)
- **Result:** 405 accounts → 200 UnifiedIdentity profiles

### 3.3 Risk Detection Engine (8 Rules)

| # | Rule | Severity | Logic |
|---|------|----------|-------|
| 1 | Offboarding Gap | CRITICAL | DISABLED on platform A + ACTIVE on B |
| 2 | Cross-Platform Admin | HIGH | Admin on ≥3 platforms |
| 3 | Dormant Admin | HIGH | Admin + lastLogin > 90 days |
| 4 | Orphaned Account | MEDIUM | Active account, no owner |
| 5 | Privilege Creep | MEDIUM | ≥30 permissions including sensitive |
| 6 | Contractor Excessive | HIGH | Non-employee with admin |
| 7 | Stale Service Account | CRITICAL | Service account admin, unused > 180 days |
| 8 | SoD Violation | CRITICAL | Admin on IdP AND cloud infrastructure |

### 3.4 Behavioral Analyzer (6 Rules)
- Privilege Spike: >3 privilege changes in 7 days (+0.30)
- Off-Hours Activity: >30% events outside 7AM-8PM (+0.20)
- Cross-Platform Cascade: ≥3 platforms in short window (+0.25)
- Excessive Failures: Failed > 2× successful logins (+0.25)
- Token Misuse: API token from external IP (+0.30)
- MFA Bypass: Any MFA bypass event (+0.35)

### 3.5 Alert Consolidation
- Groups risks by identity + risk type family
- 148 raw alerts → 26 consolidated incidents = **82% noise reduction**

### 3.6 Group Hierarchy (Effective Privilege Resolution)
- BFS traversal of nested group memberships
- Detects hidden admin through inheritance chains (e.g., Contractors → IT-Team → AD-Admins → Domain Admins)

---

## 4. Knowledge Evolution Pipeline

```
OBSERVATION (0.3) → PATTERN_CANDIDATE (0.5) → VALIDATED_PATTERN (0.7) → KNOWLEDGE (0.9)
     ↑                                                                         ↓
     └─────────────────── Outcome Learning Feedback Loop ──────────────────────┘
```

### 4.1 Confidence Model
```
FinalConfidence = Evidence × Consistency × Validation × Outcome
```
- **Evidence:** min(1.0, 0.3 + evidenceCount × 0.15)
- **Consistency:** 1.0 - contradictionCount × 0.2
- **Validation:** Human(1.0), Auto(0.7), None(0.5)
- **Outcome:** successfulRemediations / totalRemediations

### 4.2 Context Scoring (5-Factor)
```
ContextScore = Relevance × Confidence × Similarity × EvidenceStrength × Recency
```
Recency uses exponential decay: e^(-0.01 × days), ~70-day half-life.

### 4.3 Pattern Discovery
- Collects OBSERVATION-state units, extracts keywords
- Groups by shared keywords (≥3 occurrences)
- Auto-promotes to PATTERN_CANDIDATE

---

## 5. Spring AI Integration

### 5.1 Tool-Calling (8 @Tool Methods)
```java
@Tool("AnalyzeIdentityRisks")     // Risk analysis for one identity
@Tool("DetectOffboardingGap")      // Cross-platform status check
@Tool("RecordSecurityIncident")    // Log security events
@Tool("RecordRemediationAction")   // Log remediation steps
@Tool("RecordAuditFinding")        // Log audit findings
@Tool("RecommendRemediation")      // Generate fix steps with policy refs
@Tool("ListIdentityRisks")        // All active risks
@Tool("ListAllIkosKnowledge")     // Full knowledge store
```

### 5.2 Autonomous Agent Loop
5-iteration autonomous loop: ListRisks → Analyze → Recommend → CreateTasks → RecordRemediation. Zero human prompts required.

### 5.3 Advisors
- **KnowledgeEvolutionAdvisor:** Before=inject context, After=extract observations
- **AutoMemoryToolsAdvisor:** Persistent MEMORY.md file management

### 5.4 Subagents
- RISK_ANALYST — Deep-dive identity risk analysis
- COMPLIANCE_REVIEWER — Map to NIST/GDPR controls
- REMEDIATION_PLANNER — Platform-specific fix steps

---

## 6. MITRE ATT&CK Detection Matrix

| Technique | ID | IKOS Detection |
|-----------|-----|---------------|
| Valid Accounts | T1078 | Offboarding gaps, dormant admins, orphaned accounts |
| Account Manipulation | T1098 | Cross-platform admin, privilege creep, SoD violations |
| Use Alternate Auth Material | T1550 | Stale service accounts, token misuse |

---

## 7. Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Runtime | Java 25 | Core platform |
| Framework | Spring Boot 3.x/4.x | Application framework |
| AI/ML | Spring AI 2.0.0 | LLM tool-calling, advisors |
| LLM | Google Gemini 2.5 Flash | Autonomous agent loop |
| Persistence | Jackson 3 + File System | Knowledge serialization |
| Dashboard | HTML/CSS/JS + Chart.js 4.4.3 | Interactive visualization |
| Graph | vis.js Network | Identity relationship graph |
| Build | Maven 3.9+ | Multi-module build |
| Container | Docker (multi-stage) | Deployment |

---

*IKOS Architecture Document v1.0*
