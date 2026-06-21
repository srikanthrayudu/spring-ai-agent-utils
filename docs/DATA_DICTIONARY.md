# 📖 IKOS Data Dictionary

> Complete schema reference for all simulated identity telemetry data

---

## 📊 Dataset Overview

| Metric | Value | Description |
|--------|-------|-------------|
| **Unique Identities** | 200 | Individual people/service accounts |
| **Total Accounts** | 405 | Platform-specific accounts across 5 platforms |
| **Platforms** | 5 | ActiveDirectory, AWS_IAM, Okta, Salesforce, ServiceNow |
| **Audit Events** | 1,000 | Login, privilege change, resource access events |
| **Offboarding Records** | 51 | Termination records with platform disable status |
| **Temporary Exceptions** | 20 | Time-bound admin grants with revocation tracking |
| **Group Hierarchy** | 25 groups | Nested group→role→permission inheritance chains |

### Anomaly Distribution

| Category | Count | Percentage | Description |
|----------|-------|------------|-------------|
| Normal Users | 102 | 51% | Standard employees with baseline access |
| Offboarding Gaps | 21 | 11% | Disabled on one platform, active on another |
| Over-Privileged | 17 | 9% | Admin across 3+ platforms simultaneously |
| Dormant Admins | 12 | 6% | Admin accounts inactive >90 days |
| Token/Svc Abuse | 10 | 5% | Stale service accounts with admin privileges |
| Stale Exceptions | 10 | 5% | Temporary admin grants never revoked |
| Legitimate High-Priv | 28 | 14% | Justified admin access (false positive traps) |

---

## 📋 Core Data Models

### 1. `IdentityAccount` — Platform Account Record

Represents a single account on a specific identity platform. Multiple records are correlated into a `UnifiedIdentity`.

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `accountId` | String | ✅ | Platform-specific identifier | `john.smith`, `Jsmith`, `john.smith@acme.com` |
| `platform` | String | ✅ | Identity platform name | `ActiveDirectory`, `AWS_IAM`, `Okta`, `Salesforce`, `ServiceNow` |
| `displayName` | String | ✅ | Human-readable display name | `John Smith` |
| `email` | String | ⬜ | Email address | `john.smith@acme.com` |
| `employeeId` | String | ⬜ | Cross-platform employee ID | `EMP-0042` |
| `status` | Enum | ✅ | Account lifecycle state | `ACTIVE`, `DISABLED`, `LOCKED`, `SUSPENDED`, `PENDING_DELETION` |
| `roles` | List\<String\> | ✅ | Assigned roles on this platform | `["Domain Admins", "Schema Admins"]` |
| `groups` | List\<String\> | ✅ | Group memberships | `["IT-Team", "Security-Team"]` |
| `isAdmin` | Boolean | ✅ | Has admin-level privileges | `true` / `false` |
| `lastLogin` | DateTime | ⬜ | Last authentication timestamp | `2026-06-15T18:42:01` |
| `createdAt` | DateTime | ⬜ | Account creation timestamp | `2023-03-14T09:00:00` |
| `metadata` | Map\<String,String\> | ⬜ | Platform-specific attributes | `{"department": "Security", "manager": "CISO"}` |
| `accountType` | Enum | ✅ | Identity classification | `EMPLOYEE`, `CONTRACTOR`, `SERVICE_ACCOUNT`, `API_TOKEN` |
| `credentialAgeDays` | Integer | ⬜ | Days since credential rotation | `180` (or `-1` if unknown) |
| `lastCredentialRotation` | DateTime | ⬜ | Last key/password rotation | `2026-01-15T00:00:00` |

#### Account ID Conventions by Platform

| Platform | ID Format | Example |
|----------|-----------|---------|
| ActiveDirectory | `firstname.lastname` | `john.smith` |
| AWS_IAM | `FlastName` (first initial + last name) | `Jsmith` |
| Okta | `email` | `john.smith@acme.com` |
| Salesforce | `email` | `john.smith@acme.com` |
| ServiceNow | `accountId` (service accounts) | `svc-etl-prod-8` |

#### Sample Record (JSON)

```json
{
  "accountId": "john.smith",
  "platform": "ActiveDirectory",
  "displayName": "John Smith",
  "email": "john.smith@acme.com",
  "employeeId": "EMP-0042",
  "status": "ACTIVE",
  "roles": ["Domain Admins", "Enterprise Admins"],
  "groups": ["Security-Team"],
  "isAdmin": true,
  "lastLogin": "2026-06-15T18:42:01",
  "createdAt": "2022-03-14T09:00:00",
  "metadata": {"department": "Security", "manager": "CISO"},
  "accountType": "EMPLOYEE",
  "credentialAgeDays": -1,
  "lastCredentialRotation": null
}
```

