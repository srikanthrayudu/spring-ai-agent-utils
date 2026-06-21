---
name: compliance-report
description: Generate a compliance report mapping all IKOS risks to regulatory framework controls (NIST 800-53, SOX, HIPAA, ISO 27001).
---

# Compliance Report Skill

You are generating a compliance mapping report. Map every known risk to regulatory controls.

## Step 1: Gather All Risks
Use `ListIdentityRisks` to retrieve every active risk observation.
Use `ListAllIkosKnowledge` to also get incidents and audit findings.

## Step 2: Map to Compliance Frameworks
For each risk, map it to relevant controls:

| Risk Type | NIST 800-53 | SOX | HIPAA | ISO 27001 |
|-----------|-------------|-----|-------|-----------|
| Dormant Admin | AC-2(3) | IT-GC | §164.312(a) | A.9.2.5 |
| Offboarding Gap | PS-4 | IT-GC | §164.312(a) | A.7.3.1 |
| Cross-Platform Admin | AC-6(1) | IT-GC | §164.312(a) | A.9.2.3 |
| SoD Violation | AC-5 | IT-GC | §164.312(a) | A.6.1.2 |
| Privilege Creep | AC-6 | IT-GC | §164.312(a) | A.9.2.5 |
| Stale Service Acct | AC-2(3) | IT-GC | §164.312(d) | A.9.4.3 |
| Credential Rotation | IA-5(1) | IT-GC | §164.312(d) | A.9.4.3 |

## Step 3: Generate Report
Structure the output as:
1. **Executive Summary** — risk counts by severity
2. **Compliance Matrix** — each risk mapped to controls
3. **Gaps** — controls not covered by current IKOS detection
4. **Recommendations** — remediation priorities by compliance impact

## Step 4: Record
Use `RecordAuditFinding` to persist each compliance gap found.
