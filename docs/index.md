# IKOS Documentation

IKOS is a hackathon prototype for Identity Sprawl and Privileged Access Abuse Detection in hybrid enterprises.

It consolidates simulated identity records from Active Directory, AWS IAM, Okta, Salesforce, and ServiceNow; resolves effective privilege; detects cross-platform identity risk; consolidates alert noise; and produces explainable remediation guidance.

## Start Here

| Document | Use it for |
| --- | --- |
| [Enterprise Expectations](ENTERPRISE_EXPECTATIONS.md) | Understanding what an enterprise user or judge should expect from the product |
| [Architecture](ARCHITECTURE.md) | System design, data flow, detection engines, and AI/ML approach |
| [Data Dictionary](DATA_DICTIONARY.md) | Source records, generated telemetry, risk fields, and schema definitions |
| [Spring AI Agent Utils Integration](SPRING_AI_AGENT_UTILS_INTEGRATION.md) | How IKOS uses agent tools, advisors, skills, and subagents without becoming a framework-docs project |
| [Risk Report](RISK_REPORT.md) | Sample CISO-style output with top risky identities and remediation |

## Prototype Capabilities

| Capability | Status |
| --- | --- |
| Multi-platform simulated identity data | Implemented |
| Identity correlation | Implemented |
| Nested group and inherited privilege resolution | Implemented |
| Risk scoring with evidence | Implemented |
| Behavioral signal analysis | Implemented |
| Alert consolidation | Implemented |
| Governance knowledge evolution | Implemented |
| Spring AI tool integration | Implemented |
| Dashboard and sample report | Implemented |

## Demo Commands

Generate the dashboard:

```bash
./mvnw exec:java -pl ikos \
  -Dexec.mainClass=org.springaicommunity.agent.ikos.QuickDashboard
```

Run the verified IKOS tests:

```bash
./mvnw test -pl ikos
```

Run with Docker:

```bash
docker-compose up --build
```

## Recommended Pitch

Do not present IKOS as another IAM dashboard. Present it as a cross-platform identity intelligence layer:

> IKOS detects risk created between identity systems, not just inside one system. It then turns each finding, review, and remediation outcome into reusable security knowledge.