---

### 2. `UnifiedIdentity` — Cross-Platform Identity Profile

The output of the Identity Correlation Engine. Maps one person to all their platform accounts.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `unifiedId` | String | Stable unique identifier | `UID-0042` |
| `displayName` | String | Canonical display name | `John Smith` |
| `email` | String | Primary email | `john.smith@acme.com` |
| `employeeId` | String | Employee identifier | `EMP-0042` |
| `department` | String | Organizational department | `Security` |
| `accounts` | List\<IdentityAccount\> | All linked platform accounts | *3 accounts across AD, AWS, Okta* |
| `metadata` | Map\<String,String\> | Additional attributes | `{"title": "Senior Engineer"}` |

#### Computed Properties

| Property | Type | Logic |
|----------|------|-------|
| `platforms()` | Set\<String\> | Distinct platforms from all accounts |
| `adminPlatformCount()` | long | Number of platforms with admin access |
| `activePlatforms()` | Set\<String\> | Platforms where status = ACTIVE |
| `disabledPlatforms()` | Set\<String\> | Platforms where status = DISABLED |
| `isCrossPlatformAdmin()` | boolean | Admin on ≥3 platforms |
| `hasOffboardingGap()` | boolean | Disabled on any platform + active on any other |
| `computeRiskScore()` | double [0.0–1.0] | Weighted risk: offboarding(0.35) + admin(0.30) + dormant(0.25) + spread(0.10) |

---

### 3. `AuditEvent` — Platform Audit Log Entry

Represents a single event from an identity platform's audit log.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `eventId` | String | Unique event identifier | `EVT-00001` |
| `accountId` | String | Account that generated the event | `john.smith` |
| `platform` | String | Source platform | `ActiveDirectory` |
| `eventType` | Enum | Event classification | See enum below |
| `description` | String | Human-readable description | `"Successful login to ActiveDirectory"` |
| `timestamp` | DateTime | When the event occurred | `2026-06-15T14:23:00` |
| `sourceIp` | String | Originating IP address | `10.0.1.42` or `203.0.113.55` |
| `targetResource` | String | Resource accessed/modified | `prod-db`, `staging-s3`, `okta-sso` |
| `success` | boolean | Whether the action succeeded | `true` / `false` |
| `metadata` | Map\<String,String\> | Event-specific attributes | `{}` |

#### Event Types

| EventType | Description | Frequency |
|-----------|-------------|-----------|
| `LOGIN_SUCCESS` | Successful authentication | ~40% |
| `LOGIN_FAILURE` | Failed login attempt | ~10% |
| `PRIVILEGE_GRANT` | Role or permission granted | ~8% |
| `ROLE_CHANGE` | Role assignment modified | ~5% |
| `RESOURCE_ACCESS` | Resource read/write | ~10% |
| `API_CALL` | API endpoint invocation | ~7% |
| `TOKEN_USAGE` | API token authentication | ~5% |
| `GROUP_ADD` | Added to security group | ~5% |
| `PASSWORD_CHANGE` | Password/credential updated | ~5% |
| `MFA_BYPASS` | MFA bypassed or failed | ~5% |

#### IP Address Ranges

| Range | Classification | Use |
|-------|---------------|-----|
| `10.0.x.x` | Corporate internal | Normal office access |
| `172.16.x.x` | Corporate internal | VPN access |
| `203.0.113.x` | External | Suspicious — test/documentation range |
| `198.51.100.x` | External | Suspicious — test/documentation range |
| `185.220.101.x` | External | Known Tor exit node range |
| `91.219.237.x` | External | Known malicious actor range |
| `45.33.32.x` | External | Scanner/recon range |

---

### 4. `OffboardingRecord` — Employee Termination Record

Tracks employee termination and per-platform account disable status.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `employeeId` | String | Employee identifier | `EMP-0042` |
| `displayName` | String | Employee name | `John Smith` |
| `terminationDate` | LocalDate | Date of termination | `2026-03-15` |
| `platformDisableStatus` | Map\<String, Boolean\> | Per-platform: true=disabled, false=still active | `{"AD": true, "AWS": false}` |
| `terminationType` | Enum | Termination classification | `VOLUNTARY`, `INVOLUNTARY`, `CONTRACT_END`, `RETIREMENT` |

