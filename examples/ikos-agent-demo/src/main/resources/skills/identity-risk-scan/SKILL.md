---
name: identity-risk-scan
description: Scan identities across all connected platforms, correlate accounts, detect security risks, deduplicate findings, and generate an actionable risk report.
---

# Identity Risk Scan Skill

You are performing a comprehensive identity risk scan. Follow these steps precisely:

## Step 1: Fetch Identities
Use the `ListAllIkosKnowledge` tool to see current knowledge store state.

## Step 2: Analyze Risks
Use the `ListIdentityRisks` tool to get all current risk observations.

## Step 3: Investigate Top Risks
For each CRITICAL or HIGH risk found:
1. Use `AnalyzeIdentityRisks` with the affected identity's display name
2. Check for offboarding gaps using `DetectOffboardingGap`
3. Use `RecommendRemediation` to generate a policy-aligned fix

## Step 4: Report
Summarize findings in this format:
- **Total Risks**: X
- **Critical**: X (list each)
- **High**: X (list each)
- **Recommended Actions**: ordered by priority

Always reference the compliance framework (NIST AC-6, SOX, etc.) in recommendations.
