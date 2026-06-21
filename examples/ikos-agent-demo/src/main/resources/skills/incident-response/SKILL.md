---
name: incident-response
description: Execute a structured SOC incident response workflow for a compromised identity — triage, investigate, contain, remediate, document, and verify.
---

# Incident Response Skill

You are executing a structured SOC incident response for a potentially compromised identity. Follow the NIST SP 800-61 incident handling lifecycle.

## Phase 1: Detection & Triage

1. Use `AnalyzeIdentityRisks` with the affected identity's name
2. Assess severity:
   - 🔴 CRITICAL: Admin on 3+ platforms, or active SoD violation with offboarding gap
   - 🟠 HIGH: Admin on 2 platforms, or dormant admin with no MFA
   - 🟡 MEDIUM: Stale credentials or orphaned account
   - 🟢 LOW: Privilege creep without admin access
3. Use `ComputeBlastRadius` to understand the full impact zone

## Phase 2: Containment

Based on severity:

### CRITICAL — Immediate containment
1. Use `ContainIdentity` with action `DISABLE` — disable all accounts
2. Use `ContainIdentity` with action `REVOKE_SESSIONS` — kill active sessions
3. Use `EscalateToSOC` with the reason for escalation

### HIGH — Targeted containment
1. Use `ContainIdentity` with action `ISOLATE` — restrict to read-only
2. Use `ContainIdentity` with action `MONITOR` — enable enhanced logging

### MEDIUM/LOW — Monitoring
1. Use `ContainIdentity` with action `MONITOR` — enhanced logging only
2. Schedule review within 24-48 hours

## Phase 3: Investigation

1. Use `QueryIdentityGraph` to find all connected identities and platforms
2. Use `ListAllIkosKnowledge` to check for related incidents or patterns
3. Check for Indicators of Compromise:
   - Off-hours access attempts
   - Privilege escalation events
   - Access to unusual resources
   - Token reuse or credential sharing
4. Document all findings using `RecordObservation`

## Phase 4: Eradication & Remediation

1. Use `RecommendRemediation` to generate policy-aligned remediation steps
2. Execute remediation actions:
   - Rotate all credentials
   - Remove excessive privileges
   - Disable unnecessary accounts
   - Update access policies
3. Record actions using `RecordRemediationAction` with success/failure status

## Phase 5: Recovery & Verification

1. Verify containment actions were effective
2. Re-run `AnalyzeIdentityRisks` to confirm risk level has decreased
3. Re-run `ComplianceCheck` to verify compliance posture improvement
4. Record the incident using `RecordSecurityIncident` with full timeline

## Phase 6: Post-Incident

1. Use `ComplianceCheck` to generate final compliance impact report
2. Create TodoWrite tasks for:
   - Lessons learned documentation
   - Detection rule improvements
   - Policy updates
   - Training recommendations
3. Record observations using `RecordObservation` for the knowledge evolution pipeline

## Report Structure

Generate a structured incident report:
- **Incident ID**: Auto-generated
- **Timeline**: Detection → Containment → Remediation → Recovery
- **Affected Identity**: Name, platforms, risk level
- **Root Cause**: What caused the risk
- **Actions Taken**: Each containment/remediation step with timestamps
- **Compliance Impact**: NIST/GDPR/CIS control references
- **Lessons Learned**: What to improve for next time
- **MITRE ATT&CK**: Applicable techniques (T1078, T1098, T1550)