**Key Insight:** When `platformDisableStatus` has mixed values (some true, some false), this represents an **offboarding gap** — the highest-severity identity risk.

---

### 5. `TemporaryAccessException` — Time-Bound Admin Grant

Tracks temporary elevated access with revocation status.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `exceptionId` | String | Exception identifier | `EXC-0005` |
| `employeeId` | String | Employee identifier | `EMP-0006` |
| `displayName` | String | Grantee name | `Andrew Wilson` |
| `grantedPlatforms` | List\<String\> | Platforms with elevated access | `["AD", "AWS", "Okta"]` |
| `grantedRoles` | List\<String\> | Elevated roles granted | `["Domain Admins", "AdministratorAccess", "SuperAdmin"]` |
| `grantedAt` | DateTime | When access was granted | `2026-05-19T18:42:01` |
| `expiresAt` | DateTime | When access should expire | `2026-05-22T18:42:01` |
| `reason` | String | Justification | `"Incident response P1"` |
| `revokedPlatforms` | List\<String\> | Platforms where access was revoked | `["AD"]` ← only 1 of 3! |
| `approver` | String | Who approved the exception | `"CISO"`, `"VP-Engineering"` |

**Key Insight:** When `grantedPlatforms.size() > revokedPlatforms.size()` and `expiresAt < now()`, this is a **stale exception** — admin privileges granted temporarily but never revoked.

---

### 6. `GroupHierarchy` — Nested Group Membership Tree

Models the nested group→parent→permission inheritance that creates hidden effective privileges.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `groupName` | String | Group identifier | `IT-Team` |
| `platform` | String | Platform this group belongs to | `ActiveDirectory` |
| `parentGroups` | Set\<String\> | Groups this group is nested under | `{"AD-Admins"}` |
| `grantedPermissions` | Set\<String\> | Direct permissions from this group | `{"read:servers"}` |
| `isAdminGroup` | boolean | Whether this grants admin-level access | `false` |

#### Enterprise Hierarchy (Pre-built)

```
ActiveDirectory:
  Domain Users (base) → VPN-Users → Remote Desktop Users
  IT-Team → AD-Admins → Domain Admins (ADMIN)
  Security-Team → AD-Admins → Domain Admins (ADMIN)
  Contractors → IT-Team → AD-Admins → Domain Admins (HIDDEN ADMIN!)

AWS_IAM:
  ReadOnly → PowerUserAccess
  DevOps → CloudAdmins (ADMIN)
  ContractorDevs → DevOps → CloudAdmins (HIDDEN ADMIN!)
  DataEngineers → S3FullAccess → CloudAdmins (HIDDEN ADMIN!)

Okta:
  Everyone → AppAdmin → GlobalAdmin (ADMIN)
  HelpDeskAdmin → SuperAdmin (ADMIN)
```

**Key Insight:** A user assigned to `Contractors` in AD inherits admin through `Contractors → IT-Team → AD-Admins → Domain Admins`. This is Case 2 from the problem statement.

---

### 7. `RiskIndicator` — Detected Risk Finding

| Field | Type | Description |
|-------|------|-------------|
| `riskId` | String | Unique risk identifier (e.g., `RISK-0042`) |
| `riskType` | RiskType | Classification of risk |
| `severity` | RiskSeverity | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `identityId` | String | Affected unified identity |
| `platforms` | List\<String\> | Platforms involved |
| `statement` | String | Human-readable risk description |
| `evidence` | List\<Evidence\> | Supporting evidence chain |
| `remediation` | List\<String\> | Recommended actions |
| `policyReferences` | List\<PolicyReference\> | Mapped compliance controls |
| `riskScore` | double | Computed risk score [0.0–1.0] |
| `confidence` | double | Detection confidence [0.0–1.0] |

#### Risk Types (8 Detection Rules)

| RiskType | Severity | MITRE ATT&CK | Description |
|----------|----------|--------------|-------------|
| `OFFBOARDING_GAP` | CRITICAL | T1078: Valid Accounts | Disabled on platform A, active on B |
| `CROSS_PLATFORM_ADMIN` | HIGH | T1098: Account Manipulation | Admin on 3+ platforms |
| `DORMANT_ADMIN` | HIGH | T1078: Valid Accounts | Admin inactive >90 days |
| `ORPHANED_ACCOUNT` | MEDIUM | T1078: Valid Accounts | Active account with no owner |
| `PRIVILEGE_CREEP` | MEDIUM | T1098: Account Manipulation | 30+ permissions with sensitive ones |
| `CONTRACTOR_EXCESSIVE_ACCESS` | HIGH | T1078: Valid Accounts | Non-employee with admin privileges |
| `STALE_SERVICE_ACCOUNT` | CRITICAL | T1550: Use Alternate Auth | Service account admin unused >180 days |
| `SOD_VIOLATION` | CRITICAL | T1098: Account Manipulation | Admin on both IdP AND cloud |

