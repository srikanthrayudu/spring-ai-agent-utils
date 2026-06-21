# 🛡️ IKOS Identity Risk Intelligence Report

> **Acme Corporation — Quarterly Identity Access Review**
> **Report Date:** 2026-06-20 | **Classification:** CONFIDENTIAL
> **Prepared by:** IKOS Autonomous Security Intelligence System

---

## Executive Summary

IKOS analyzed **200 unified identities** across **5 platforms** (Active Directory, AWS IAM, Okta, Salesforce, ServiceNow) comprising **405 total accounts** and **1,000+ audit events**.

| Metric | Value |
|--------|-------|
| **Total Risks Detected** | 148 |
| **Critical Risks** | 81 |
| **High Risks** | 52 |
| **Offboarding Gaps** | 18 active |
| **SoD Violations** | 15 active |
| **Stale Service Accounts** | 7 critical |
| **Alert Noise Reduction** | 82% (148 raw → 26 consolidated) |
| **Identities Requiring Immediate Action** | 10 |

### Risk Distribution

| Severity | Count | Percentage |
|----------|-------|------------|
| 🔴 CRITICAL | 81 | 55% |
| 🟠 HIGH | 52 | 35% |
| 🟡 MEDIUM | 15 | 10% |

---

## Top 10 Risky Identities — Immediate Action Required

### 1. 🔴 Eric Carter (UID-0003) — Risk Score: 95%

| Field | Value |
|-------|-------|
| **Employee ID** | EMP-0004 |
| **Risk Types** | SoD Violation, Cross-Platform Admin |
| **Platforms** | ActiveDirectory (ADMIN), AWS IAM (ADMIN), Okta (ADMIN) |

**Finding:** Admin on both identity provider (AD + Okta) AND cloud infrastructure (AWS) — toxic privilege combination enabling full lateral movement.

**Evidence:**
- `[AD] eric.carter` — ACTIVE, admin, lastLogin: 2026-06-15
- `[AWS] Ecarter` — ACTIVE, admin, lastLogin: 2026-06-08
- `[Okta] eric.carter@acme.com` — ACTIVE, admin, lastLogin: 2026-06-14

**Remediation:**
1. `AD:` Remove from Domain Admins; assign read-only Security Auditor role
2. `AWS:` Detach AdministratorAccess policy; attach custom least-privilege policy
3. `Okta:` Downgrade from Super Admin to Application Admin
4. Implement break-glass procedure for emergency admin access
5. Enable CloudTrail enhanced logging for all remaining admin actions

**Compliance:** NIST AC-5 (Separation of Duties), NIST AC-6 (Least Privilege), MITRE T1098

---

### 2. 🔴 Stephanie Johnson (UID-0018) — Risk Score: 90%

| Field | Value |
|-------|-------|
| **Employee ID** | EMP-0019 |
| **Risk Types** | Offboarding Gap |
| **Platforms** | AD (DISABLED), AWS IAM (ACTIVE), Okta (ACTIVE) |

**Finding:** Terminated employee. AD account disabled but AWS and Okta accounts remain ACTIVE — potential unauthorized data exfiltration path.

**Evidence:**
- `[AD] stephanie.johnson` — **DISABLED**, lastLogin: 2026-03-24
- `[AWS] Sjohnson` — ACTIVE, lastLogin: 2026-05-16 (logged in 53 DAYS after AD disabled!)
- `[Okta] stephanie.johnson@acme.com` — ACTIVE, lastLogin: 2026-06-17

**Remediation:**
1. `AWS:` IMMEDIATELY disable IAM user `Sjohnson`: `aws iam update-login-profile --user-name Sjohnson --no-password-reset-required && aws iam delete-login-profile --user-name Sjohnson`
2. `AWS:` Revoke all access keys: `aws iam list-access-keys --user-name Sjohnson | jq -r '.AccessKeyMetadata[].AccessKeyId' | xargs -I {} aws iam update-access-key --access-key-id {} --status Inactive --user-name Sjohnson`
3. `Okta:` Suspend user in Okta admin console → Users → stephanie.johnson@acme.com → Suspend
4. `Audit:` Generate CloudTrail report for Sjohnson from 2026-03-24 to present — investigate all data access post-termination
5. `Process:` Update offboarding runbook to include AWS + Okta deprovisioning as mandatory steps

