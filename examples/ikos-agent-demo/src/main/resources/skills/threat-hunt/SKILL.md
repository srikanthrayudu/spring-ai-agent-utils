---
name: threat-hunt
description: Proactive threat hunting across the identity infrastructure — search for hidden privilege abuse, lateral movement paths, and indicators of compromise that standard detection may miss.
---

# Threat Hunt Skill

You are performing a proactive threat hunt across the enterprise identity infrastructure. This is NOT reactive incident response — you are actively looking for threats hiding in plain sight.

## Step 1: Reconnaissance

Use `ListAllIkosKnowledge` to understand the current knowledge state.
Use `ListIdentityRisks` to identify known risks.

## Step 2: Hunt for Hidden Threats

For each identity category, hunt for these Indicators of Compromise (IoCs):

### Service Accounts
1. Use `AnalyzeIdentityRisks` on any service account with admin privileges
2. Check for credential age > 90 days (stale credentials = high risk)
3. Look for service accounts with access to multiple platforms (blast radius)
4. Use `ComputeBlastRadius` on any service account with admin on 2+ platforms

### Dormant Admins
1. Find admin accounts with no login in 90+ days
2. Cross-reference with `DetectOffboardingGap` — are they terminated employees?
3. Check if dormant accounts have been granted NEW permissions recently (privilege spike)

### Lateral Movement Paths
1. Identify identities with admin on BOTH identity provider (AD/Okta) AND cloud (AWS)
2. These are the "keys to the kingdom" — compromise one, own everything
3. Use `QueryIdentityGraph` to map the full blast zone
4. Use `ComputeBlastRadius` on the top 3 highest-risk identities

### Toxic Privilege Combinations
1. SoD violations: admin on identity provider + cloud infrastructure
2. Offboarding gaps: disabled on one platform, active on others
3. Orphaned accounts: active accounts with no identifiable owner

## Step 3: Assess & Prioritize

For each finding:
1. Use `ComplianceCheck` to map to regulatory controls
2. Assign a threat score based on blast radius + privilege level + time-at-risk
3. Determine if immediate containment is needed (use `ContainIdentity` if critical)
4. If extremely high risk, escalate using `EscalateToSOC`

## Step 4: Report

Structure your threat hunt report:
- **Hunt Summary**: What you looked for and what you found
- **Critical Findings**: Threats that need immediate action
- **Hidden Risks**: Risks that were not in the standard detection ruleset
- **Recommended Detections**: New detection rules to add based on findings
- **MITRE ATT&CK Mapping**: Map findings to T1078, T1098, T1550, etc.
