# 🎬 IKOS Demo Script — 5-Minute Hackathon Presentation

## Opening (30 seconds)

> "Every enterprise has a ticking time bomb — **identity sprawl**. When a contractor leaves and their AD account is disabled, but their AWS and Okta accounts stay active for 4 months... that's how breaches happen. IKOS finds these invisible risks across platforms and **learns from every interaction**."

---

## Demo Flow

### Step 1: Dashboard Overview (60 seconds)

**Open:** `ikos-dashboard.html` → Overview tab

**Talk through the summary cards:**
- "200 unified identities across 5 platforms — AD, AWS, Okta, Salesforce, ServiceNow"
- "81 CRITICAL risks detected — these are the ones that need action TODAY"
- "82% alert noise reduction — 148 raw alerts consolidated to 26 actionable incidents"

**Click:** Heatmap tab → "This privilege heatmap shows which departments have the most admin access. Engineering and Security are hot zones."

### Step 2: Identity Graph (60 seconds)

**Click:** Graph tab

**Talk:** "This is the cross-platform identity graph. Each node is a person. The lines connect them to platforms. RED nodes are high-risk identities."

**Click a red node:** "Eric Carter — admin on AD, AWS, AND Okta simultaneously. That's a Separation of Duties violation. If his credentials are compromised, the attacker has keys to everything."

### Step 3: Risk Drill-Down (60 seconds)

**Click:** Risks tab → Click "Detail" on a CRITICAL SoD violation

**Talk:** "Every risk has an evidence chain — these are the actual account records proving the violation. And platform-specific remediation steps — 'Split admin roles', 'Implement break-glass procedure', 'Enable enhanced monitoring.'"

**Click:** Show an OFFBOARDING_GAP risk → "This is Case 1 from the problem statement — Stephanie Johnson was terminated, AD was disabled, but her AWS and Okta accounts are STILL ACTIVE 3 months later."

### Step 4: Knowledge Evolution (60 seconds)

**Talk:** "What makes IKOS unique isn't just detection — it's **learning**."

**Show pipeline concept:**
1. "First detection = OBSERVATION (30% confidence)"
2. "Same pattern detected 3 times = auto-promoted to PATTERN_CANDIDATE (50%)"
3. "Governance review confirms it = VALIDATED_PATTERN (70%)"
4. "Remediation succeeds = SECURITY_KNOWLEDGE (90%)"

> "Every incident makes IKOS smarter. That's the moat — knowledge compounds."

### Step 5: Compliance & Report (30 seconds)

**Click:** Compliance tab → "Every finding maps to NIST AC-2, AC-6, MITRE ATT&CK, GDPR Article 5 and 32."

**Click:** Report tab → "CISO-ready risk report with top 10 identities and specific remediation steps."

---

## Closing (30 seconds)

> "IKOS doesn't just manage access — it creates **organizational security memory**. Built on Spring AI and Spring Boot — the same stack running 60% of enterprise Java apps. Zero new infrastructure. Plug it into your existing identity pipeline."

---

## Anticipated Judge Questions

**Q: "How does this differ from a static rule engine?"**
A: "Static engines detect the same things every time with zero learning. IKOS has a 4-stage knowledge evolution pipeline — confidence grows with evidence, governance review, and outcome feedback. It discovers patterns autonomously."

**Q: "How does the AI/ML component work?"**
A: "We use Spring AI's autonomous agent loop — the LLM chains tool calls (ListRisks → Analyze → Recommend → Execute) without human intervention. Plus behavioral anomaly detection with 6 rules scoring identity risk profiles."

**Q: "Is the data simulated?"**
A: "Yes, as required by the problem statement. We generate 200 identities, 405 accounts across 5 platforms, 1000+ audit events with realistic anomaly distribution matching the specified ratios. Deterministic seed for reproducibility."

**Q: "How does nested group inheritance work?"**
A: "BFS traversal of the group hierarchy. A user in 'Contractors' group inherits admin through Contractors → IT-Team → AD-Admins → Domain Admins. This is exactly Case 2 from the problem — hidden privilege through nested groups."

**Q: "Can this work with real data?"**
A: "The `IdentityDataSource` interface is pluggable. Replace `SimulatedDataSource` with LDAP, AWS SDK, or Okta API connectors. The correlation, detection, and knowledge engines work with any data source."

**Q: "How do you handle false positives?"**
A: "18% of our simulated identities are legitimate high-privilege users — false positive traps. The system flags them but the knowledge evolution pipeline learns from governance review decisions to adjust confidence over time."