**Compliance:** NIST AC-2 (Account Management), GDPR Art.32, MITRE T1078 (Valid Accounts)

---

### 3. 🔴 svc-pipeline-8 (UID-0009) — Risk Score: 95%

| Field | Value |
|-------|-------|
| **Account Type** | SERVICE_ACCOUNT |
| **Risk Types** | Stale Service Account, Dormant Admin, Orphaned |
| **Platforms** | AWS IAM (ADMIN), ServiceNow (ADMIN) |

**Finding:** Service account with admin on 2 platforms, last used >300 days ago. No identifiable owner. Credentials never rotated.

**Evidence:**
- `[AWS] svc-pipeline-8` — ACTIVE, **ADMIN**, lastLogin: 2025-08-22 (303 days ago!)
- `[ServiceNow] svc-pipeline-8` — ACTIVE, **ADMIN**, lastLogin: 2025-10-12

**Remediation:**
1. `AWS:` Rotate access keys immediately: `aws iam create-access-key --user-name svc-pipeline-8` then delete old keys
2. `AWS:` Detach AdministratorAccess; attach minimal S3/DynamoDB read policy
3. `ServiceNow:` Downgrade from admin role to itil_user
4. Assign owner from DevOps team within 48 hours
5. Set automated credential rotation policy (90-day maximum)

**Compliance:** NIST IA-5 (Authenticator Management), MITRE T1550 (Use Alternate Auth Material)

---

### 4. 🔴 Megan Nelson (UID-0052) — Risk Score: 90%

| Field | Value |
|-------|-------|
| **Risk Types** | Offboarding Gap |
| **Platforms** | AD (DISABLED), AWS IAM (ACTIVE, ADMIN), Okta (ACTIVE) |

**Finding:** Terminated employee with admin-level AWS access still active, plus active Okta SSO.

**Remediation:**
1. `AWS:` Disable IAM user `Mnelson` and deactivate MFA
2. `AWS:` Revoke all active sessions: `aws iam delete-virtual-mfa-device`
3. `Okta:` Deactivate user → Deprovision all linked applications
4. Audit all S3/EC2 actions by Mnelson post-termination

**Compliance:** NIST AC-2, GDPR Art.5 (Data Minimisation), MITRE T1078

---

### 5. 🔴 Carlos Adams (UID-0019) — Risk Score: 95%

| Field | Value |
|-------|-------|
| **Risk Types** | SoD Violation, Cross-Platform Admin |
| **Platforms** | AD (ADMIN), AWS IAM (ADMIN), Okta (ADMIN) |

**Finding:** Triple-platform admin with SoD violation.

**Remediation:**
1. Split admin roles: AD admin OR AWS admin, not both
2. Implement break-glass procedure with approval workflow
3. Enable Okta Behavior Detection for anomalous access patterns

**Compliance:** NIST AC-5, NIST AC-6, CIS Control 6

---

### 6. 🔴 David Lee (EMP-0054) — Stale Exception — Risk Score: 92%

| Field | Value |
|-------|-------|
| **Risk Types** | Stale Exception |
| **Platforms** | AD (revoked), AWS IAM (STILL ACTIVE), Okta (STILL ACTIVE) |

**Finding:** Temporary admin granted 77 days ago for "Incident response P1" — expired but NEVER revoked on AWS and Okta.

**Evidence:**
- Exception EXC-0053: granted 2026-04-01, expired 2026-04-04
- Approved by: Security-Lead
- Still active on: AWS_IAM, Okta (77 days overdue!)

**Remediation:**
1. `AWS:` Remove AdministratorAccess policy from user
2. `Okta:` Revoke Super Admin role assignment
3. Rotate all credentials issued during the exception window
4. Update exception tracker to mark as REVOKED

**Compliance:** NIST AC-2(3) (Disable Inactive Accounts), CIS Control 5

---

### 7. 🔴 svc-deploy-94 (UID-0095) — Risk Score: 95%

