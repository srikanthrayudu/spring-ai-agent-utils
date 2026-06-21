# Enterprise Expectations

This document explains what an enterprise identity-security user would expect from an Identity Sprawl and Privileged Access Abuse Detection product, and how IKOS addresses those expectations in the hackathon prototype.

## Buyer Persona

IKOS is aimed at security engineering, IAM governance, GRC, and SOC teams in hybrid enterprises where identity data is spread across directory services, cloud IAM, SSO, and SaaS platforms.

The enterprise user is not looking for another login-anomaly dashboard. They want a system that answers:

- Who is this person or service account across all platforms?
- What access do they effectively have, including inherited privilege?
- Is the access still justified by lifecycle state, owner, department, exception, and recent usage?
- Which finding should be remediated first?
- What exact platform action should be taken?
- Can the finding survive audit scrutiny?

## Expectations And IKOS Coverage

| Enterprise expectation | Why it matters | IKOS prototype coverage |
| --- | --- | --- |
| Identity coverage | Gaps in identity inventory become breach paths | Simulates 200 identities and 405 accounts across AD, AWS IAM, Okta, Salesforce, and ServiceNow |
| Identity resolution | Same person appears with different platform IDs | Correlates by employee ID, email, display name, and platform account patterns |
| Effective privilege | Flat role lists miss inherited admin paths | Models nested groups and computes hidden admin access through traversal |
| Cross-platform reasoning | One platform cannot see toxic combinations | Scores risks across identity provider, cloud, and SaaS accounts together |
| Lifecycle validation | Offboarding errors create active attacker paths | Detects disabled-in-one-platform but active-in-another conditions |
| Service account governance | Service accounts often have excessive persistent privilege | Detects ownerless, stale, admin, and unrotated service/API accounts |
| Temporary access expiry | Emergency access often survives after policy expiry | Models exceptions and flags stale grants not revoked across all platforms |
| False-positive handling | Legitimate admins should not drown the queue | Includes legitimate high-privilege identities as false-positive traps |
| Alert consolidation | SOC teams need incidents, not duplicate alerts | Consolidates repeated findings by identity and risk family |
| Explainability | Audit and remediation require evidence | Stores evidence, policy references, confidence, and remediation steps |
| Governance readiness | Findings must map to controls | Maps to NIST AC-2/AC-6/IA-4, GDPR Articles 5/32, CIS 5/6, and MITRE ATT&CK |

## What Judges Should See First

The strongest demo order is:

1. Dashboard overview: show identity coverage, risk counts, and alert reduction.
2. Offboarding gap: show terminated user disabled in AD but active in AWS/Okta.
3. Hidden privilege: show nested group inheritance causing admin access.
4. Risk report: show evidence and platform-specific remediation.
5. Knowledge evolution: show why this is more than static rules.
6. Spring AI agent layer: show the LLM-callable tools and subagent workflow.

## Production Expectations Beyond The Prototype

The prototype is intentionally simulated because the problem statement does not provide a dataset. For production, an enterprise would expect:

| Production need | Implementation path |
| --- | --- |
| Real connectors | Implement `IdentityDataSource` for LDAP/AD, Microsoft Graph, AWS IAM, Okta, Salesforce, and ServiceNow |
| Secure credential handling | Use vault-backed secrets and read-only API scopes |
| Incremental scans | Replace full simulation runs with scheduled delta ingestion |
| Reviewer workflows | Add approval queues for exception validation and remediation sign-off |
| SOAR/SIEM integration | Push consolidated incidents via REST API or webhook |
| Historical baselines | Persist feature windows for behavioral drift and privilege spike detection |
| Access graph storage | Move from in-memory/file persistence to graph or relational storage |
| Policy tuning | Externalize thresholds by department, region, account type, and platform |

## Recommended Hackathon Positioning

Do not pitch IKOS as a generic IAM dashboard. Pitch it as:

> A cross-platform identity intelligence layer that detects the attack paths created between IAM systems, then turns each finding and remediation outcome into reusable security knowledge.

That positioning matches the problem statement and creates clear uniqueness against basic rule engines.