---

### 8. `KnowledgeUnit` — Knowledge Evolution Record

The core entity of the knowledge evolution pipeline.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique knowledge unit ID |
| `statement` | String | Knowledge statement |
| `type` | KnowledgeType | Category classification |
| `state` | KnowledgeState | Evolution stage |
| `context` | String | Contextual information |
| `confidence` | double | Confidence score [0.0–1.0] |
| `evidence` | List\<String\> | Supporting evidence |
| `tags` | Set\<String\> | Classification tags |
| `lastReviewed` | DateTime | Last review timestamp |
| `reviewedBy` | String | Reviewer identity |

#### Knowledge States (Evolution Pipeline)

| State | Confidence | Description |
|-------|-----------|-------------|
| `OBSERVATION` | 0.3 | Initial detection — raw finding |
| `PATTERN_CANDIDATE` | 0.5 | Auto-discovered recurring pattern |
| `VALIDATED_PATTERN` | 0.7 | Governance-reviewed and confirmed |
| `KNOWLEDGE` | 0.9 | Promoted to organizational security knowledge |

#### Knowledge Types

| Type | Description |
|------|-------------|
| `RISK_OBSERVATION` | Detected security risk |
| `SECURITY_INCIDENT` | Recorded security event |
| `REMEDIATION_ACTION` | Action taken to fix risk |
| `AUDIT_FINDING` | Compliance check result |
| `LOCAL_PATTERN` | Auto-discovered pattern |
| `SECURITY_KNOWLEDGE` | Promoted organizational wisdom |
| `RECOMMENDATION` | AI-generated remediation plan |
| `PROMOTION_CANDIDATE` | Pending governance review |

---

### 9. `Confidence` — 4-Factor Confidence Model

```
Confidence = Evidence × Consistency × Validation × Outcome
```

| Factor | Range | Description |
|--------|-------|-------------|
| `evidence` | 0.3–1.0 | Number of supporting evidence items |
| `consistency` | 0.0–1.0 | Absence of contradictions |
| `validation` | 0.5–1.0 | Review quality: human(1.0), auto(0.7), none(0.5) |
| `outcome` | 0.0–1.0 | Success rate of applied remediations |

---

## 🔗 Cross-Platform Identity Resolution

The Identity Correlation Engine resolves accounts across platforms using:

1. **Employee ID match** — exact match on `employeeId` field
2. **Email match** — exact match on `email` field
3. **Display name match** — fuzzy matching on `displayName`

### Example Resolution

```
john.smith (ActiveDirectory)  ← employeeId: EMP-0042
Jsmith (AWS_IAM)              ← employeeId: EMP-0042
john.smith@acme.com (Okta)    ← employeeId: EMP-0042
    → UnifiedIdentity: UID-0042 (John Smith) — 3 platforms
```

---

## 📐 Compliance Control Mapping

| Standard | Control | IKOS Detection |
|----------|---------|----------------|
| NIST AC-2 | Account Management | Offboarding gaps, orphaned accounts |
| NIST AC-5 | Separation of Duties | SoD violations (IdP + cloud admin) |
| NIST AC-6 | Least Privilege | Cross-platform admin, privilege creep |
| NIST IA-4 | Identifier Management | Cross-platform identity correlation |
| NIST IA-5 | Authenticator Management | Credential rotation violations |
| MITRE T1078 | Valid Accounts | Offboarding gaps, dormant admins |
| MITRE T1098 | Account Manipulation | Privilege escalation, SoD violations |
| MITRE T1550 | Use Alternate Auth Material | Stale service accounts, token misuse |
| GDPR Art.5 | Data Minimisation | Excessive access scope |
| GDPR Art.32 | Security of Processing | Identity controls across systems |
| CIS Control 5 | Account Management | Full lifecycle coverage |
| CIS Control 6 | Access Control | Privilege scope enforcement |

---

*Generated by IKOS — Identity Knowledge Operating System*
*Data simulation seed: 42 (deterministic for reproducibility)*
