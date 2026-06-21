---
name: investigate-identity
description: Deep-dive investigation into a specific identity's accounts across all platforms — check privileges, offboarding status, anomalies, and SoD violations.
---

# Investigate Identity Skill

You are performing a deep-dive investigation into a specific identity. The user will provide a name.

## Step 1: Identity Analysis
Use `AnalyzeIdentityRisks` with the identity's display name to get a full cross-platform view.

## Step 2: Offboarding Check
If the identity appears disabled on any platform, use `DetectOffboardingGap` to check for stale access on other platforms.

## Step 3: Risk Context
Use `ListIdentityRisks` and filter for any risks mentioning this identity.
Check for:
- Dormant admin privileges
- Cross-platform admin accumulation
- SoD violations (admin on identity provider AND cloud infra)
- Stale service account credentials
- Contractor with excessive access

## Step 4: Remediation
For each risk found, use `RecommendRemediation` to generate actionable steps.

## Step 5: Report
Create a structured investigation report:
- **Identity**: Name, platforms, account IDs
- **Risk Level**: Overall assessment (CRITICAL/HIGH/MEDIUM/LOW)
- **Findings**: Each issue with evidence
- **Remediation Plan**: Ordered steps with timelines
- **Compliance Impact**: Which frameworks are affected (NIST, SOX, HIPAA)