| Field | Value |
|-------|-------|
| **Risk Types** | Stale Service Account, Dormant Admin |
| **Platforms** | AWS IAM (ADMIN), ServiceNow (ADMIN) |

**Finding:** Deployment service account with cross-platform admin, inactive >240 days.

**Remediation:**
1. Disable both accounts pending owner verification
2. Rotate all API keys and access tokens
3. Review CI/CD pipeline dependencies before permanent removal

**Compliance:** NIST IA-5, MITRE T1550

---

### 8. 🔴 Tyler Carter (EMP-0071) — Stale Exception — Risk Score: 90%

| Field | Value |
|-------|-------|
| **Risk Types** | Stale Exception |
| **Finding** | Emergency production fix admin, expired 43 days ago, never revoked on ANY platform |

**Remediation:**
1. Revoke admin on all 3 platforms (AD, AWS, Okta) immediately
2. Rotate credentials
3. Flag approver (IT-Director) for exception review training

---

### 9. 🔴 Stephanie Baker (UID-0045) — Risk Score: 85%

| Field | Value |
|-------|-------|
| **Risk Types** | Dormant Admin, SoD Violation |
| **Platforms** | AD (ADMIN, last login: Jan 2026), AWS (ADMIN, last login: Nov 2025) |

**Finding:** Admin on both platforms, inactive 119-209 days. SoD violation with zero recent activity.

**Remediation:**
1. Disable both admin accounts
2. Rotate credentials
3. Require re-certification before any re-enablement

---

### 10. 🔴 Alex Wilson (UID-0093) — Risk Score: 90%

| Field | Value |
|-------|-------|
| **Risk Types** | Offboarding Gap |
| **Platforms** | AD (DISABLED), AWS (ACTIVE), Okta (ACTIVE) |

**Finding:** Offboarded employee with 2 active platform accounts.

**Remediation:**
1. Disable AWS and Okta accounts
2. Audit all post-termination activity
3. Revoke all API tokens and session cookies

---

## Compliance Summary

| Framework | Control | Findings | Status |
|-----------|---------|----------|--------|
| NIST AC-2 | Account Management | 18 offboarding gaps | ❌ Non-Compliant |
| NIST AC-5 | Separation of Duties | 15 SoD violations | ❌ Non-Compliant |
| NIST AC-6 | Least Privilege | 17 over-privileged | ⚠️ Partially Compliant |
| NIST IA-5 | Authenticator Management | 7 stale service accounts | ❌ Non-Compliant |
| MITRE T1078 | Valid Accounts | 18 identity gaps detected | ✅ Detected |
| MITRE T1098 | Account Manipulation | 15 privilege violations | ✅ Detected |
| MITRE T1550 | Alternate Auth Material | 7 token/credential abuse | ✅ Detected |
| GDPR Art.5 | Data Minimisation | Excessive access scope | ⚠️ Review Required |
| GDPR Art.32 | Security of Processing | Identity controls gaps | ❌ Non-Compliant |
| CIS Control 5 | Account Management | Lifecycle gaps found | ❌ Non-Compliant |
| CIS Control 6 | Access Control | Privilege scope violations | ❌ Non-Compliant |

---

## Alert Consolidation Summary

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Total Alerts | 148 | 26 | **82% reduction** |
| CRITICAL Alerts | 81 | 14 | 83% reduction |
| HIGH Alerts | 52 | 8 | 85% reduction |
| MEDIUM Alerts | 15 | 3 | 80% reduction |

**Method:** Clustered by identity + risk type family. Related risks for the same person merged into single actionable incidents.

---

## Recommendations

1. **Immediate (24 hours):** Disable all 18 offboarding gap accounts across AWS + Okta
2. **Short-term (1 week):** Revoke all 10 stale exception admin grants
3. **Medium-term (30 days):** Implement automated offboarding workflow linking HR → AD → AWS → Okta
4. **Long-term (90 days):** Deploy IKOS continuous monitoring for real-time identity risk detection

---

*Generated by IKOS v1.0 — Identity Knowledge Operating System*
*Classification: CONFIDENTIAL — For Internal Use Only*
